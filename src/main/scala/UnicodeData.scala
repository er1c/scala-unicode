

object UnicodeData {
  
  def main(args: Array[String]): Unit = {// http://www.unicode.org/Public/7.0.0/ucd/UnicodeData.txt
    import scala.io.Source
    import java.io.InputStream

    def toInt(hex: String): Int = Integer.parseInt(hex, 16)

    def printList[T](list: List[T]) { println(list.size + ": " + list) }

    val filename = "/UnicodeData.txt"
    
    val stream : InputStream = getClass.getResourceAsStream(filename)
    val lines = scala.io.Source.fromInputStream(stream).getLines // 27268
    val arrays = lines.map { x => x.split(";", 15)  } // 15 elements
      .filter { c => c(12) != "" || c(13) != "" } // 2188
    // 0;   1;  2; 3;   4;     5;   6;    7;      8;  9;   10; 11;12;13;14;
    //cp;name;cat;cc;bicat;decomp;dec;digval;numval;mir;uname;com;uc;lc;tc;
    val tuples = arrays.map { c => (c(0),c(1),c(2),c(3),c(4),c(5),c(9),c(12),c(13),c(14)) }.toList // for now
    val (lowers, uppers) = tuples.partition(t => t._9 == "")
    val compat = tuples.filter(t => t._6.startsWith("<compat>"))
    
    println(tuples.size)
    printList(compat.toList)
    
    
    val l = lowers.toList
    val u = uppers.toList
    println(l.toList)
    println(l.size)
    println(u.toList)
    println(u.size)
    //first.foreach(println)
    return
    //val tuples = arrays.map { c => (c(0), c(1).trim(), c(2).trim, c(3).trim) }
    // filter for 'simple case folding C + S' and greater than FFFF? && (t._1.size == 4)
    val fTuples = tuples.filter(t => (t._2 == "C" || t._2 == "S") ).toList
    val upperLower = fTuples.map(t => (toInt(t._1), toInt(t._3)))
    val pairs = upperLower.map { case (u, l) => (u, l, u - l) }

    def printIndented(list: List[(Int,Int,Int,Int)]) = {
      list.foreach(f => if (f._4 == 0) print("\n" + f) else print(f)); println
    }
    
    def addDiff(arr: List[(Int, Int, Int)]) = {
      arr.foldLeft[List[(Int, Int, Int, Int)]](Nil) { 
        case (x :: xs, elem) if (x._3 == elem._3) => {
          val diff = elem._1 - x._1
          // only ranges with diff or 1 or 2 matter
          // some ranges with 2 are only 2 long
          if (diff <= 2) (elem._1, elem._2, elem._3, diff) :: x :: xs
          else (elem._1, elem._2, elem._3, 0) :: x :: xs
        }
        case (list, elem) => (elem._1, elem._2, elem._3, 0) :: list
      }.reverse
    }
     
    def adjustStart(arr: List[(Int, Int, Int, Int)]) = {
      arr.tail.foldLeft[List[(Int, Int, Int, Int)]](List(arr.head)) { 
        case (x :: xs, elem) if (x._4 == 0 || x._4 == elem._4) => elem :: x :: xs
        case (list, elem) => (elem._1, elem._2, elem._3, 0) :: list
      }.reverse
    }
     
    def keepLast(arr: List[(Int, Int, Int, Int)]): List[(Int, Int, Int, Int)] = {
      import scala.annotation.tailrec
      val h = arr.head
      val tail = arr.tail
      @tailrec
      def process(list: List[(Int, Int, Int, Int)], acc: List[(Int, Int, Int, Int)], prev: (Int, Int, Int, Int)): List[(Int, Int, Int, Int)] = {
        list match {
          case Nil => acc
          case x :: xs => 
            if (x._4 == 0 && x._4 != prev._4) process(xs, x :: prev :: acc, x)
            else if (x._4 == 0 || xs == Nil) process(xs, x :: acc, x) // Nil to get last element
            else process(xs, acc, x)
        }
      }
      process(tail, List(h), h).reverse
    }
     
    def addDiffLower(arr: List[(Int, Int, Int)]) = {
      arr.foldLeft[List[(Int, Int, Int, Int)]](Nil) { 
        case (x :: xs, elem) if (x._3 == elem._3) => {
          val diff = elem._2 - x._2
          // only ranges with diff or 1 or 2 matter
          // some ranges with 2 are only 2 long
          if (diff <= 2) (elem._1, elem._2, elem._3, diff) :: x :: xs
          else (elem._1, elem._2, elem._3, 0) :: x :: xs
        }
        case (list, elem) => (elem._1, elem._2, elem._3, 0) :: list
      }.reverse
    }
     
    def lowerDedup(arr: List[(Int, Int, Int)]) = {
      arr.tail.foldLeft[List[(Int, Int, Int)]](List(arr.head)) { 
        case (x :: xs, elem) if (x._2 == elem._2) => x :: xs
        case (list, elem) => elem :: list
       }.reverse
     }
    
    // process uppers     
    val u2 = addDiff(pairs)
    val u3 = adjustStart(u2)
    //printIndented(u3)
    val u4 = keepLast(u3)
    
    // Lower case manipulation
    val lPairs = pairs.sortBy{ case ((u,l,d))  => ((l,u)) } 
    val l2 = lowerDedup(lPairs)
    // 1104 total, 1083 in lowers with dups removed
    // 244 in uppers because of upper -> lower can have more than one mapping 
    // 230 in lowers
    // upper to lower is loss less but back is not reversible 
    val l3 = addDiffLower(l2)
    val l4 = adjustStart(l3)
    //printIndented(l4)
    val l5 = keepLast(l4)
    
    val uk = u4.unzip { case(_, _, c, d) => (c, d)}
    val ul = u4.unzip { case(a, b, _, _) => (a, b)}
    val lk = l5.unzip { case(_, _, c, d) => (c, d)}
    val ll = l5.unzip { case(a, b, _, _) => (a, b)} 
    println(uk._1.mkString("val upperDeltas = Array[scala.Int](", ", ", ")"))
    println(uk._2.mkString("val upperSteps = Array[scala.Int](", ", ", ")"))
    println(ul._1.mkString("val upperRanges = Array[scala.Int](", ", ", ")"))
    println(lk._1.mkString("val lowerDeltas = Array[scala.Int](", ", ", ")"))
    println(lk._2.mkString("val lowerSteps = Array[scala.Int](", ", ", ")"))
    println(ll._2.mkString("val lowerRanges = Array[scala.Int](", ", ", ")"))
  }
      
}
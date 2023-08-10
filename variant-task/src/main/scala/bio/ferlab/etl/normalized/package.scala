package bio.ferlab.etl

package object normalized {

  val validContigNames: List[String] = List("chrX", "chrY") ++ (1 to 22).map(n => s"chr$n")

}

package model

case class SEQUENCING_EXPERIMENT(
                                  `owner`: String = "CQDG",
                                  `experimental_strategy`: Seq[String] = Seq("WGS"),
                                  `alir`: String = "alir",
                                  `snv`: String = "snv",
                                  `gcnv`: String = "gcnv",
                                  `gsv`: String = "gsv",
                                  `ssup`: String = "ssup",
                                )

package model

case class SEQUENCING_EXPERIMENT_INPUT(
                                  `owner`: String = "CQDG",
                                  `experimental_strategy`: Seq[String] = Seq("WGS"),
                                  `alir`: String = "alir",
                                  `snv`: String = "snv",
                                  `gcnv`: String = "gcnv",
                                  `gsv`: String = "gsv",
                                  `ssup`: String = "ssup",
                                  `is_paired_end`: Boolean = true,
                                )

case class SEQUENCING_EXPERIMENT(
                                        `owner`: String = "CQDG",
                                        `experimental_strategy`: Seq[String] = Seq("WGS"),
                                        `alir`: String = "alir",
                                        `snv`: String = "snv",
                                        `gcnv`: String = "gcnv",
                                        `gsv`: String = "gsv",
                                        `ssup`: String = "ssup",
                                      )

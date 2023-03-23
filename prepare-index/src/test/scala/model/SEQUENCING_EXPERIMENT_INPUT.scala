package model

case class SEQUENCING_EXPERIMENT_INPUT(
                                  `owner`: String = "CQDG",
                                  `experimental_strategy`: Seq[String] = Seq("WGS"),
                                  `alir`: String = "alir",
                                  `snv`: String = "snv",
                                  `gcnv`: String = "gcnv",
                                  `gsv`: String = "gsv",
                                  `ssup`: String = "ssup",
                                  `run_date`: String = "12.12.12/12",
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

case class SEQUENCING_EXPERIMENT_SINGLE(
                                  `owner`: String = "CQDG",
                                  `experimental_strategy`: String = "WGS",
                                  `alir`: String = "alir",
                                  `snv`: String = "snv",
                                  `gcnv`: String = "gcnv",
                                  `gsv`: String = "gsv",
                                  `ssup`: String = "ssup",
                                )

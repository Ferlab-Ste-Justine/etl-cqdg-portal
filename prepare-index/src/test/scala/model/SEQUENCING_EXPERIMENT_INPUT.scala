package model

case class SEQUENCING_EXPERIMENT_INPUT(
                                  `owner`: String = "CQDG",
                                  `experimental_strategy`: Seq[String] = Seq("WGS"),
                                  `analysis_files`: Seq[ANALYSIS_FILE] = Seq(
                                    ANALYSIS_FILE("Annotated-SNV", "annSnv"),
                                    ANALYSIS_FILE("Aligned-reads", "alir"),
                                    ANALYSIS_FILE("SNV", "snv"),
                                    ANALYSIS_FILE("Germline-CNV", "gcnv"),
                                    ANALYSIS_FILE("Germline-structural-variant", "gsv"),
                                    ANALYSIS_FILE("Sequencing-data-supplement", "ssup"),
                                  ),
                                  `run_date`: String = "12.12.12/12",
                                  `is_paired_end`: Boolean = true,
                                )

case class SEQUENCING_EXPERIMENT(
                                        `owner`: String = "CQDG",
                                        `experimental_strategy`: Seq[String] = Seq("WGS"),
                                        `analysis_files`: Seq[ANALYSIS_FILE] = Seq(
                                          ANALYSIS_FILE("Annotated-SNV", "annSnv"),
                                          ANALYSIS_FILE("Aligned-reads", "alir"),
                                          ANALYSIS_FILE("SNV", "snv"),
                                          ANALYSIS_FILE("Germline-CNV", "gcnv"),
                                          ANALYSIS_FILE("Germline-structural-variant", "gsv"),
                                          ANALYSIS_FILE("Sequencing-data-supplement", "ssup"),
                                        )
                                      )

case class SEQUENCING_EXPERIMENT_SINGLE(
                                  `owner`: String = "CQDG",
                                  `experimental_strategy`: String = "WGS",
                                  `analysis_files`: Seq[ANALYSIS_FILE] = Seq(
                                    ANALYSIS_FILE("Annotated-SNV", "annSnv"),
                                    ANALYSIS_FILE("Aligned-reads", "alir"),
                                    ANALYSIS_FILE("SNV", "snv"),
                                    ANALYSIS_FILE("Germline-CNV", "gcnv"),
                                    ANALYSIS_FILE("Germline-structural-variant", "gsv"),
                                    ANALYSIS_FILE("Sequencing-data-supplement", "ssup"),
                                  )
                                )

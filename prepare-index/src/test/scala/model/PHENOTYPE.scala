package model

case class PHENOTYPE(
                      `study_id`: String = "STU0000001",
                      `release_id`: String = "5",
                      `fhir_id`: String = "PHE0000001",
                      `phenotype_source_text`: String = null,
                      `phenotype_HPO_code`: PHENOTYPE_HPO_CODE = PHENOTYPE_HPO_CODE(),
                      `cqdg_participant_id`: String = "PRT0000003",
                      `phenotype_observed`: String = "POS",
                    )

case class PHENOTYPE_ENRICHED(
                            `name`: String = "Abnormality of the cardiovascular system (HP:0001626)",
                            `parents`: Seq[String] = Seq.empty,
                            `is_tagged`: Boolean = false,
                            `is_leaf`: Boolean = false,
                            `age_at_event_days`: Seq[Int] = Seq.empty
                          )


case class PHENOTYPE_HPO_CODE(
                               `system`: String = "http://purl.obolibrary.org/obo/hp.owl",
                               `code`: String = "HP:0003124",
                             )


case class PHENOTYPE_TAGGED (
                              `internal_phenotype_id`: String = "1",
                              `is_tagged`: Boolean = true,
                              `phenotype_id`: String = "HP:0001626",
                              `is_leaf`: Boolean = false,
                              `name`: String = "Abnormality of the cardiovascular system",
                              `parents`: Seq[String] = Nil,
                              `age_at_event`: Int = 0,
                              `display_name`: String = "Abnormality of the cardiovascular system (HP:0001626)"
                            )

case class PHENOTYPE_TAGGED_WITH_ANCESTORS (
                                             `phenotype_id`: String = "HP:0001626",
                                             `name`: String = "Abnormality of the cardiovascular system",
                                             `parents`: Seq[String] = Nil,
                                             `is_leaf`: Boolean = false,
                                             `is_tagged`: Boolean = false,
                                             `display_name`: String = "Abnormality of the cardiovascular system (HP:0001626)",
                                             `age_at_event`: Seq[Int] = Seq(0)
                                           )

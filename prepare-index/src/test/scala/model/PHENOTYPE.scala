package model

case class PHENOTYPE(
                      `study_id`: String = "STU0000001",
                      `release_id`: String = "5",
                      `fhir_id`: String = "PHE0000001",
                      `phenotype_source_text`: String = null,
                      `phenotype_HPO_code`: PHENOTYPE_HPO_CODE = PHENOTYPE_HPO_CODE(),
                      `cqdg_participant_id`: String = "PRT0000003",
                      `phenotype_observed`: String = null,
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

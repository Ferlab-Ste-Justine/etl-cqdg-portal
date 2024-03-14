package model

case class FAMILY_RELATIONSHIP(
                                `study_id`: String = "STU0000001",
                                `internal_family_relationship_id`: String = "439751",
                                `category`: String = "FR_XA5WESR3",
                                `submitter_participant_id`: String = "FR_XA5WESR3",
                                `focus_participant_id`: String = "428396",
                                `relationship_to_proband`: String = "Mother",
                              )

case class FAMILY_RELATIONSHIP_WITH_FAMILY(
                                            `participant_id`: String = "P1",
                                            `submitter_participant_id`: String = "S_P1",
                                            `focus_participant_id`: String = "P2",
                                            `relationship_to_proband`: String = "Father",
                                            `family_id`: String = "Family1",
                                            `family_type`: String = "trio",
                                            `submitter_family_id`: String = "fam1",
                                          )

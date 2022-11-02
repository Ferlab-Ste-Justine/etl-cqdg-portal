package model

case class GROUP(
                  study_id: String = "Study_1",
                  release_id: String = "5",
                  internal_family_id: String = "1",
                  family_type: String = "trio",
                  family_members: Seq[String] = Seq("P1", "P2", "P3"),
                  submitter_family_id: String = "1"
                 )
case class FAMILY(
                   cqdg_participant_id: String = "42367",
                   fhir_id: String = "42367",
                   study_id: String = "Study_1",
                   release_id: String = "5",
                   family_id: String = "FM_NV901ZZN",
                   father_id: Option[String] = None,
                   mother_id: Option[String] = None,
                   family_relations: Seq[FAMILY_RELATIONS] = Seq(FAMILY_RELATIONS())
                 )

case class FAMILY_RELATIONS(
                   related_participant_id: String = "PT_48DYT4PP",
                   related_participant_fhir_id: String = "123",
                   relation: String = "mother"
                 )

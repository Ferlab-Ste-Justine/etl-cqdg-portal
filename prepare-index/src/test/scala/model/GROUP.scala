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
                   submitter_participant_id: String = "42367",
                   focus_participant_id: String = "42367",
                   relationship_to_proband: String = "mother",
                   internal_familyrelationship_id: String = "5",
                   family_type: String = "trio",
                   family_id: String = "family_id",
                 )

case class FAMILY_RELATIONS(
                   related_participant_id: String = "PT_48DYT4PP",
                   related_participant_fhir_id: String = "123",
                   relation: String = "mother"
                 )

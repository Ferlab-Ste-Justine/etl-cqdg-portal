/**
 * Generated by [[bio.ferlab.datalake.testutils.ClassGenerator]]
 * on 2023-08-17T11:40:58.129806
 */
package models




case class NORMALIZED_DOCUMENT_REFERENCE(`fhir_id`: String = "FIL0000019",
                                         `participant_id`: String = "PRT0000003",
                                         `biospecimen_reference`: String = "SAM0000003",
                                         `data_type`: String = "Sequencing-data-supplement",
                                         `data_category`: String = "Genomics",
                                         `dataset`: String = "ds_name 1",
                                         `files`: Seq[FILES] = Seq(FILES()),
                                         `study_id`: String = "STU0000001",
                                         `security`: String = "R",
                                         `release_id`: Int = 1)

case class FILES(`file_name`: String = "mpsMetrics_S03344.tar.gz",
                 `file_format`: String = "TGZ",
                 `file_size`: Float = 0.0f,
                 `ferload_url`: String = "http://flerloadurl/03fe6239ff7db8a5706103d3f0dd08441004ed5b",
                 `file_hash`: Option[String] = None)
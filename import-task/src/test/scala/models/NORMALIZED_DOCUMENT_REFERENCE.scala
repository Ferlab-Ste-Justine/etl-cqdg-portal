/**
 * Generated by [[bio.ferlab.datalake.testutils.ClassGenerator]]
 * on 2023-08-17T11:40:58.129806
 */
package models




case class NORMALIZED_DOCUMENT_REFERENCE(`fhir_id`: String = "FIL0000101",
                                         `participant_id`: String = "PRT0000004",
                                         `biospecimen_reference`: Seq[String] = Seq("SAM0000003", "SAM0000004", "SAM0000001"),
                                         `data_type`: String = "Annotated-SNV",
                                         `data_category`: String = "genomics",
                                         `dataset`: String = "d1",
                                         `files`: Seq[FILES] = Seq(FILES()),
                                         `study_id`: String = "study1",
                                         `relates_to`: Option[String] = None,
                                         `security`: String = null)

case class FILES(`file_name`: String = "FIL0000101.variants_HSJ0140.vep.vcf.gz",
                 `file_format`: String = "VCF",
                 `file_size`: Float = 8.0f,
                 `ferload_url`: String = "s3://cqdg-dev-file-import/studies/study1/study_version_1/WGS/annotation/variants_HSJ0140.vep.vcf.gz",
                 `file_hash`: Option[String] = None)

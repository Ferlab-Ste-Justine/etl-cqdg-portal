package model

case class TASK(
                 `fhir_id`: String = "SXP0000001",
                 `owner`: String = "CQDG",
                 `bio_informatic_analysis`: String = "GGBA",
                 `lab_aliquot_ids`: Seq[String] = Seq("nanuq_sample_id"),
                 `ldm_sample_id`: String = "S16523",
                 `run_ids`: Seq[String] = Seq("runNameExample"),
                 `run_dates`: Seq[String] = Seq("2007-04-14"),
                 `read_length`: Option[String] = Some("151"),
                 `selection`: CODEABLE = CODEABLE("RR", "RR_display"),
                 `source`: CODEABLE = CODEABLE("TSC", "TSC_display"),
                 `protocol`: String = "protocol2",
                 `target_capture_kit`: String = "targetCaptureKit2",
                 `target_loci`: String = "targetedLoci2",
                 `pipeline`: String = "testPipeline",
                 `is_paired_end`: Boolean = true,
                 `capture_kit`: String = "RocheKapaHyperExome",
                 `platform`: String = "Illumina",
                 `experimental_strategy`: String = "WXS", // FIXME remove this field after all studies are updated (replace _1)
                 `experimental_strategy_1`: CODEABLE = CODEABLE("WXS", "wxs_display"),
                 `sequencer_id`: String = "NB552318",
                 `genome_build`: String = "GRCh38",
                 `_for`: String = "PRT0000001",
                 `analysis_files`: Seq[ANALYSIS_FILE] = Seq(
                   ANALYSIS_FILE("Annotated-SNV", "12"),
                   ANALYSIS_FILE("Aligned-reads", "1"),
                   ANALYSIS_FILE("SNV", "2"),
                   ANALYSIS_FILE("Germline-CNV", "3"),
                   ANALYSIS_FILE("Germline-structural-variant", "4"),
                   ANALYSIS_FILE("Sequencing-data-supplement", "5"),
                 ),
                 `study_id`: String = "STU0000001",
               )

case class ANALYSIS_FILE(
                          data_type: String,
                          file_id: String,
                        )
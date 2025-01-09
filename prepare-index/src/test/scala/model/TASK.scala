package model

case class TASK(
                 `fhir_id`: String = "SXP0000001",
                 `study_id`: String = "STU0000001",
                 `_for`: String = "PRT0000001",
                 `owner`: String = "CQDG",
                 `is_paired_end`: Boolean = true,
                 `bio_informatic_analysis`: String = "GGBA",
                 `labAliquotID`: String = "nanuq_sample_id",
                 `run_name`: String = "runNameExample",
                 `run_alias`: String = "runAliasExample",
                 `run_date`: String = "2007-04-14",
                 `capture_kit`: String = "RocheKapaHyperExome",
                 `platform`: String = "Illumina",
                 `experimental_strategy`: String = "WXS",
                 `sequencer_id`: String = "NB552318",
                 `workflow_name`: String = "Dragen",
                 `workflow_version`: String = "1.1.0",
                 `genome_build`: String = "GRCh38",
                 `analysis_files`: Seq[ANALYSIS_FILE] = Seq(
                   ANALYSIS_FILE("Annotated-SNV", "12"),
                   ANALYSIS_FILE("Aligned-reads", "1"),
                   ANALYSIS_FILE("SNV", "2"),
                   ANALYSIS_FILE("Germline-CNV", "3"),
                   ANALYSIS_FILE("Germline-structural-variant", "4"),
                   ANALYSIS_FILE("Sequencing-data-supplement", "5"),
                 ),
               )

case class ANALYSIS_FILE(
                          data_type: String,
                          file_id: String,
                        )
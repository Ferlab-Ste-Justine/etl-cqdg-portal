package model

case class TASK(
                 `fhir_id`: String = "SXP0000001",
                 `study_id`: String = "STU0000001",
                 `release_id`: String = "5",
                 `_for`: String = "PRT0000001",
                 `owner`: String = "CQDG",
                 `is_paired_end`: Boolean = true,
                 `bio_informatic_analysis`: String = "GGBA",
                 `labAliquotID`: String = "nanuq_sample_id",
                 `run_name`: String = "runNameExample",
                 `run_alias`: String = "runAliasExample",
                 `run_date`: String = null,
                 `capture_kit`: String = "RocheKapaHyperExome",
                 `platform`: String = "Illumina",
                 `experimental_strategy`: String = "WXS",
                 `sequencer_id`: String = "NB552318",
                 `workflow_name`: String = "Dragen",
                 `workflow_version`: String = "1.1.0",
                 `genome_build`: String = "GRCh38",
                 `alir`: String = "1",
                 `snv`: String = "2",
                 `gcnv`: String = "3",
                 `gsv`: String = "4",
                 `ssup`: String = "5",
               )



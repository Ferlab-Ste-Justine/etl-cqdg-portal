package model

case class SEQUENCING_EXPERIMENT_SINGLE(
    `owner`: String = "CQDG",
    `experimental_strategy_1`: CODEABLE = CODEABLE("WXS", "wxs_display"),
    `experimental_strategy`: String =
      "WXS", // FIXME remove this field after all studies are updated (replace _1)
    `selection`: CODEABLE = CODEABLE("RR", "RR_display"),
    `source`: CODEABLE = CODEABLE("TSC", "TSC_display"),
    `protocol`: String = "protocol2",
    `target_capture_kit`: String = "targetCaptureKit2",
    `target_loci`: String = "targetedLoci2",
    `pipelines`: Seq[String] = Seq("testPipeline1", "testPipeline2"),
    `run_ids`: Seq[String] = Seq("runNameExample"),
    `run_dates`: Seq[String] = Seq("2007-04-14"),
//                                  `is_paired_end`: Boolean = false,
    `analysis_files`: Seq[ANALYSIS_FILE] = Seq(
      ANALYSIS_FILE("Annotated-SNV", "12"),
      ANALYSIS_FILE("Aligned-reads", "1"),
      ANALYSIS_FILE("SNV", "2"),
      ANALYSIS_FILE("Germline-CNV", "3"),
      ANALYSIS_FILE("Germline-structural-variant", "4"),
      ANALYSIS_FILE("Sequencing-data-supplement", "5")
    )
)

case class TASK_SAMPLE(
    `lab_aliquot_ids`: Seq[String] = Seq("nanuq_sample_id"),
    `ldm_sample_id`: String = "S16523"
)

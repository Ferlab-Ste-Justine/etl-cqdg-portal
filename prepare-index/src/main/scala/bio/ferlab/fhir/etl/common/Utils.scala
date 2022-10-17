package bio.ferlab.fhir.etl.common

import bio.ferlab.fhir.etl.common.OntologyUtils._
import org.apache.spark.sql.{Column, DataFrame}
import org.apache.spark.sql.expressions.{UserDefinedFunction, Window}
import org.apache.spark.sql.functions.{when, _}

object Utils {
  val DOWN_SYNDROM_MONDO_TERM = "MONDO:0008608"

  val observableTitleStandard: Column => Column = term => trim(regexp_replace(term, "_", ":"))

  val sequencingExperimentCols = Seq("fhir_id", "sequencing_experiment_id", "experiment_strategy",
    "instrument_model", "library_name", "library_strand", "platform")

  private def reformatSequencingExperiment(documentDF: DataFrame) = {
    documentDF
      .withColumn("sequencing_experiment", struct(col("experiment_strategy")))
      .withColumn("file_facet_ids", struct(col("fhir_id") as "file_fhir_id_1", col("fhir_id") as "file_fhir_id_2"))
      .drop("experiment_strategy")
  }

  private def reformatBiospecimen(biospecimensDf: DataFrame) = {
    biospecimensDf
      .withColumn("biospecimen_facet_ids", struct(col("fhir_id") as "biospecimen_fhir_id_1", col("fhir_id") as "biospecimen_fhir_id_2"))
      .withColumn("biospecimen", struct((biospecimensDf.columns :+ "biospecimen_facet_ids").map(col): _*))
      .withColumnRenamed("fhir_id", "specimen_fhir_id")
      .withColumnRenamed("participant_fhir_id", "specimen_participant_fhir_id")
      .select("specimen_fhir_id", "specimen_participant_fhir_id", "biospecimen")
  }


  implicit class DataFrameOperations(df: DataFrame) {
    def addStudy(studyDf: DataFrame): DataFrame = {
      val cols = studyDf.columns.filterNot(Seq("release_id").contains(_))

      val refactorStudyDf = studyDf
        .withColumn("study", struct(cols.map(col): _*))
        .drop(studyDf.columns.filterNot(c => c == "study_id"): _*)

      df.join(refactorStudyDf, Seq("study_id"), "left_outer")
    }

    def addOutcomes(vitalStatusDf: DataFrame): DataFrame = {
      val reformatObservation: DataFrame = vitalStatusDf
        .withColumn("outcome", struct(vitalStatusDf.columns.map(col): _*))
        .select("participant_fhir_id", "outcome")
        .groupBy("participant_fhir_id")
        .agg(
          collect_list(col("outcome")) as "outcomes"
        )

      df
        .join(reformatObservation, col("fhir_id") === col("participant_fhir_id"), "left_outer")
        .withColumn("outcomes", coalesce(col("outcomes"), array()))
        .drop("participant_fhir_id")
    }

    def addDownSyndromeDiagnosis(diseases: DataFrame, mondoTerms: DataFrame): DataFrame = {
      val mondoDownSyndrome = mondoTerms.where(
        exists(col("parents"), p => p like s"%$DOWN_SYNDROM_MONDO_TERM%") || col("id") === DOWN_SYNDROM_MONDO_TERM).select(col("id") as "mondo_down_syndrome_id", col("name") as "mondo_down_syndrome_name")

      val downSyndromeDiagnosis = diseases.join(mondoDownSyndrome, col("mondo_id") === col("mondo_down_syndrome_id"))
        .select(
          col("participant_fhir_id"),
          when(col("mondo_down_syndrome_id").isNotNull, displayTerm(col("mondo_down_syndrome_id"), col("mondo_down_syndrome_name")))
            .otherwise(null) as "down_syndrome_diagnosis"
        )
        .groupBy("participant_fhir_id")
        .agg(collect_set("down_syndrome_diagnosis") as "down_syndrome_diagnosis")
      df.join(downSyndromeDiagnosis, col("fhir_id") === col("participant_fhir_id"), "left_outer")
        .withColumn("down_syndrome_status", when(size(col("down_syndrome_diagnosis")) > 0, "T21").otherwise("D21"))
        .drop("participant_fhir_id")

    }

    def addDiagnosisPhenotypes(phenotypeDF: DataFrame, diagnosesDF: DataFrame)(hpoTerms: DataFrame, mondoTerms: DataFrame, icdTerms: DataFrame): DataFrame = {
      val (observedPhenotypes, nonObservedPhenotypes, observedPhenotypesWithAncestors) = getTaggedPhenotypes(phenotypeDF, hpoTerms)

      val (diagnosis, mondoWithAncestors) = getDiagnosis(diagnosesDF, mondoTerms, icdTerms)

      df
        .join(diagnosis, col("fhir_id") === col("cqdg_participant_id"), "left_outer")
        .join(mondoWithAncestors, Seq("cqdg_participant_id"), "left_outer")
        .join(observedPhenotypes, Seq("cqdg_participant_id"), "left_outer")
        .join(nonObservedPhenotypes, Seq("cqdg_participant_id"), "left_outer")
        .join(observedPhenotypesWithAncestors, Seq("cqdg_participant_id"), "left_outer")
    }

    def addCauseOfDeath(causeOfDeath: DataFrame): DataFrame = {
      val cleanCauseOfDeath = causeOfDeath
        .drop("study_id","release_id", "fhir_id")

      df
        .join(cleanCauseOfDeath, col("submitter_participant_ids") === col("fhir_id"), "left_outer")
        .drop("submitter_participant_ids")
    }

    def addDiseaseStatus(diseaseStatus: DataFrame): DataFrame = {
      val cleanDiseaseStatus = diseaseStatus
        .drop("study_id","release_id", "fhir_id")
        .withColumnRenamed("disease_status", "is_affected")

      df
        .join(cleanDiseaseStatus, col("subject") === col("fhir_id"), "left_outer")
        .drop("subject")
    }

    def addGroup(group: DataFrame): DataFrame = {
      val explodedGroupDf = group
        .withColumn("family_member", explode(col("family_members")))
        .drop("study_id", "release_id")

      df
        .join(explodedGroupDf, col("family_member") === col("submitter_participant_id"), "left_outer")
        .select("internal_family_id", "submitter_family_id", "submitter_participant_id", "focus_participant_id", "relationship_to_proband", "family_type")
    }

    def addFamilyRelationshipToParticipant(familyRelationship: DataFrame): DataFrame = {

      val groupedFamilyDf = familyRelationship
      .withColumn("family_relationship", struct(familyRelationship.columns.map(col): _*))
        .groupBy("submitter_family_id")
        .agg(
          collect_list(col("submitter_participant_id")) as "submitter_participant_ids",
          collect_list(col("family_relationship")) as "family_relationships"
        )
        .withColumn("submitter_participant_ids", explode(col("submitter_participant_ids")))
        .drop("submitter_family_id")

      df.join(groupedFamilyDf, groupedFamilyDf.col("submitter_participant_ids") === df.col("fhir_id"), "left_outer")
    }

    def addParticipantFilesWithBiospecimen(filesDf: DataFrame, biospecimensDf: DataFrame, seqExperiment: DataFrame): DataFrame = {
      val biospecimenDfReformat = reformatBiospecimen(biospecimensDf)

      val filesWithSeqExpDF = reformatSequencingExperiment(filesDf)

      val filesWithBiospecimenDf =
        filesWithSeqExpDF
          .withColumn("specimen_fhir_id_file", explode_outer(col("specimen_fhir_ids")))
          .join(biospecimenDfReformat,
            col("specimen_fhir_id_file") === biospecimenDfReformat("specimen_fhir_id"),
            "full")
          .withColumnRenamed("participant_fhir_id", "participant_fhir_id_file")
          .withColumn("file_name", when(col("fhir_id").isNull, "dummy_file").otherwise(col("file_name")))
          .withColumn("participant_fhir_id",
            when(col("biospecimen.participant_fhir_id").isNotNull, col("biospecimen.participant_fhir_id"))
              .otherwise(col("participant_fhir_id_file"))
          )
          .drop("participant_fhir_id_file")
          .groupBy("fhir_id", "participant_fhir_id")
          .agg(collect_list(col("biospecimen")) as "biospecimens",
            filesWithSeqExpDF.columns.filter(c => !c.equals("fhir_id") && !c.equals("participant_fhir_id")).map(c => first(c).as(c)): _*)
          .withColumn("file_facet_ids", struct(col("fhir_id") as "file_fhir_id_1", col("fhir_id") as "file_fhir_id_2"))
          .drop("specimen_fhir_ids")

      val filesWithBiospecimenGroupedByParticipantIdDf =
        filesWithBiospecimenDf
          .withColumn("file", struct(filesWithBiospecimenDf.columns.filterNot(c => c.equals("participant_fhir_id")).map(col): _*))
          .withColumn("biospecimens_unique_ids", transform(col("file.biospecimens"), c => concat_ws("_", c("fhir_id"), c("container_id"))))
          .select("participant_fhir_id", "file", "biospecimens_unique_ids")
          .groupBy("participant_fhir_id")
          .agg(
            coalesce(count(col("file.file_id")), lit(0)) as "nb_files",
            collect_list(col("file")) as "files",
            coalesce(size(array_distinct(flatten(collect_set(col("biospecimens_unique_ids"))))), lit(0)) as "nb_biospecimens"
          )

      df
        .join(filesWithBiospecimenGroupedByParticipantIdDf, df("fhir_id") === filesWithBiospecimenGroupedByParticipantIdDf("participant_fhir_id"), "left_outer")
        .withColumn("files", coalesce(col("files"), array()))
        .drop("participant_fhir_id")
    }

    def addFileParticipantsWithBiospecimen(participantDf: DataFrame, biospecimensDf: DataFrame): DataFrame = {

      val fileWithSeqExp = reformatSequencingExperiment(df)
        .withColumn("specimen_fhir_id", explode_outer(col("specimen_fhir_ids")))

      val biospecimensDfReformat = reformatBiospecimen(biospecimensDf)

      val fileWithBiospecimen = fileWithSeqExp
        .select(struct(col("*")) as "file")
        .join(biospecimensDfReformat, col("file.specimen_fhir_id") === biospecimensDfReformat("specimen_fhir_id"), "left_outer")
        .withColumn("participant_file_fhir_id", when(biospecimensDfReformat("specimen_participant_fhir_id").isNotNull, biospecimensDfReformat("specimen_participant_fhir_id")).otherwise(col("file.participant_fhir_id")))
        .withColumn("biospecimen_unique_id", when(col("biospecimen.fhir_id").isNotNull, concat_ws("_", col("biospecimen.fhir_id"), col("biospecimen.container_id"))).otherwise(null))
        .groupBy("file.fhir_id", "participant_file_fhir_id")
        .agg(collect_list(col("biospecimen")) as "biospecimens", first("file") as "file", count(col("biospecimen_unique_id")) as "nb_biospecimens")

      val participantReformat = participantDf.select(struct(col("*")) as "participant")
      fileWithBiospecimen
        .join(participantReformat, col("participant_file_fhir_id") === col("participant.fhir_id"))
        .withColumn("participant", struct(col("participant.*"), col("biospecimens")))
        .drop("biospecimens")
        .groupBy(col("fhir_id"))
        .agg(collect_list(col("participant")) as "participants", first("file") as "file", count(lit(1)) as "nb_participants", sum("nb_biospecimens") as "nb_biospecimens")
        .select(col("file.*"), col("participants"), col("nb_participants"), col("nb_biospecimens"))


    }

    def addBiospecimenFiles(filesDf: DataFrame): DataFrame = {
      val filesWithSeqExperiments = reformatSequencingExperiment(filesDf)

      val fileColumns = filesWithSeqExperiments.columns.collect { case c if c != "specimen_fhir_ids" => col(c) }
      val reformatFile = filesWithSeqExperiments
        .withColumn("biospecimen_fhir_id", explode(col("specimen_fhir_ids")))
        .drop("document_reference_fhir_id")
        .withColumn("file", struct(fileColumns: _*))
        .select("biospecimen_fhir_id", "file")
        .groupBy("biospecimen_fhir_id")
        .agg(collect_list(col("file")) as "files")

      df
        .join(reformatFile, df("fhir_id") === reformatFile("biospecimen_fhir_id"), "left_outer")
        .withColumn("files", coalesce(col("files"), array()))
        .withColumn("nb_files", coalesce(size(col("files")), lit(0)))
        .drop("biospecimen_fhir_id")
    }

    def addBiospecimenParticipant(participantsDf: DataFrame): DataFrame = {
      val reformatParticipant: DataFrame = participantsDf
        .withColumn("participant", struct(participantsDf.columns.map(col): _*))
        .withColumn("participant_fhir_id", col("fhir_id"))
        .select("participant_fhir_id", "participant")

      df.join(reformatParticipant, "participant_fhir_id")
    }

    def addFamily(familyDf: DataFrame, familyRelationshipDf: DataFrame): DataFrame = {
      val explodedFamilyDF = familyDf
        .withColumn("family_members_exp", explode(col("family_members")))
        .drop("study_id", "release_id")

      val isProbandDf = familyRelationshipDf
        .select("submitter_participant_id", "relationship_to_proband")
        .withColumn("is_a_proband", when(col("relationship_to_proband") === "Is the proband", lit(true)).otherwise(lit(false)))
        .drop("relationship_to_proband")

      val familyWithGroup = familyRelationshipDf
        .join(explodedFamilyDF, col("family_members_exp") === col("submitter_participant_id"), "left_outer")
        .groupBy("study_id", "release_id", "internal_family_id", "submitter_family_id")
        .agg(collect_list(struct(
          col("submitter_participant_id"),
          col("focus_participant_id"),
          col("relationship_to_proband"),
          col("internal_family_id") as "internal_familyrelationship_id",
          col("family_type"),
          col("submitter_family_id") as "family_id",
        )) as "familyRelationships",
          first(col("family_members")) as "family_members",
        )
        .withColumn("family_members_exp", explode(col("family_members")))
        .join(isProbandDf, col("submitter_participant_id") === col("family_members_exp"))
        .drop("submitter_participant_id", "family_members", "study_id", "release_id", "internal_family_id", "submitter_family_id")

      df
        .join(familyWithGroup, col("cqdg_participant_id") === col("family_members_exp"), "left_outer")
        .drop("family_members_exp")
    }
  }
}

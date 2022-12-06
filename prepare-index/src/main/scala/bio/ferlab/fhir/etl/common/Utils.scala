package bio.ferlab.fhir.etl.common

import bio.ferlab.fhir.etl.common.OntologyUtils._
import org.apache.spark.sql.{Column, DataFrame}
import org.apache.spark.sql.expressions.{UserDefinedFunction, Window}
import org.apache.spark.sql.functions.{when, _}

object Utils {

  implicit class DataFrameOperations(df: DataFrame) {
    def addStudy(studyDf: DataFrame): DataFrame = {
      val cols = studyDf.columns.filterNot(Seq("release_id").contains(_))

      val refactorStudyDf = studyDf
        .withColumn("study", struct(cols.map(col): _*))
        .drop(studyDf.columns.filterNot(c => c == "study_id"): _*)

      df.join(refactorStudyDf, Seq("study_id"), "left_outer")
    }

    def addParticipantWithBiospecimen(participantDf: DataFrame, biospecimenDF: DataFrame, sampleRegistration: DataFrame): DataFrame = {
      val biospecimenWithSamples = biospecimenDF
        .addSamplesToBiospecimen(sampleRegistration)

      val participantsWithBiospecimen = participantDf.addBiospecimen(biospecimenWithSamples)

      val participantsWithBiospecimens = participantsWithBiospecimen
        .withColumn("participant", struct(participantsWithBiospecimen.columns.filterNot(Seq("study_id", "release_id").contains).map(col): _*))
        .drop(participantsWithBiospecimen.columns.filterNot(Seq("participant", "participant_id").contains): _*)

      val filesWithParticipants = df
        .join(participantsWithBiospecimens, Seq("participant_id"), "inner")
        .drop("participant_id")

      filesWithParticipants
        .groupBy(filesWithParticipants.columns.filterNot(Seq("participant").contains) map col: _*)
        .agg(collect_list("participant") as "participants")
    }

    def addSamplesToBiospecimen(samplesDf: DataFrame): DataFrame = {
      val samplesGrouped = samplesDf
        .withColumn("sample_type", col("sample_type")("code"))
        .withColumnRenamed("fhir_id", "sample_id")
        .withColumnRenamed("parent", "fhir_id")

      df.join(samplesGrouped, Seq("fhir_id", "subject", "study_id", "release_id"), "left_outer")
    }

    def addBiospecimen(biospecimenDf: DataFrame): DataFrame = {
      val groupColumns = Seq("subject", "study_id", "release_id")

      val biospecimenGrouped = biospecimenDf
        .groupBy(groupColumns.head, groupColumns.tail: _*)
        .agg(collect_list(struct(biospecimenDf.columns.filterNot(groupColumns.contains).map(col): _*)) as "biospecimens")
        .withColumnRenamed("subject", "participant_id")

      df.join(biospecimenGrouped, Seq("participant_id", "study_id", "release_id"), "left_outer")
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
        .withColumnRenamed("fhir_id", "participant_id")
        .drop("cqdg_participant_id")
    }

    def addCauseOfDeath(causeOfDeath: DataFrame): DataFrame = {
      val cleanCauseOfDeath = causeOfDeath
        .drop("study_id","release_id", "fhir_id")

      df
        .join(cleanCauseOfDeath, col("submitter_participant_ids") === col("fhir_id"), "left_outer")
        .drop("submitter_participant_ids")
    }

    def addSequencingExperiment(sequencingExperiment: DataFrame): DataFrame = {
      val sequencingExperimentClean = sequencingExperiment
        .drop("fhir_id", "study_id", "release_id")
        .withColumnRenamed("_for", "participant_id")

      val seqExperimentByFile = sequencingExperimentClean
        .withColumn("all_files", filter(array(col("alir"), col("snv"), col("gcnv"), col("gsv"), col("ssup")), a => a.isNotNull))
        .withColumn("all_files_exp", explode(col("all_files")))
        .groupBy("all_files_exp")
        .agg(collect_list(struct(sequencingExperimentClean.columns.filterNot(Seq("participant_id", "all_files", "all_files_exp").contains).map(e => col(e)): _*))(0) as "sequencing_experiment")
        .withColumnRenamed("all_files_exp", "fhir_id")

      df.join(seqExperimentByFile, Seq("fhir_id"), "left_outer")
    }

    def addBiospecimenWithSamples(biospecimenDf: DataFrame, sampleRegistrationDf: DataFrame): DataFrame = {
      val sampleRegistrationClean = sampleRegistrationDf
        .withColumn("sample_type", col("sample_type")("code"))
        .groupBy("subject")
        .agg(collect_list(struct(
          col("fhir_id"),
          col("sample_type"),
          col("submitter_participant_id"),
        )) as "samples")

      val biospecimenWithSamplesDf = biospecimenDf
        .join(sampleRegistrationClean, Seq("subject"), "left_outer")
        .withColumn("biospecimen_tissue_source", col("biospecimen_tissue_source")("code"))
        .withColumn("age_biospecimen_collection", col("age_biospecimen_collection")("value"))

      val biospecimenWithSamplesGroupedDf = biospecimenWithSamplesDf
        .groupBy("subject")
        .agg(collect_list(struct(
          biospecimenWithSamplesDf.columns.filterNot("subject" == _).map(col): _*
        )) as "biospecimens")

      df
        .join(biospecimenWithSamplesGroupedDf, col("subject") === col("participant_id"), "left_outer")
        .drop("subject")
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

    def addFilesWithBiospecimen(filesDf: DataFrame, biospecimensDf: DataFrame, seqExperiment: DataFrame, sampleRegistrationDF: DataFrame): DataFrame = {
      val biospecimenWithSample = biospecimensDf
        .addSamplesToBiospecimen(sampleRegistrationDF)

      val biospecimenGrouped = biospecimenWithSample
        .withColumn("biospecimen",
          struct(
            biospecimenWithSample.columns.filterNot(Seq("subject", "study_id", "release_id").contains).map(col): _*)
        )
        .withColumnRenamed("subject", "participant_id")
        .select("participant_id", "study_id", "release_id", "biospecimen")
        .groupBy("participant_id", "study_id", "release_id")
        .agg(collect_list("biospecimen") as "biospecimens")

      val filesWithSeqExp = filesDf
        .withColumn("files_exp", explode(col("files")))
        .addSequencingExperiment(seqExperiment)
        .select("*", "files_exp.*")
        .drop("files", "files_exp")
        .withColumnRenamed("fhir_id", "file_id")

      val filesWithBiospecimen = filesWithSeqExp
        .join(biospecimenGrouped, Seq("participant_id", "study_id", "release_id"), "left_outer")

      val filesGroupedPerParticipant = filesWithBiospecimen
        .groupBy("participant_id",  "study_id", "release_id")
        .agg(collect_list(struct(
          filesWithBiospecimen.columns.filterNot(Seq("participant_id",  "study_id", "release_id").contains).map(col): _*
        )) as "files")

      df.join(filesGroupedPerParticipant, Seq("participant_id", "study_id", "release_id"), "inner")
    }

    def addFiles(filesDf: DataFrame, seqExperiment: DataFrame): DataFrame = {

      val filesWithSeqExp = filesDf
        .addSequencingExperiment(seqExperiment)
        .withColumn("files_exp", explode(col("files")))
        .select("files_exp.*", filesDf.columns.filterNot(Seq("files").contains).:+("sequencing_experiment"): _*)
        .withColumnRenamed("participant_id", "subject")
        .withColumnRenamed("fhir_id", "file_id")

      val filesWithSeqExpGrouped = filesWithSeqExp
        .groupBy("subject", "study_id", "release_id")
        .agg(collect_list(struct(filesWithSeqExp.columns.filterNot(Seq("subject", "study_id", "release_id").contains) map col: _*)) as "files")

      df.join(filesWithSeqExpGrouped, Seq("subject", "study_id", "release_id"), "inner")
    }

    def addParticipant(participantsDf: DataFrame): DataFrame = {
      val reformatParticipant: DataFrame = participantsDf
        .withColumn("participant", struct(participantsDf.columns.map(col): _*))
        .withColumnRenamed("participant_id", "subject")
        .select("subject", "participant")

      df.join(reformatParticipant, Seq("subject"), "inner")
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
        .join(familyWithGroup, col("participant_id") === col("family_members_exp"), "left_outer")
        .drop("family_members_exp")
    }
  }
}

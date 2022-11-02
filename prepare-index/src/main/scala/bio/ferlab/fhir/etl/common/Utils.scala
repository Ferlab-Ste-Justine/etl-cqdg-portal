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
      val biospecimenWithSamples = biospecimenDF.addSamplesToBiospecimen(sampleRegistration)

      val participantsWithBiospecimen = participantDf.addBiospecimen(biospecimenWithSamples)

      val participantsWithBiospecimenGrouped = participantsWithBiospecimen
        .withColumn("participant", struct(participantsWithBiospecimen.columns.filterNot(Seq("study_id", "release_id").contains).map(col): _*))
        .drop(participantsWithBiospecimen.columns.filterNot(Seq("participant", "cqdg_participant_id").contains): _*)

      df.join(participantsWithBiospecimenGrouped, col("cqdg_participant_id") === col("participant_id"), "left_outer")
        .drop("cqdg_participant_id")
    }

    def addSamplesToBiospecimen(samplesDf: DataFrame): DataFrame = {
      val groupColumns = Seq("subject", "parent", "study_id", "release_id")

      val samplesGrouped = samplesDf
        .groupBy(groupColumns.head, groupColumns.tail: _*)
        .agg(collect_list(struct(samplesDf.columns.filterNot(groupColumns.contains).map(col): _*)) as "samples")
        .withColumnRenamed("parent", "fhir_id")


      df.join(samplesGrouped, Seq("fhir_id", "subject", "study_id", "release_id"))
    }

    def addBiospecimen(biospecimenDf: DataFrame): DataFrame = {
      val groupColumns = Seq("subject", "study_id", "release_id")

      val biospecimenGrouped = biospecimenDf
        .groupBy(groupColumns.head, groupColumns.tail: _*)
        .agg(collect_list(struct(biospecimenDf.columns.filterNot(groupColumns.contains).map(col): _*)) as "biospecimens")
        .withColumnRenamed("subject", "fhir_id")

      df.join(biospecimenGrouped, Seq("fhir_id", "study_id", "release_id"))
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

    //TODO - SEQ exp should be added per file using the participant ID or the fileID (alir, svn, ...)???
    def addSequencingExperiment(sequencingExperiment: DataFrame): DataFrame = {
      val sequencingExperimentClean = sequencingExperiment
        .drop("fhir_id", "study_id", "release_id")
        .withColumnRenamed("for", "participant_id")

      df.join(sequencingExperimentClean, Seq("participant_id"), "left_outer")
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

      val cleanSeqExperiment = seqExperiment
        .drop("fhir_id", "study_id", "release_id")

      val filesWithSeqExp = filesDf
        .withColumn("files_exp", explode(col("files")))
        .join(cleanSeqExperiment, col("for") === col("participant_id"), "left_outer")
        .select("*", "files_exp.*")
        .drop("files", "for", "files_exp")

      val filesWithBiospecimen = filesWithSeqExp
        .join(biospecimenGrouped, Seq("participant_id", "study_id", "release_id"), "left_outer")

      val filesGroupedPerParticipant = filesWithBiospecimen
        .groupBy("participant_id",  "study_id", "release_id")
        .agg(collect_list(struct(
          filesWithBiospecimen.columns.filterNot(Seq("participant_id",  "study_id", "release_id").contains).map(col): _*
        )) as "files")
        .withColumnRenamed("participant_id", "cqdg_participant_id")

      df.join(filesGroupedPerParticipant, Seq("cqdg_participant_id", "study_id", "release_id"), "left_outer")
    }

    def addFiles(filesDf: DataFrame, seqExperiment: DataFrame): DataFrame = {

      val filesWithSeqExp = filesDf.addSequencingExperiment(seqExperiment)

      val filesWithSeqExpGrouped = filesWithSeqExp
        .groupBy("participant_id", "study_id", "release_id")
        .agg(collect_list(struct(
          filesWithSeqExp.columns.filterNot(Seq("participant_id", "study_id", "release_id").contains(_)).map(col): _*
        )) as "files")
        .withColumnRenamed("participant_id", "subject")

      df.join(filesWithSeqExpGrouped, Seq("subject", "study_id", "release_id"), "left_outer")
    }

    def addParticipant(participantsDf: DataFrame): DataFrame = {
      val reformatParticipant: DataFrame = participantsDf
              .withColumn("participant", struct(participantsDf.columns.map(col): _*))
              .withColumn("subject", col("fhir_id"))
              .select("subject", "participant")

      df.join(reformatParticipant, Seq("subject"))
    }

    def addFamily(familyDf: DataFrame, familyRelationshipDf: DataFrame): DataFrame = {
      val explodedFamilyDF = familyDf
        .withColumn("family_members_exp", explode(col("family_members")))
        .drop("study_id", "release_id")

      val isProbandDf = familyRelationshipDf
        .select("submitter_participant_id", "relationship_to_proband")
        .withColumn("is_a_proband", when(col("relationship_to_proband") === "Is the proband", lit(true)).otherwise(lit(false)))
        .drop("relationship_to_proband")

      familyRelationshipDf.show(false)
      isProbandDf.show(false)

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

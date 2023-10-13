package bio.ferlab.fhir.etl.common

import bio.ferlab.fhir.etl.common.OntologyUtils._
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions.{when, _}

object Utils {

  implicit class DataFrameOperations(df: DataFrame) {
    def addStudy(studyDf: DataFrame): DataFrame = {
      val requiredStudyCols = Seq("study_code", "name", "population", "domain", "data_access_codes", "contact")
      val cols = studyDf.columns.filter(requiredStudyCols.contains(_))

      val refactorStudyDf = studyDf
        .withColumn("study", struct(cols.map(col): _*))
        .drop(studyDf.columns.filterNot(c => c == "study_id"): _*)

      df.join(refactorStudyDf, Seq("study_id"), "left_outer")
    }

    def addParticipantWithBiospecimen(participantDf: DataFrame, biospecimenDF: DataFrame, sampleRegistration: DataFrame): DataFrame = {
      val biospecimenWithSamples = biospecimenDF
        .addSamplesToBiospecimen(sampleRegistration)
        .withColumn("age_biospecimen_collection", col("age_biospecimen_collection")("value"))
        .withColumnRenamed("fhir_id", "biospecimen_id")

      val biospecimenIdWithParticipant = biospecimenWithSamples.addParticipants(participantDf)

      val biospecimenWithSamplesParticipant = biospecimenWithSamples.join(biospecimenIdWithParticipant, Seq("biospecimen_id"), "left_outer")

      val participantsWithBiospecimen = participantDf
        .addBiospecimen(biospecimenWithSamplesParticipant)

      val participantsWithBiospecimens = participantsWithBiospecimen
        .withColumn("participant", struct(participantsWithBiospecimen.columns.map(col): _*))
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
        .withColumn("sample_type",
          when(col("sample_type")("display").isNotNull,
            concat_ws(" ", col("sample_type")("display"), concat(lit("("), col("sample_type")("code"), lit(")"))))
            .otherwise(col("sample_type")("code"))
        )
        .withColumnRenamed("fhir_id", "sample_id")
        .withColumn("sample_2_id", col("sample_id")) //doubling sample_id portal use
        .withColumnRenamed("parent", "fhir_id")

      df.join(samplesGrouped, Seq("fhir_id", "subject", "study_id", "release_id"), "left_outer")
        .withColumn("biospecimen_tissue_source",
          when(col("biospecimen_tissue_source")("display").isNotNull,
            concat_ws(" ", col("biospecimen_tissue_source")("display"), concat(lit("("), col("biospecimen_tissue_source")("code"), lit(")"))))
            .otherwise(col("biospecimen_tissue_source")("code"))
        )
    }

    def addBiospecimen(biospecimenDf: DataFrame): DataFrame = {
      val groupColumns = Seq("subject", "study_id", "release_id")

      val biospecimenGrouped = biospecimenDf
        .groupBy(groupColumns.head, groupColumns.tail: _*)
        .agg(collect_list(struct(biospecimenDf.columns.filterNot(groupColumns.contains).map(col): _*)) as "biospecimens")
        .withColumnRenamed("subject", "participant_id")

      df.join(biospecimenGrouped, Seq("participant_id", "study_id", "release_id"), "left_outer")
    }

    def addParticipants(participantDf: DataFrame): DataFrame = {
      val biospecimenRenamedDf = df
        .withColumnRenamed("subject", "participant_id")

      val participantGroupedDF = participantDf
        .withColumn("participant", struct(participantDf.columns.map(col): _*))
        .drop(participantDf.columns.filterNot(s => s.equals("participant_id")): _*)

      biospecimenRenamedDf
        .join(participantGroupedDF, Seq("participant_id"), "left_outer")
        .drop(biospecimenRenamedDf.columns.filterNot(_.equals("biospecimen_id")): _*)
    }

    def addDiagnosisPhenotypes(phenotypeDF: DataFrame, diagnosesDF: DataFrame)(hpoTerms: DataFrame, mondoTerms: DataFrame, icdTerms: DataFrame): DataFrame = {
      val (observedPhenotypes, nonObservedPhenotypes, observedPhenotypesWithAncestors, phenotypes) = getTaggedPhenotypes(phenotypeDF, hpoTerms)

      val (diagnosis, mondoWithAncestors) = getDiagnosis(diagnosesDF, mondoTerms, icdTerms)

      df
        .join(diagnosis, col("fhir_id") === col("cqdg_participant_id"), "left_outer")
        .join(mondoWithAncestors, Seq("cqdg_participant_id"), "left_outer")
        .join(observedPhenotypes, Seq("cqdg_participant_id"), "left_outer")
        .join(nonObservedPhenotypes, Seq("cqdg_participant_id"), "left_outer")
        .join(phenotypes, Seq("cqdg_participant_id"), "left_outer")
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
        .withColumnRenamed("_for", "participant_id")
        .withColumnRenamed("fhir_id", "analysis_id")
        .withColumn("type_of_sequencing", when(col("is_paired_end"), lit("Paired Reads")).otherwise(lit("Unpaired Reads")))
        .withColumn("run_date", split(col("run_date"), "\\.")(0))
        .drop("study_id", "release_id", "is_paired_end")

      val seqExperimentByFile = sequencingExperimentClean
        .withColumn("all_files", filter(array(col("alir"), col("snv"), col("gcnv"), col("gsv"), col("ssup")), a => a.isNotNull))
        .withColumn("all_files_exp", explode(col("all_files")))
        .groupBy("all_files_exp")
        .agg(collect_list(struct(sequencingExperimentClean.columns.filterNot(Seq("participant_id", "all_files", "all_files_exp").contains).map(e => col(e)): _*))(0) as "sequencing_experiment")
        .withColumnRenamed("all_files_exp", "fhir_id")

      df.join(seqExperimentByFile, Seq("fhir_id"), "left_outer")
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
      val biospecimenGrouped = biospecimensDf.addSamplesGroupedToBiospecimen(sampleRegistrationDF)

      val filesWithSeqExp = filesDf
        .addAssociatedDocumentRef()
        .addSequencingExperiment(seqExperiment)
        .withColumnRenamed("fhir_id", "file_id")
        .withColumn("file_2_id", col("file_id")) //Duplicate for UI use

      val filesWithBiospecimen = filesWithSeqExp
        .join(biospecimenGrouped, Seq("participant_id", "study_id", "release_id"), "left_outer")

      val filesGroupedPerParticipant = filesWithBiospecimen
        .groupBy("participant_id",  "study_id", "release_id")
        .agg(collect_list(struct(
          filesWithBiospecimen.columns.filterNot(Seq("participant_id",  "study_id", "release_id").contains).map(col): _*
        )) as "files")

      df.join(filesGroupedPerParticipant, Seq("participant_id", "study_id", "release_id"), "inner")
    }

    def addAssociatedDocumentRef(): DataFrame = {
      val refFileFormat = Seq("CRAI", "TBI")

      val filesExp = df.withColumn("files_exp", explode(col("files")))

      val fileColumns =  filesExp.select("files_exp.*").columns

      val filesParent = filesExp.filter(!col("files_exp.file_format").isin(refFileFormat: _*)).drop("relates_to")

      val filesRef = filesExp
        .filter(col("files_exp.file_format").isin(refFileFormat: _*))
        .select("fhir_id", "relates_to")
        .withColumnRenamed("relates_to", "parent_fhir_id")
        .withColumnRenamed("fhir_id", "relates_to")
        .withColumnRenamed("parent_fhir_id", "fhir_id")

      val filesWithRef = filesParent
        .join(filesRef, Seq("fhir_id"), "left_outer")

        .drop("files")

      val fileWithRefColumns = filesWithRef.columns.filterNot(_.equals("files_exp"))

      filesWithRef.select((fileWithRefColumns ++ fileColumns.map(c => s"files_exp.$c")).map(col): _*)
    }


    def addSamplesGroupedToBiospecimen(sampleRegistrationDF: DataFrame) = {
      val biospecimenWithSample = df
        .addSamplesToBiospecimen(sampleRegistrationDF)
        .withColumn("age_biospecimen_collection", col("age_biospecimen_collection")("value"))
        .withColumnRenamed("fhir_id", "biospecimen_id")

      biospecimenWithSample
        .withColumn("biospecimen",
          struct(
            biospecimenWithSample.columns.filterNot(Seq("subject", "study_id", "release_id").contains).map(col): _*)
        )
        .withColumnRenamed("subject", "participant_id")
        .select("participant_id", "study_id", "release_id", "biospecimen")
        .groupBy("participant_id", "study_id", "release_id")
        .agg(collect_list("biospecimen") as "biospecimens")
    }

    def addFiles(filesDf: DataFrame, seqExperiment: DataFrame): DataFrame = {
      val filesWithSeqExp = filesDf
        .addAssociatedDocumentRef()
        .addSequencingExperiment(seqExperiment)
        .withColumnRenamed("participant_id", "subject")
        .withColumnRenamed("fhir_id", "file_id")
        .withColumn("file_2_id", col("file_id"))

      val filesWithSeqExpGrouped = filesWithSeqExp
        .groupBy("subject", "study_id", "release_id")
        .agg(collect_list(struct(filesWithSeqExp.columns.filterNot(Seq("subject", "study_id", "release_id").contains) map col: _*)) as "files")

      df.join(filesWithSeqExpGrouped, Seq("subject", "study_id", "release_id"), "inner")
    }

    def addDataSetToStudy(filesDf: DataFrame, participants: DataFrame, tasks: DataFrame): DataFrame = {
      val filesWithTaskAndParticipants = filesDf
        .addSequencingExperiment(tasks)
        .addAssociatedDocumentRef()
        .join(participants, Seq("participant_id", "study_id", "release_id"), "inner")
        .groupBy("study_id", "dataset")
        .agg(
          collect_set("data_type") as "data_types",
          collect_set("sequencing_experiment.experimental_strategy") as "experimental_strategies",
          size(collect_set("fhir_id")) as "file_count",
          size(collect_set("participant_id")) as "participant_count"
        )

      val studyExpDS = df
        .select("study_id", "data_sets")
        .withColumn("data_sets_exp", explode(col("data_sets")))
        .withColumn("dataset", col("data_sets_exp")("name"))
        .withColumn("dataset_desc", col("data_sets_exp")("description"))

      studyExpDS
        .join(filesWithTaskAndParticipants, Seq("study_id", "dataset"), "left_outer")
        .withColumn("dataset", struct(
          col("dataset") as "name",
          col("dataset_desc") as "description",
          col("data_types"),
          col("experimental_strategies"),
          col("file_count"),
          col("participant_count"),
        ))
        .select("study_id", "dataset")
        .groupBy("study_id")
        .agg(collect_list("dataset") as "datasets")
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

      val wholeFamilyDf = familyRelationshipDf
        .join(explodedFamilyDF, col("family_members_exp") === col("submitter_participant_id"), "left_outer")
        .withColumnRenamed("internal_family_id", "family_id")
        .withColumnRenamed("submitter_participant_id", "participant_id")


      val participantIsAffectedDf = df.select("participant_id", "submitter_participant_id", "is_affected")

      val isProbandDf = familyRelationshipDf
        .select("submitter_participant_id", "relationship_to_proband")
        .withColumn("is_a_proband", when(col("relationship_to_proband") === "Is the proband", lit(true)).otherwise(lit(false)))
        .drop("relationship_to_proband")

      val familyWithGroup = wholeFamilyDf
        .join(participantIsAffectedDf, Seq("participant_id"), "left_outer")
        .groupBy("study_id", "release_id", "family_id", "submitter_family_id")
        .agg(collect_list(struct(
          col("participant_id"),
          col("submitter_participant_id"),
          col("focus_participant_id"),
          col("relationship_to_proband"),
          col("family_id"),
          col("family_type"),
          col("submitter_family_id"),
          col("is_affected"),
        )) as "family_relationships",
          first(col("family_members")) as "family_members",
        )
        .withColumn("family_members_exp", explode(col("family_members")))
        .join(isProbandDf, col("submitter_participant_id") === col("family_members_exp"))
        .drop("submitter_participant_id", "family_members", "study_id", "release_id", "family_id", "submitter_family_id")

      val participantFamilyRelDf =
        wholeFamilyDf
          .select("participant_id", "relationship_to_proband", "family_id", "family_type")

      df
        .join(familyWithGroup, col("participant_id") === col("family_members_exp"), "left_outer")
        .join(participantFamilyRelDf, Seq("participant_id"),"left_outer")
        .drop("family_members_exp")
    }
  }
}

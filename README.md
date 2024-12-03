<p align="center">
  <img src="docs/cqdg_logo.svg" alt="CQDG repository logo" width="660px" />
</p>

## ETL (Clinical && Genomic)

ETL that extracts clinical and genomic data for the CQDG data portal.

## Run the ETL via StepFunctions

The Clinical and Genomic ETLs can now be started via DAGs in Airflow. When the ETL finishes a status will be reported 
to `cqdg-airflow` channel. Each step can be run separately for both clinical data or genomic data.

### Clinical ETL
* DAG ***etl*** (Runs all steps for clinical ETL: etl-fhir-import => etl-import => etl-prepare-index => etl-index => etl-publish)
* DAG ***etl-fhir-import*** (Runs step fhavro-export for clinical ETL)
* DAG ***etl-import*** (Runs step index-task for clinical ETL)
* DAG ***etl-prepare-index*** (Runs step prepare-index for clinical ETL)
* DAG ***etl-index*** (Runs step index-task for clinical ETL)
* DAG ***etl-publish*** (Runs step publish-task for clinical ETL)

Example Input JSON for ***etl*** DAG:
```json
{
  "es_port":"9200",
  "project":"cqdg",
  "release_id":"14",
  "study_codes":"study1,study2,study3,T-DEE"
}
```
* release_id (required) - Release ID passed to Spark Jobs
* study_codes (required)  - List of Study code to run Portal ETL against
* es_port (required) - Port of Elasticsearch cluster, should be 9200
* project - project, should be cqdg


### Genomic ETL 
* DAG ***etl-variant*** (Runs all steps for clinical ETL: enrich-specimen => etl-normalize-variants => 	etl-enrich-variants => etl-prepare-index-variant =>  etl-index)
* DAG ***enrich-specimen*** (Runs step enrich-specimen for genomic ETL, enriching the current specimen tables)
* DAG ***etl-normalize-variants*** (Runs step normalize for snv and for consequences)
* DAG ***etl-enrich-variants*** (Runs step enrich for variants and for consequences)
* DAG ***etl-prepare-index-variant*** (Runs step prepare index for variant_centric, gene_centric, variant_suggestions, gene_suggestions)
* DAG ***etl-index*** (Runs step index-task for genomic ETL)
* DAG ***etl-publish-variant*** (Runs step publish variant_centric, gene_centric, variant_suggestions, gene_suggestions)

```json
{
  "study_code": "XYZ",
  "owner": "person1",
  "dateset_batches": [
    {
      "batches": [
        "annotated_vcf1",
        "annotated_vcf2"
      ],
      "dataset": "dataset_dataset1"
    },
    {
      "batches": [
        "annotated_vcf"
      ],
      "dataset": "dataset_dataset2"
    }
  ],
  "release_id": 1,
  "project": "cqdg",
  "es_port": 9200
}
```
* release_id (required) - Release ID passed to Spark Jobs
* study_code (required)  - study_code
* dateset_batches (required) - Batches to run
* es_port (required) - Port of Elasticsearch cluster, should be 9200
* project - project, should be cqdg

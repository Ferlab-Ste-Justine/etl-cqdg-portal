FROM apache/spark:3.5.1

COPY index-task/pod_templates/pod-template-es-cert.yml /app/pod-template-es-cert.yml

COPY import-task/target/scala-2.12/import-task.jar /app/import-task.jar
COPY index-task/target/scala-2.12/index-task.jar /app/index-task.jar
COPY prepare-index/target/scala-2.12/prepare-index.jar /app/prepare-index.jar
COPY variant-task/target/scala-2.12/variant-task.jar /app/variant-task.jar


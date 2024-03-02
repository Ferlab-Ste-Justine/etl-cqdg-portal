FROM apache/spark:3.4.2

WORKDIR /opt/spark/work-dir

COPY import-task/target/scala-2.12/import-task.jar .
COPY index-task/target/scala-2.12/index-task.jar .
COPY prepare-index/target/scala-2.12/prepare-index.jar .
COPY publish-task/target/scala-2.12/publish-task.jar .
COPY variant-task/target/scala-2.12/variant-task.jar .

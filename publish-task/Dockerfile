FROM openjdk:11

COPY cqdg-qa-os-ca.crt /opt/ca_cqdg_juno_qa.crt
COPY install_ca.sh /opt/install_ca.sh
COPY target/scala-2.12/publish-task.jar .

RUN chmod +x /opt/install_ca.sh && \
    /opt/install_ca.sh && \
    rm /opt/install_ca.sh

ENTRYPOINT ["java", "-jar", "publish-task.jar"]
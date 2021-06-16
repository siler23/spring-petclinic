ARG PETCLINIC_S2I_IMAGE
FROM "${PETCLINIC_S2I_IMAGE}" as intermediate
RUN java -Djarmode=layertools -jar /deployments/*.jar extract

FROM quay.io/gmoney23/adoptopenjdk:openj9 
COPY --from=intermediate /home/jboss/dependencies .
COPY --from=intermediate /home/jboss/spring-boot-loader .
COPY --from=intermediate /home/jboss/application .
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]

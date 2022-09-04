FROM {{ MAVEN_IMAGE }} AS build
COPY . /app
WORKDIR /app
RUN mkdir /root/.m2/ && curl {{ MAVEN_SETTINGS_XML }} -o /root/.m2/settings.xml
RUN mvn -Dmaven.test.skip=true clean package

FROM {{ JRE11_IMAGE }} AS release
USER root
WORKDIR /root
COPY --from=build /app/teammanage-start/target/teammanage.jar /app/teammanage.jar
ENTRYPOINT ["java", "-Xmx100m", "-Xms20m", "-XX:ActiveProcessorCount=2", "-jar", "/app/teammanage.jar"]

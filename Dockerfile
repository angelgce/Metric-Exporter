FROM openjdk:19-slim as builder

WORKDIR /app/

COPY ./pom.xml /app
COPY  ./.mvn ./.mvn
COPY ./mvnw .


RUN ./mvnw clean package -Dmaven.test.skip -Dmaven.main.skip -Dspring-boot.repackage.skip && rm -r ./target/
#RUN ./mvnw dependency:go-offline
COPY ./src ./src

RUN ./mvnw clean package -DskipTests

FROM openjdk:19-slim

WORKDIR /app/

COPY --from=builder /app/target/centos-0.0.1-SNAPSHOT.jar .
EXPOSE 9010

ENTRYPOINT ["java", "-jar", "centos-0.0.1-SNAPSHOT.jar"]
FROM eclipse-temurin:17-jdk-centos7 as build
MAINTAINER wade@rationalagents.com

RUN yum -y install unzip

# Download/unzip Hyper API to /hyperapi
RUN wget -q https://downloads.tableau.com/tssoftware/tableauhyperapi-java-linux-x86_64-release-main.0.0.15735.re214804e.zip -O /wget-out.zip
RUN unzip -q /wget-out.zip -d /hyperapi \
  && mv /hyperapi/*/* /hyperapi

# Install gradle
RUN wget -q https://services.gradle.org/distributions/gradle-7.5.1-bin.zip -O /wget-out.zip
RUN unzip -q /wget-out.zip -d /opt/gradle
ENV PATH="${PATH}:/opt/gradle/gradle-7.5.1/bin"

# Build twbxless.jar (note build.gradle has a ref to lib, thus workdir /hyperapi)
WORKDIR /hyperapi
COPY build.gradle .
COPY src ./src
RUN gradle build

# Now the runtime image, keeping only Hyper API lib and twbxless.jar
FROM eclipse-temurin:17-jre-centos7
COPY --from=build /hyperapi/lib /hyperapi/lib
COPY --from=build /hyperapi/build/libs/twbxless.jar /hyperapi/lib

ENTRYPOINT ["java", \
  "-Dspring.jmx.enabled=false", \
  "-XX:TieredStopAtLevel=1", \
  "-jar","/hyperapi/lib/twbxless.jar"]
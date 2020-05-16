FROM openjdk:11 as build

# Download Hyper API to /hyperapi
RUN wget -q https://downloads.tableau.com/tssoftware/tableauhyperapi-java-linux-x86_64-release-hyperapi_release_7.0.0.10622.rf45095f2.zip -O /wget-out.zip \
  && unzip -q /wget-out.zip -d /hyperapi \
  && mv /hyperapi/*/* /hyperapi

# Install gradle
RUN wget -q https://services.gradle.org/distributions/gradle-6.4-bin.zip -O /wget-out.zip \
  && unzip -q /wget-out.zip -d /opt/gradle
ENV PATH="${PATH}:/opt/gradle/gradle-6.4/bin"

# Build twbxless.jar (note build.gradle has a ref to lib, thus workdir /hyperapi)
WORKDIR /hyperapi
COPY build.gradle .
COPY src ./src
RUN gradle build

# Now the runtime image, keeping only Hyper API lib and twbxless.jar
FROM openjdk:11
COPY --from=build /hyperapi/lib /hyperapi/lib
COPY --from=build /hyperapi/build/libs/twbxless.jar /hyperapi/lib

ENTRYPOINT ["java", \
  "-Dspring.jmx.enabled=false", \
  "-XX:TieredStopAtLevel=1", \
  "-jar","/hyperapi/lib/twbxless.jar"]
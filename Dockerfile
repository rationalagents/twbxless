FROM openjdk:11 as build

# Download specific Java hyperapi version to /hyperapi
RUN wget -q https://downloads.tableau.com/tssoftware/tableauhyperapi-java-linux-x86_64-release-hyperapi_release_7.0.0.10622.rf45095f2.zip -O /wget-out.zip \
  && unzip -q /wget-out.zip -d /hyperapi \
  && mv /hyperapi/*/* /hyperapi

# Install gradle
RUN wget -q https://services.gradle.org/distributions/gradle-6.4-bin.zip -O /wget-out.zip \
  && unzip -q /wget-out.zip -d /opt/gradle
ENV PATH="${PATH}:/opt/gradle/gradle-6.4/bin"

# Build hypersuck
WORKDIR /hyperapi
COPY build.gradle .
COPY src ./src
RUN gradle build

FROM openjdk:11

# Copy Hyper API and hypersuck over
COPY --from=build /hyperapi/lib /hyperapi/lib
COPY --from=build /hyperapi/build/libs/hypersuck-0.0.1.jar /hyperapi/lib/hypersuck.jar

CMD ["java","-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.jmx.enabled=false", \
  "-XX:TieredStopAtLevel=1", \
  "-Djava.net.preferIPv4Stack=true", \
  "-cp","/hyperapi/lib", \
  "-jar","/hyperapi/lib/hypersuck.jar"]
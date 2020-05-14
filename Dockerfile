FROM openjdk:11 as build

# Download Hyper API package to /hyperapi
RUN wget -q https://downloads.tableau.com/tssoftware/tableauhyperapi-java-linux-x86_64-release-hyperapi_release_7.0.0.10622.rf45095f2.zip -O /wget-out.zip \
  && unzip -q /wget-out.zip -d /hyperapi \
  && mv /hyperapi/*/* /hyperapi

# Install gradle
RUN wget -q https://services.gradle.org/distributions/gradle-6.4-bin.zip -O /wget-out.zip \
  && unzip -q /wget-out.zip -d /opt/gradle
ENV PATH="${PATH}:/opt/gradle/gradle-6.4/bin"

# Build hypersuck (note build.gradle has a ref to lib, thus the workdir /hyperapi)
WORKDIR /hyperapi
COPY build.gradle .
COPY src ./src
RUN gradle build

# Start over on runtime image
FROM openjdk:11

# Copy Hyper API and hypersuck from build
COPY --from=build /hyperapi/lib /hyperapi/lib
COPY --from=build /hyperapi/build/libs/hypersuck-0.0.1.jar /hyperapi/lib/hypersuck.jar

CMD ["java","-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.jmx.enabled=false", \
  "-XX:TieredStopAtLevel=1", \
  "-Djava.net.preferIPv4Stack=true", \
  "-jar","/hyperapi/lib/hypersuck.jar"]
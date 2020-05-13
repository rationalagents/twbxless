FROM waded/zyzzy:tableau-hyper1 as build

WORKDIR /workdir
COPY build.gradle .
COPY src ./src
COPY lib ./lib
RUN gradle build

FROM waded/zyzzy:tableau-hyper1

COPY --from=build /workdir/build/libs/hypersuck-0.0.1.jar /hyperapi/lib/hypersuck.jar

CMD ["java","-Djava.security.egd=file:/dev/./urandom", \
  "-Dserver.port=${PORT}", \
  "-Dspring.jmx.enabled=false", \
  "-XX:TieredStopAtLevel=1", \
  "-Djava.net.preferIPv4Stack=true", \
  "-cp","/hyperapi/lib", \
  "-jar","/hyperapi/lib/hypersuck.jar"]
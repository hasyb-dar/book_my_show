FROM openjdk:17-ea


#copy jar file to same directory as dockerfile

COPY target/bms-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]

# docker build -t hasyb/bmsimage:1.0.0 .

# docker push hasyb/bmsimage:1.0.0
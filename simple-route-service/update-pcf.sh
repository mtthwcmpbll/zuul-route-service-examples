mvn clean package -DskipTests
cf push simple-route-service -p target/simple-route-service-1.0.0-SNAPSHOT.jar


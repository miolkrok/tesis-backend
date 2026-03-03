plugins {
    id("java")
    id("io.quarkus") version "3.11.1"
    id("io.freefair.lombok") version "8.6"
}

group = "com.distribuida"
version = "unspecified"

repositories {
    mavenCentral()
}

val quarkusVersion = "3.11.1"
dependencies {
    //Evita poner la version en las siguientes dependencias
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:${quarkusVersion}"))
    implementation("io.quarkus:quarkus-arc") //Implementacion de CDI de quarkus (Motor de comp de negocio)
    implementation("io.quarkus:quarkus-resteasy-reactive") //Motor de rest JAXRS en su forma reactiva
    implementation("io.quarkus:quarkus-resteasy-reactive-jackson") //JSON
    implementation("io.quarkus:quarkus-hibernate-orm-panache") //JPA Hibernate+ repo

    //REST CLIENT
    implementation("io.quarkus:quarkus-rest-client-reactive")
    implementation("io.quarkus:quarkus-rest-client-reactive-jackson")

    implementation("org.postgresql:postgresql:42.7.3")
    implementation("io.quarkus:quarkus-jdbc-postgresql:3.11.2")

    implementation("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")

    // LoadBalancer
    //    implementation("io.smallrye.stork:stork-service-discovery-static-list:2.6.0")
    // Para reconocer al servidor Consul
    implementation("io.smallrye.stork:stork-service-discovery-consul:2.6.0")

    // Registros: libreria para interactuar con el servidor de registros
    // Con esta libreria cada que se levante una aplicacion de este API, Consul la reconocera
    implementation("io.smallrye.reactive:smallrye-mutiny-vertx-consul-client")

    //HEALTH
    implementation("io.quarkus:quarkus-smallrye-health")

    // JWT y Seguridad
    implementation("io.quarkus:quarkus-smallrye-jwt")
    implementation("io.quarkus:quarkus-smallrye-jwt-build")
    implementation("io.quarkus:quarkus-security-jpa")

    // Azure Blob Storage SDK
    implementation("com.azure:azure-storage-blob:12.25.1")


    // AWS SDK para S3 (versión 2.x compatible con Quarkus)
    implementation("software.amazon.awssdk:s3:2.20.26")
    implementation("software.amazon.awssdk:netty-nio-client:2.20.26")
}

tasks.test {
    useJUnitPlatform()
}
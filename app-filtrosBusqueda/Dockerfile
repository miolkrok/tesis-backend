#GENERAR LA IMAGEN
# docker build -t app-filtro .
# docker tag app-authors miolkrok/app-filtro
#
# docker build -t miolkrok/app-filtros-busqueda .

FROM eclipse-temurin:17.0.11_9-jre-jammy

RUN mkdir /app
WORKDIR /app

COPY build/quarkus-app/app ./app
COPY build/quarkus-app/lib/ ./lib
COPY build/quarkus-app/quarkus/ ./quarkus
COPY build/quarkus-app/quarkus-run.jar app.jar


CMD ["java","-jar","app.jar"]
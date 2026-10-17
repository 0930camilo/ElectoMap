FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends tesseract-ocr libtesseract5 libgomp1 \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /workspace/target/gestion-campania-backend-*.jar app.jar
COPY tessdata ./tessdata
ENV OCR_TESSDATA_PATH=/app/tessdata
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

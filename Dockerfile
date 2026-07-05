# ---- build ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 의존성 레이어 캐시: 소스보다 빌드 스크립트를 먼저 복사한다.
COPY gradlew ./
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN ./gradlew --no-daemon dependencies --quiet || true

COPY src src
RUN ./gradlew --no-daemon bootJar

# ---- runtime ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/maribel-backend-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

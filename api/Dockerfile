# ── 1단계: 빌드 ──
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# 의존성 목록만 먼저 넣어 두면, 소스만 바뀌었을 때 내려받기를 다시 하지 않는다.
COPY gradle ./gradle
COPY gradlew settings.gradle build.gradle ./
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies

COPY src ./src
# 테스트는 CI 에서 이미 돌린다. 이미지 만들 때 또 돌리지 않는다.
RUN ./gradlew --no-daemon bootJar -x test

# ── 2단계: 실행 ──
FROM eclipse-temurin:21-jre

WORKDIR /app

# 영상 썸네일을 뽑는 데 쓴다. 없으면 업로드는 되지만 썸네일이 안 붙는다.
RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg \
    && rm -rf /var/lib/apt/lists/*

# 루트로 돌리지 않는다.
RUN groupadd --system app && useradd --system --gid app app

COPY --from=build /app/build/libs/*.jar app.jar

# 업로드 파일이 담길 곳. 컨테이너를 지워도 남게 하려면 볼륨으로 붙인다.
RUN mkdir -p /app/uploads && chown -R app:app /app

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# ── 1단계: 빌드 ──
FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

# 의존성 목록만 먼저 넣어 두면, 소스만 바뀌었을 때 내려받기를 다시 하지 않는다.
COPY gradle ./gradle
COPY gradlew settings.gradle build.gradle ./
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies

COPY src ./src
# 테스트는 CI 에서 이미 돌린다. 이미지 만들 때 또 돌리지 않는다.
RUN ./gradlew --no-daemon bootJar -x test

# ── 2단계: 실행 ──
FROM eclipse-temurin:25-jre

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

# 무료 서버(메모리 512MB)에서 자바가 차지하는 몫을 줄이는 옵션. 힙 크기(-Xmx)는 서비스 환경변수(JAVA_TOOL_OPTIONS)가 정한다.
#  -XX:TieredStopAtLevel=1    JIT 를 C1 만 쓴다. 코드 캐시·컴파일 스레드 메모리와 기동 때 CPU 를 아낀다.
#  -Xss512k                   스레드 하나당 스택을 줄인다.
#  -XX:MaxDirectMemorySize    네트워크·파일 버퍼(네이티브)가 무한히 커지지 않게 막는다.
ENTRYPOINT ["java", "-XX:TieredStopAtLevel=1", "-Xss512k", "-XX:ReservedCodeCacheSize=64m", "-XX:MaxDirectMemorySize=64m", "-jar", "/app/app.jar"]

# 배포

컨테이너 세 개(`api` · `web` · `streaming`)와 MySQL 로 돕니다.
루트 `docker-compose.yml` 의 `full` 프로필이 그 전부를 한 번에 띄웁니다.

---

## 필요한 것

| | |
|---|---|
| Docker | 이미지 빌드·실행 |
| Java 21 · Node.js 20+ | 컨테이너 없이 직접 돌릴 때만 |

빌드는 컨테이너 안에서 하므로, 배포하는 기계에는 Docker 만 있으면 됩니다.

---

## 한 번에 띄우기

```bash
echo "JWT_SECRET=$(openssl rand -hex 32)" > .env
docker compose --profile full up -d --build
```

| 주소 | 무엇 |
|---|---|
| http://localhost:3000 | 화면 (nginx 가 정적 파일 서빙) |
| http://localhost:8080 | API |
| http://localhost:8081/hls | HLS 재생 |
| `rtmp://localhost:1935/live` | OBS 송출 |

`JWT_SECRET` 은 기본값이 없습니다. 안 넣으면 API 가
`jwt.secret 은 최소 32바이트여야 합니다` 로 멈춥니다. 일부러 그렇게 뒀습니다.

평소 개발할 때 쓰는 `docker compose up -d` 는 그대로 MySQL 과 스트리밍 서버만 띄웁니다.
`api` · `web` 은 `full` 프로필에만 들어 있습니다.

---

## 이미지 구성

### `api/Dockerfile`

```
eclipse-temurin:21-jdk  →  ./gradlew bootJar -x test
eclipse-temurin:21-jre  →  java -jar app.jar
```

- 의존성 목록(`gradle/`, `build.gradle`)을 소스보다 먼저 넣어, 소스만 바뀌면 다시 내려받지 않습니다.
- 테스트는 CI 에서 이미 돌았으므로 이미지 빌드에서는 건너뜁니다.
- 실행 이미지에 **ffmpeg** 을 넣습니다. 영상 썸네일을 뽑는 데 씁니다.
  빼면 업로드는 되지만 썸네일이 안 붙습니다.
- 루트가 아닌 `app` 사용자로 돕니다.
- 업로드 파일은 `/app/uploads` 에 쌓이고, compose 가 `api-uploads` 볼륨을 붙여 둡니다.
  `docker compose down` 으로는 지워지지 않습니다. 지우려면 `down -v`.

### `web/Dockerfile`

```
node:22-alpine  →  npm ci && npm run build
nginx:1.27-alpine  →  dist/ 를 :80 으로
```

- `web/nginx.conf` 가 SPA 새로고침을 처리합니다(`try_files ... /index.html`).
- `/assets/` 는 파일 이름에 해시가 붙으므로 1년 캐시, `index.html` 은 `no-cache`.

### `streaming/Dockerfile`

`tiangolo/nginx-rtmp` 에 `streaming/nginx.conf` 를 얹습니다.
송출 입구(1935)와 HLS 를 만드는 안쪽 통로(1936)가 나뉘어 있습니다 —
이유는 [03-api-spec.md](03-api-spec.md) 참고.

---

## 설정

값을 바꾸는 곳은 `docker-compose.yml` 의 `api.environment`,
그 값을 읽는 곳은 `api/src/main/resources/application-docker.yaml` 입니다.
이미지 안에는 비밀을 넣지 않습니다.

| 환경변수 | 기본값 | 설명 |
|---|---|---|
| `JWT_SECRET` | 없음 (필수) | HS256 서명 키. 32바이트 이상 |
| `JWT_EXPIRATION` | `3600000` | 액세스 토큰 만료(ms) |
| `JWT_REFRESH_EXPIRATION` | `1209600000` | 리프레시 토큰 만료(ms) |
| `DB_URL` | `jdbc:mysql://mysql:3306/streaming?...` | 컨테이너끼리는 서비스 이름으로 찾아갑니다 |
| `DB_USERNAME` · `DB_PASSWORD` | `streaming` | |
| `DDL_AUTO` | `update` | |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | 화면 주소 |
| `HLS_BASE_URL` | `http://localhost:8081/hls` | **시청자 브라우저가 직접 여는 주소** |
| `ADMIN_EMAILS` | 비어 있음 | 기동 시 ADMIN 을 줄 계정 (쉼표 구분) |
| `LOG_LEVEL` | `INFO` | |

### 다른 주소로 서비스할 때

Vite 는 API 주소를 **빌드할 때 코드에 박아 넣습니다.** 컨테이너를 다시 띄우는 것만으로는 안 바뀝니다.

```bash
VITE_API_BASE_URL=https://api.내도메인 docker compose --profile full up -d --build web
```

같이 바꿔야 하는 것:

- `api.environment.CORS_ALLOWED_ORIGINS` → 화면 주소
- `api.environment.HLS_BASE_URL` → 바깥에서 닿는 HLS 주소
- `streaming/nginx.conf` 의 `on_publish` 콜백 주소 (API 가 컨테이너 밖에 있을 때만)

---

## GitHub Actions

| 워크플로 | 언제 | 하는 일 |
|---|---|---|
| `ci.yml` | PR · main | API `./gradlew build`, Web `lint` → `build` |
| `docker.yml` | PR · main | 이미지 세 개를 실제로 빌드. main 이면 GHCR 로 푸시 |

`docker.yml` 은 PR 에서 **만들기만 하고 올리지 않습니다.** Dockerfile 이 깨졌는지 여기서 걸러집니다.
`main` 에 들어가면 `ghcr.io/ginye28/streaming-platform/{api,web,streaming}` 으로 올라갑니다.
태그는 `latest` 와 커밋 SHA 두 가지입니다.

푸시는 `GITHUB_TOKEN` 으로 하므로 따로 넣어 둘 비밀값이 없습니다.

---

## 아직 안 한 것

- **서버에 올리는 자동화(CD).** 이미지는 GHCR 까지 올라가지만,
  그걸 받아서 다시 띄우는 건 아직 손으로 합니다.
- **HTTPS.** 인증서와 리버스 프록시가 없습니다.
  실제 도메인에 붙일 때는 `web` 앞에 프록시를 하나 더 두는 게 편합니다.
- **DB 백업.** `mysql-data` 볼륨에만 있습니다.

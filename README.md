# Streaming Platform

실시간 스트리밍 플랫폼 개인 프로젝트입니다.

## 기술 스택

| 영역 | 사용 기술 |
|---|---|
| Backend | Java 21, Spring Boot 4, Spring Security, Spring Data JPA, JWT |
| Frontend | React 19, Vite, hls.js |
| Database | MySQL 8 |
| Streaming | nginx-rtmp (RTMP 수신 → HLS 변환) |

## 프로젝트 구조

```
api/         Spring Boot 백엔드
web/         React 프론트엔드
streaming/   nginx-rtmp 도커 구성
docs/        요구사항 · ERD · API 명세 · 컨벤션
```

---

## 사전 준비

- JDK 21
- Node.js 20 이상
- Docker Desktop
- Git

> **Windows 사용자**: 아래 명령은 **CMD 기준**입니다.
> PowerShell 이라면 `gradlew.bat` 앞에 `.\` 를 붙이세요 (`.\gradlew.bat`).
> Git Bash 를 쓴다면 macOS/Linux 명령을 그대로 쓰면 됩니다 (`./gradlew`).

---

## 내려받기

```bash
git clone https://github.com/ginye28/streaming-platform.git
cd streaming-platform
```

이미 받아 뒀다면 그 폴더로 이동하면 됩니다.
**`api` 폴더가 보이는 위치**에서 아래 명령들을 실행하세요.

---

## 실행하기

DB 를 어떻게 준비하느냐에 따라 세 가지 방법이 있습니다. **A 가 가장 간단합니다.**

| 방법 | 필요한 것 | 설정 파일 |
|---|---|---|
| A. H2 | 없음 | 필요 없음 |
| B. Docker MySQL | Docker | 필요 없음 |
| C. 직접 설치한 MySQL | MySQL | 2개 만들어야 함 |

### A. DB 없이 바로 켜기

MySQL 없이 H2(파일 기반)로 돕니다. 처음이거나 OBS 만 확인할 때 권장합니다.

**Windows (CMD)**
```bat
cd api
gradlew.bat bootRun --args="--spring.profiles.active=local"
```

**macOS / Linux / Git Bash**
```bash
cd api
./gradlew bootRun --args='--spring.profiles.active=local'
```

- 데이터는 `api/data/` 에 저장되어 서버를 껐다 켜도 남습니다.
- 데이터를 눈으로 보려면: http://localhost:8080/h2-console
  (JDBC URL `jdbc:h2:file:./data/streaming`, 사용자 `sa`, 비밀번호 없음)
- 처음부터 다시 시작하려면 `api/data/` 폴더를 지우면 됩니다.

### B. Docker 로 MySQL 띄우기

프로젝트 루트에서 MySQL 컨테이너를 띄웁니다. DB 를 직접 설치하지 않아도 됩니다.

```bash
docker compose up -d mysql
```

`healthy` 가 될 때까지 기다린 뒤(처음엔 30초쯤 걸립니다) 실행합니다.

```bash
docker compose ps          # STATUS 가 healthy 인지 확인
```

**Windows (CMD)**
```bat
cd api
gradlew.bat bootRun --args="--spring.profiles.active=mysql"
```

**macOS / Linux / Git Bash**
```bash
cd api
./gradlew bootRun --args='--spring.profiles.active=mysql'
```

접속 정보 (컨테이너 기본값, `docker-compose.yml` 에 정의):

| 항목 | 값 |
|---|---|
| host / port | `localhost:3306` |
| database | `streaming` |
| user / password | `streaming` / `streaming` |
| root password | `root` |

- DB 데이터는 도커 볼륨에 남습니다. `docker compose down` 해도 유지됩니다.
- 완전히 초기화하려면 `docker compose down -v`

### C. 직접 설치한 MySQL 로 실행

**1. 데이터베이스 생성**

```sql
CREATE DATABASE streaming
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

**2. 로컬 설정 파일 만들기**

두 파일은 `.gitignore` 처리되어 있어 직접 만들어야 합니다.

```bash
cd api/src/main/resources
cp application-secret.yaml.example application-secret.yaml
cp application-db.yaml.example    application-db.yaml
```

- `application-secret.yaml` → `jwt.secret` 은 **최소 32바이트**여야 합니다.
  짧으면 애플리케이션이 기동되지 않습니다.
- `application-db.yaml` → DB 계정과 비밀번호

**3. 실행** — 프로필 없이 그냥 실행합니다.

```bash
cd api
./gradlew bootRun
```

> A · B 프로필의 JWT 시크릿은 개발 전용 고정값입니다. 배포에는 쓰지 마세요.

### 공통

실행 후:

- API: http://localhost:8080
- Swagger: http://localhost:8080/swagger

**프론트엔드**

```bash
cd web
cp .env.example .env   # 최초 1회 (Windows CMD: copy .env.example .env)
npm install            # 최초 1회
npm run dev
```

http://localhost:5173

**스트리밍 서버** — 프로젝트 루트에서 실행합니다.

```bash
docker compose up -d streaming
```

- RTMP 수신: `rtmp://localhost:1935/live`
- HLS 재생: 백엔드가 알려주는 `hlsUrl`
- 상태 확인: `curl http://localhost:8081/health`

> 스트리밍 서버는 송출 시작 시 백엔드(`8080`)로 스트림 키 검증 요청을 보냅니다.
> **백엔드가 떠 있지 않으면 송출이 거부됩니다.**

MySQL 과 스트리밍 서버를 한 번에 띄우려면:

```bash
docker compose up -d
```

---

## OBS로 방송 송출하기

백엔드와 스트리밍 서버를 켠 뒤, 아래 스크립트를 실행하면 계정 준비부터
결과 확인까지 알아서 해 줍니다. **프로젝트 루트에서** 실행하세요.

**Windows (PowerShell)**
```powershell
powershell -ExecutionPolicy Bypass -File scripts\verify-live.ps1
```

**macOS / Linux / Git Bash**
```bash
./scripts/verify-live.sh
```

스크립트가 OBS 에 넣을 값을 알려주고, [방송 시작] 을 누르면
방송이 잡히는지 · 재생 URL 이 만들어지는지 · 스트림 키가 노출되지 않는지까지 확인합니다.

**직접 해보려면**

1. 회원가입 후 로그인해 토큰을 받습니다.
2. `GET /api/users/stream-key` 로 본인의 스트림 키를 조회합니다.
3. OBS → 설정 → 방송
   - 서비스: **사용자 지정...**
   - 서버: `rtmp://localhost:1935/live`
   - 스트림 키: 2번에서 받은 값
4. 방송 시작 후 `GET /api/lives` 에 방송이 뜨는지 확인합니다.
5. 응답의 `hlsUrl` 끝부분(공개 이름)으로 재생합니다.
   `http://localhost:5173/?stream={공개이름}`

스트림 키가 노출됐다면 `POST /api/users/stream-key/regenerate` 로 재발급하세요.

### 스트림 키가 재생 URL 에 안 보이는 이유

`on_publish` 가 302 리다이렉트로 송출 이름을 **공개 이름**(계정마다 다른 UUID)으로
바꾸기 때문입니다. 덕분에 시청자에게 주는 재생 URL 에 송출 키가 들어가지 않습니다.

> ⚠️ 이 리다이렉트는 실제 OBS·nginx 조합에서 확인이 필요합니다.
> API 쪽 동작은 테스트로 검증했지만, nginx 가 302 를 받아 실제로 이름을 바꾸는지는
> 송출을 해봐야 압니다.
>
> **송출이 거부되면** `api/src/main/resources/application.yaml` 에서
> `app.rtmp.rename-on-publish: false` 로 바꾸고 다시 시도하세요.
> 송출은 되지만 재생 URL 에 스트림 키가 그대로 드러납니다.

---

## 테스트

```bash
cd api
./gradlew test          # 리포지토리 회귀 테스트 + API 통합 테스트
```

테스트는 H2 인메모리 DB를 사용하므로 **MySQL이나 로컬 설정 파일 없이도 실행**됩니다.
실패 시 리포트: `api/build/reports/tests/test/index.html`

```bash
cd web
npm run lint
npm run build
```

---

## CI

`.github/workflows/ci.yml` 이 모든 PR과 `main` 푸시에서 자동 실행됩니다.

- **API**: `./gradlew build` (컴파일 + 테스트). 실패 시 테스트 리포트가 아티팩트로 올라갑니다.
- **Web**: `npm ci` → `npm run lint` → `npm run build`

머지 전에 CI가 초록인지 확인하세요. 로컬에서 같은 명령을 그대로 돌려볼 수 있습니다.

---

## 구현 상태

| 기능 | 상태 |
|---|---|
| 회원가입 · 로그인 (JWT) | 완료 |
| 영상 등록 · 조회 · 수정 · 삭제 | 완료 |
| 영상 검색 · 인기 · 최신 | 완료 |
| 댓글 CRUD | 완료 |
| 좋아요 · 구독 (상태·개수 조회 포함) | 완료 |
| 채널 페이지 · 구독 피드 | 완료 |
| 프로필 수정 · 비밀번호 변경 | 완료 |
| 파일 업로드 | 완료 |
| RTMP 송출 + HLS 재생 | 기본 동작 |
| 라이브 방송 (시작·종료·목록·지난 방송) | 완료 |
| 실시간 채팅 (WebSocket) · 채팅 저장 | 완료 |
| 동시 시청자 수 | 완료 (단일 서버 기준) |
| 방송 시작 알림 | 완료 |
| 로그아웃 (토큰 무효화) | 미착수 — 리프레시 토큰 설계 필요 |
| 소셜 로그인 | 엔티티만 준비 (`Provider`) |

API 상세는 [docs/03-api-spec.md](docs/03-api-spec.md) 참고.

---

## 다음 할 일

1. **실제 OBS 로 송출 확인** — `./scripts/verify-live.sh` 로 확인.
   `on_publish` 302 리다이렉트가 기대대로 동작하는지가 핵심입니다.
2. **프론트 화면** — 현재는 재생 확인용 최소 화면만 있습니다.
   라이브 목록 · 채널 페이지 · 채팅창 · 알림함이 API 는 준비돼 있습니다
3. **리프레시 토큰 + 로그아웃** — 지금은 액세스 토큰만 있어 만료 시 재로그인해야 하고,
   서버가 토큰을 무효화할 수 없습니다
4. **Redis 도입** — 시청자 수 집계와 채팅 브로커를 서버 여러 대로 확장할 때 필요
5. **카테고리 · 신고/차단** 등 운영 기능

---

## 자주 겪는 문제

**애플리케이션이 기동되지 않고 `jwt.secret 은 최소 32바이트여야 합니다` 가 뜬다**
→ `application-secret.yaml` 의 `jwt.secret` 을 32자 이상으로 늘리세요.
설정 파일을 만들기 귀찮다면 A 방법(`--spring.profiles.active=local`)으로 실행하세요.

**MySQL 이 없어서 서버가 안 뜬다**
→ A 방법으로 실행하면 DB 설치 없이 H2 로 돕니다.

**업로드한 영상이 404 로 안 나온다**
→ 파일은 `api/uploads/` 에 저장되고 `/uploads/{파일명}` 으로 서빙됩니다.
백엔드를 `api/` 디렉터리에서 실행했는지 확인하세요 (경로가 실행 위치 기준입니다).

**프론트에서 API 호출 시 CORS 오류**
→ `api/src/main/resources/application.yaml` 의 `app.cors.allowed-origins` 에
프론트 주소가 포함되어 있는지 확인하세요.

**OBS 에서 "서버에 연결할 수 없습니다"**
→ 백엔드가 떠 있어야 스트림 키 검증이 통과합니다. 스트림 키가 맞는지도 확인하세요.
`docker compose logs -f` 로 nginx 로그를 볼 수 있습니다.

**지연 로딩 관련 오류가 배포 후에 터진다**
→ `application-db.yaml` 에 `spring.jpa.open-in-view: false` 를 넣고 개발하세요.
숨은 지연 로딩 문제를 개발 단계에서 미리 잡을 수 있습니다.

---

## 개발 기간

2026.07 ~

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
- MySQL 8
- Docker (스트리밍 서버용)

---

## 처음 실행하기

### 1. 데이터베이스 생성

```sql
CREATE DATABASE streaming
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

### 2. 로컬 설정 파일 만들기

두 파일은 `.gitignore` 처리되어 있어 직접 만들어야 합니다.
예시 파일을 복사한 뒤 값을 채우세요.

```bash
cd api/src/main/resources
cp application-secret.yaml.example application-secret.yaml
cp application-db.yaml.example    application-db.yaml
```

- `application-secret.yaml` → `jwt.secret` 은 **최소 32바이트**여야 합니다.
  짧으면 애플리케이션이 기동되지 않습니다.
- `application-db.yaml` → DB 계정과 비밀번호

### 3. 백엔드 실행

```bash
cd api
./gradlew bootRun
```

- API: http://localhost:8080
- Swagger: http://localhost:8080/swagger

### 4. 프론트엔드 실행

```bash
cd web
cp .env.example .env   # 최초 1회
npm install            # 최초 1회
npm run dev
```

http://localhost:5173

### 5. 스트리밍 서버 실행

```bash
cd streaming
docker compose up -d --build
```

- RTMP 수신: `rtmp://localhost:1935/live`
- HLS 재생: `http://localhost:8081/hls/{스트림키}.m3u8`
- 상태 확인: `curl http://localhost:8081/health`

> 스트리밍 서버는 송출 시작 시 백엔드(`8080`)로 스트림 키 검증 요청을 보냅니다.
> **백엔드가 떠 있지 않으면 송출이 거부됩니다.**

---

## OBS로 방송 송출하기

1. 회원가입 후 로그인해 토큰을 받습니다.
2. `GET /api/users/stream-key` 로 본인의 스트림 키를 조회합니다.
3. OBS → 설정 → 방송
   - 서비스: **사용자 지정**
   - 서버: `rtmp://localhost:1935/live`
   - 스트림 키: 2번에서 받은 값
4. 방송 시작 후 `http://localhost:5173/?stream={스트림키}` 에서 재생을 확인합니다.

키가 노출됐다면 `POST /api/users/stream-key/regenerate` 로 재발급하세요.

> ⚠️ 현재는 송출 이름이 곧 스트림 키라서 재생 URL에 키가 그대로 드러납니다.
> `on_publish` 응답에서 공개 채널명으로 rename 하도록 바꾸는 것이 다음 과제입니다.

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
| 로그아웃 (토큰 무효화) | 미착수 — 리프레시 토큰 설계 필요 |
| 방송 상태 (LIVE/종료) | 미착수 — 라이브 모델 결정 후 |
| 실시간 채팅 | 미착수 |
| 알림 | 미착수 |
| 소셜 로그인 | 엔티티만 준비 (`Provider`) |

API 상세는 [docs/03-api-spec.md](docs/03-api-spec.md) 참고.

---

## 다음 할 일

1. **라이브 / VOD 모델 정리** — `Stream` 엔티티는 현재 업로드 영상(VOD) 기준입니다.
   라이브 방송을 별도 엔티티로 분리할지, `Stream` 에 `isLive` · `hlsUrl` 을 추가할지
   먼저 정해야 이후 작업이 꼬이지 않습니다.
2. **스트림 키 노출 제거** — `on_publish` 3xx 리다이렉트로 공개 채널명 rename
3. **방송 상태 관리** — `on_publish` / `on_publish_done` 콜백으로 LIVE ↔ 종료 전환
4. **실시간 채팅** — WebSocket
5. **프론트 화면** — 현재는 재생 확인용 최소 화면만 있습니다

---

## 자주 겪는 문제

**애플리케이션이 기동되지 않고 `jwt.secret 은 최소 32바이트여야 합니다` 가 뜬다**
→ `application-secret.yaml` 의 `jwt.secret` 을 32자 이상으로 늘리세요.

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

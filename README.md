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

작업 브랜치로 이동합니다. `git fetch` 를 먼저 해야 로컬이 브랜치를 인식합니다.

```bash
git fetch origin
git checkout claude/code-review-26k8iw
```

> `error: pathspec ... did not match any file(s) known to git` 가 뜨면
> `git fetch origin` 을 빠뜨린 것입니다. 그래도 안 되면 아래처럼 명시적으로:
> ```bash
> git checkout -b claude/code-review-26k8iw origin/claude/code-review-26k8iw
> ```

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

## 관리자 계정 만들기

카테고리 생성과 신고 처리는 `ADMIN` 권한이 필요합니다.
승격 API 자체도 관리자만 쓸 수 있으므로, **첫 관리자는 설정으로 만듭니다.**

**1. 평범하게 회원가입합니다.**

```bash
curl -X POST http://localhost:8080/api/auth/signup ^
  -H "Content-Type: application/json" ^
  -d "{\"email\":\"me@example.com\",\"password\":\"password123\",\"nickname\":\"관리자\"}"
```

(PowerShell 에서는 `curl` 이 아니라 `curl.exe` 를 쓰세요.)

**2. 그 이메일을 `app.admin.emails` 에 넣고 서버를 재시작합니다.**

한 번만 쓸 거라면 실행 인자로:

```bash
gradlew.bat bootRun --args="--spring.profiles.active=local --app.admin.emails=me@example.com"
```

계속 쓸 거라면 `api/src/main/resources/application.yaml` 에 적어 둡니다:

```yaml
app:
  admin:
    emails: "me@example.com"     # 쉼표로 여러 개 가능
```

**3. 기동 로그를 확인합니다.**

```
INFO ... AdminBootstrap : 관리자로 승격: me@example.com
```

계정이 아직 없으면 경고만 남기고 넘어갑니다. 먼저 가입부터 하세요.

**4. 이후로는 API 로 다른 사람을 승격시킬 수 있습니다.**

```
GET   /api/admin/users              # 사용자 목록에서 userId 확인
PATCH /api/admin/users/{userId}/role   {"role":"ADMIN"}
```

자기 자신의 관리자 권한은 해제할 수 없습니다 (되돌릴 방법이 없어지므로).

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
| 리프레시 토큰 · 로그아웃 | 완료 |
| 카테고리 (영상 분류, 관리자 생성) | 완료 |
| 사용자 차단 (피드 필터링) | 완료 |
| 신고 (접수 · 관리자 처리) | 완료 |
| 관리자 계정 발급 (설정 부트스트랩 + 승격 API) | 완료 |
| 소셜 로그인 | 엔티티만 준비 (`Provider`) |

API 상세는 [docs/03-api-spec.md](docs/03-api-spec.md) 참고.

---

## 다음 할 일

1. **실제 OBS 로 송출 확인** — `./scripts/verify-live.sh` 로 확인.
   `on_publish` 302 리다이렉트가 기대대로 동작하는지가 핵심입니다.
2. **프론트 화면** — 현재는 재생 확인용 최소 화면만 있습니다.
   라이브 목록 · 채널 페이지 · 채팅창 · 알림함이 API 는 준비돼 있습니다.
   프론트에서 액세스 토큰이 만료되면 `POST /api/auth/refresh` 로 재발급받도록
   붙이는 작업이 아직 남아 있습니다 (API 자체는 완료).
3. **Redis 도입** — 시청자 수 집계와 채팅 브로커를 서버 여러 대로 확장할 때 필요
4. **관리자 화면** — 신고 처리·카테고리 관리 API 는 준비됐지만 화면이 없습니다.

---

## 자주 겪는 문제

**`verify-live.ps1` 실행 시 `예기치 않은 토큰` / `종결자가 없습니다` 오류**

Windows PowerShell 5.1 은 BOM 이 없는 `.ps1` 파일을 UTF-8 이 아니라
시스템 코드페이지(한국어 Windows 는 CP949)로 읽습니다. 파일 안의 한글이
깨지면서 따옴표 짝이 어긋나 구문 오류가 납니다.

저장소의 스크립트는 **UTF-8 BOM + CRLF** 로 관리되고 `.gitattributes` 가
줄바꿈을 고정합니다. 오류가 난다면 최신 코드를 받으세요.

```powershell
git pull
```

직접 `.ps1` 을 수정할 때도 **UTF-8 with BOM** 으로 저장해야 합니다.


**PowerShell 에서 `curl` 이 보안 경고를 띄우며 멈춘다**

Windows PowerShell 5.1 에서 `curl` 은 진짜 curl 이 아니라
`Invoke-WebRequest` 의 별칭입니다. 응답을 IE 엔진으로 파싱하려다 경고가 뜹니다.

셋 중 아무거나 쓰면 됩니다.

```powershell
curl.exe http://localhost:8081/health          # Windows 10+ 에 내장된 진짜 curl
Invoke-RestMethod http://localhost:8081/health # PowerShell 방식
curl http://localhost:8081/health -UseBasicParsing
```

이 README 의 `curl` 예시는 macOS/Linux/Git Bash 기준입니다.
PowerShell 에서는 `curl.exe` 로 바꿔 쓰세요.


**`bootRun` 이 80% EXECUTING 에서 안 끝난다**

**정상입니다.** `bootRun` 은 서버를 계속 켜 두는 명령이라 끝나지 않습니다.
빌드처럼 완료되고 프롬프트가 돌아오는 게 아니라, 서버가 살아 있는 동안
계속 `EXECUTING` 으로 표시됩니다. 17분이든 5시간이든 그대로입니다.

위로 스크롤해 `Started ApiApplication in ...` 이 보이면 이미 떠 있는 것입니다.
확인은 브라우저로: http://localhost:8080/swagger

- **그 터미널 창은 그대로 두세요.** 닫으면 서버가 꺼집니다.
- 다른 명령은 **새 터미널 창**에서 실행하세요.
- 서버를 끄려면 그 창에서 `Ctrl + C`


**`Please set the JAVA_HOME variable in your environment` (Windows)**
**`ERROR: JAVA_HOME is set to an invalid directory` (Windows)**

JDK 21 이 없거나 `JAVA_HOME` 이 **실제로 존재하지 않는 경로**를 가리키는 경우입니다.
`'""'은(는) 내부 또는 외부 명령...` 이 함께 뜨는 것도 같은 원인입니다.

> ⚠️ 아래 경로의 버전 번호(`21.0.x.y`)는 설치할 때마다 다릅니다.
> **그대로 복사하지 말고 반드시 본인 PC 의 실제 경로를 확인하세요.**

**1) 설치된 JDK 경로 찾기**

```bat
dir /b "C:\Program Files\Eclipse Adoptium"
dir /b "C:\Program Files\Java"
dir /b "C:\Program Files\Microsoft"
```

`jdk-21...` 로 시작하는 폴더 이름이 보이면 그게 실제 경로입니다.
예를 들어 `jdk-21.0.8.9-hotspot` 이 나왔다면 전체 경로는
`C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot` 입니다.

이미 PATH 에 잡혀 있다면 이렇게도 찾을 수 있습니다.

```bat
where java
```

나온 경로에서 `\bin\java.exe` 를 뺀 부분이 `JAVA_HOME` 입니다.

**2) 아무것도 안 나오면 설치**

```bat
winget install EclipseAdoptium.Temurin.21.JDK
```

설치 후 **1) 을 다시 실행**해 실제 폴더 이름을 확인하세요.

**3) JAVA_HOME 설정** — 1) 에서 확인한 **실제 경로**를 넣습니다.

```bat
setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\<1번에서_확인한_폴더명>"
```

> `setx` 는 **새로 여는 터미널부터** 적용됩니다. 지금 창을 닫고 새로 여세요.

**4) 제대로 잡혔는지 확인** — 이 단계를 건너뛰지 마세요.

```bat
echo %JAVA_HOME%
dir "%JAVA_HOME%\bin\java.exe"
```

`java.exe` 가 목록에 보이면 성공입니다.
`파일을 찾을 수 없습니다` 가 나오면 경로가 여전히 틀린 것이니 1) 로 돌아가세요.

**5) 실행**

```bat
cd api
gradlew.bat bootRun --args="--spring.profiles.active=local"
```

**`error: pathspec 'claude/...' did not match any file(s) known to git`**

로컬이 아직 원격 브랜치를 모르는 상태입니다. 먼저 가져오세요.

```bash
git fetch origin
git checkout claude/code-review-26k8iw
```

**CMD 에서 `./gradlew` 가 실행되지 않는다**

`./` 는 macOS/Linux 문법입니다. CMD 에서는 `gradlew.bat`,
PowerShell 에서는 `.\gradlew.bat` 을 쓰세요.
`--args` 값도 CMD 에서는 작은따옴표가 아니라 큰따옴표여야 합니다.


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

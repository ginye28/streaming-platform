# CLAUDE.md

이 저장소에서 작업하는 Claude 에게. 새 세션이면 **먼저 [docs/08-handoff.md](docs/08-handoff.md) 를 읽는다.**
무엇이 끝났고, 무엇이 남았고(소리 확인 · 토스 결제 운영 재확인 · 과제 제출 등), 어디서 막혔는지가 거기 있다.

## 프로젝트

유튜브형·라이브 중심 스트리밍 플랫폼(SP). 개인 프로젝트이며 과제로 제출한다.
- `api/` Spring Boot 4 · Java 21 · JPA · Flyway(MySQL 문법, 운영 DB 는 TiDB)
- `web/` React 19 · Vite · hls.js
- `streaming/` nginx-rtmp (OBS 방송용), `scripts/` 내 PC 에서 라이브 켜기(PowerShell)
- `docs/` 요구사항 · ERD · API · 아키텍처 · 컨벤션 · 배포 · 겪은 문제 · 인수인계
- 운영: 웹 Vercel(`sp-web.vercel.app`) · API Render 무료 Docker(`sp-api-5mal.onrender.com`) · DB TiDB Cloud. `main` 에 병합하면 자동 배포된다(`api/` 가 바뀔 때만 Render 가 재배포).

## 작업 규칙

- **사용자는 한국어로 말한다. 한국어로 답한다.** 코드 주석·커밋 메시지도 한국어(기존 스타일에 맞춘다).
- 사용자는 Windows 를 쓴다. 셸은 PowerShell/Git Bash. 긴 heredoc 은 잘릴 수 있으니 파일로 쓴다.
- **커밋·푸시·PR·병합은 사용자가 요청했을 때만.** 병합은 CI 7개 통과를 확인한 뒤. `main` 에 직접 푸시하지 않는다.
- 비밀 값(토큰, API 키, 비밀번호, 스트림 키)은 저장소·문서·채팅에 적지 않는다. 입력이 필요하면 사용자가 직접 한다.
  로그인·계정 생성·결제·CAPTCHA 도 AI 가 대신 하지 않는다.
- 변경하면 테스트를 돌린다.
  - 백엔드: `cd api; .\gradlew.bat test` (JDK 21 필요. 없으면 Docker `eclipse-temurin:21-jdk` 로)
  - 웹: `cd web; npm run lint; npm run build`
  - 스키마를 바꾸면 새 Flyway 파일을 만든다(기존 파일 수정 금지). 운영 DB(TiDB)에서 돌 수 있는 문법인지 확인한다.
- 문서도 같이 고친다(`docs/03-api-spec.md`, `docs/06-deployment.md`, README 구현 상태).

## 알려진 함정

- TiDB 는 Hibernate 의 `for update of <alias>` 를 못 알아듣는다 → 행 잠금은 네이티브 `select … for update`.
- Jackson 3: record 의 primitive `boolean` 이 요청에서 빠지면 400 → `Boolean` 을 쓴다.
- 새 enum 은 DB ENUM 이 아니라 varchar(`@JdbcTypeCode(SqlTypes.VARCHAR)`).
- `scripts/*.ps1` 은 UTF-8 **BOM** 이어야 한다(Windows PowerShell 5.1 이 BOM 없는 한글을 잘못 읽는다). BOM 을 지우지 않는다.
- Render 무료 서버: 15분 무활동이면 잠들고 깨는 데 오래 걸린다. 메모리 512MB, 자바 힙 300MB. 브라우저 방송은 동시 1개로 막아 두었다.
- 브라우저 방송(`/ingest`)은 H.264 로 녹화되는 Chrome·Edge 만 된다. 서버는 영상을 다시 인코딩하지 않는다.
- 토스 결제는 **"API 개별 연동 키"**(`test_ck_` / `test_sk_`)만 된다. 결제위젯 키(`gck`/`gsk`)는 거절된다. 테스트 키라 실제 청구는 없다.
- Windows Git Bash 는 `/tmp` 같은 경로를 바꿔 버린다 → Docker 명령 앞에 `export MSYS_NO_PATHCONV=1`.

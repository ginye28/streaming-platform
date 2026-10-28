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

## 제품 원칙과 제약

사용자·포지셔닝·원칙의 원본은 [PRODUCT.md](PRODUCT.md) 다. UI 를 만들거나 고칠 때 먼저 읽는다. 시각 규칙은 DESIGN.md 가 생기면 거기를 따른다.

- 사용자는 시청자(일반 소비자), 스트리머, 관리자다. 한국어 UI 가 기본이다.
- **보는 데까지의 길을 줄인다.** 라이브가 중심이고 지금 방송 중인 것이 먼저 보인다.
- **모바일 웹도 일급이다.** 데스크톱과 모바일 양쪽에서 프리미엄이어야 하고, 375px 폭에서 가로 넘침이 없어야 한다.
- **한계를 숨기지 않고 안내한다.** 서버가 잠들어 느릴 때, 브라우저 방송에 소리가 안 들어갈 때, 지원하지 않는 브라우저일 때 화면이 이유와 해결법을 알려 준다.
- **콘텐츠가 주인공이다.** 빈 상태도 다음 행동을 안내한다.
- **속도와 매끄러운 움직임이 완성도의 일부다.**
- **돈과 신뢰 관련 화면은 정직하다.** 결제·후원·환불은 결과와 영수증을 분명히 보여 준다.
- 시각 체계는 홈 위쪽의 어두운 시네마 슬라이더 + 아래쪽 YouTube 구조의 카드 격자, 밤·낮 같은 그린(#10b981) 포인트·퍼플(#a78bfa) 보조·무광 면·상단바만(사이드바 없음)이다. 흰 바탕 위 그린 글자는 `--accent-ink`. CSS 는 `base.css`(토큰) · `shell.css`(상단바·공통 부품) · `home.css`(카드·구역) · `cinema.css`(홈의 시네마 무대) · `skin.css`(다른 화면의 모양) · `watch.css`(시청 화면 아래쪽) · `forms.css`(올리기·방송하기·내 계정·로그인) · `app.css`(화면별 규칙)로 나뉜다. 아직 새 체계로 옮기지 않은 화면은 `app.css` 의 옛 규칙이 남아 있다.
- 스택은 React + Vite + 평문 CSS 로 정해져 있다. Next.js 로 옮기지 않고 Tailwind 를 들이지 않는다.
- 운영 사이트는 콘텐츠가 비어 있다. 방송·영상·시청자 수·후기를 지어내지 않고, 데모 콘텐츠는 데모임을 드러낸다.
- 밝은 화면이 기본이고 나이트 모드는 사용자가 켤 때만 쓴다(OS 설정을 따르지 않는다).

## 작업 규칙

- **사용자는 한국어로 말한다. 한국어로 답한다.** 코드 주석·커밋 메시지도 한국어(기존 스타일에 맞춘다).
- 사용자는 Windows 를 쓴다. 셸은 PowerShell/Git Bash. 긴 heredoc 은 잘릴 수 있으니 파일로 쓴다.
- **작업이 끝나면 사용자가 따로 말하지 않아도 커밋 → 브랜치 푸시 → PR → CI 7개 통과 확인 → 병합 → 배포 확인까지 한다.**
  (2026-10-12 사용자 지시: "항상 다 끝나면 알아서 병합까지 해줘".) `main` 에 직접 푸시하지 않는다. CI 가 실패하면 병합하지 않고 고친다.
  - 병합하면 `api/` 변경은 Render 가 약 4분 재배포하며 그동안 서비스가 끊긴다. 방송 중이면(`GET /api/lives`) 끝나길 기다리거나 사용자에게 알린다.
  - DB 마이그레이션(Flyway)이 들어 있으면 PR 설명에 적는다.
  - 되돌리기 어려운 외부 작업(과제 최종 제출, 결제, 계정 삭제 등)은 이 지시에 포함되지 않는다. 그건 따로 확인받는다.
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
- **보안 규칙은 [docs/09-security.md](docs/09-security.md).** 사용자가 적은 주소를 `<a href>` 로 쓸 때는 서버 `@HttpUrl` + 화면 `safeHttpUrl` 을 거친다(`javascript:` 로 스크립트가 실행된다). 새 STOMP 경로를 만들면 `StompAuthChannelInterceptor.ALLOWED_DESTINATION` 에도 넣는다(안 넣으면 구독이 거절된다).
- 요청 횟수 제한(`app.security.rate-limit`)은 테스트에서 꺼져 있다(같은 IP·계정으로 가입을 수없이 한다). 제한을 확인하는 테스트(`RateLimitIntegrationTest`)는 `@TestPropertySource` 로 켜고, IP 는 `remoteAddr(...)` 로 테스트마다 다르게 준다. 한도는 서버 메모리라 테스트끼리 공유된다.
- 글을 받는 새 DTO 칸에는 `@SafeText`·`@Size` 를, 업로드 경로(`/uploads/…`)나 http(s) 주소를 받는 칸에는 `@SafeUrl` 을, 외부 링크만 받는 칸에는 `@HttpUrl` 을 붙인다(`common/validation`). 안 붙이면 태그·`javascript:` 가 그대로 저장된다.
- 화면 CSP(`web/vercel.json`, `web/security-headers.conf`)에 없는 외부 스크립트·서비스를 붙이면 브라우저가 막는다 → 두 파일에 주소를 더한다.
- 화면 효과(blur·무한 애니메이션)를 더할 때는 그 효과가 있는 CSS 파일(`home.css` 끝, `cinema.css` 끝, `shell.css`)의 `:root[data-perf='lite']` 규칙에도 끄는 규칙을 넣는다([docs/10-performance.md](docs/10-performance.md)).
- 영상 WebSocket(`/ingest`·`/watch`)은 압축(permessage-deflate)을 협상하지 않는다(`WebSocketNoCompressionFilter`). 되돌리면 0.1 CPU 서버가 시청자 13명에서 막힌다. 새 영상 경로를 만들면 같은 필터 경로에 넣는다.
- 저지연 중계에서 시청자별 전송은 일반 스레드(가상 스레드 아님)이고, 느린 시청자는 큐 상한(약 10초)에서 끊는다. 구독자 목록은 순회 중 수정되므로 `CopyOnWriteArrayList` 다([docs/10-performance.md](docs/10-performance.md)).
- Windows Git Bash 는 `/tmp` 같은 경로를 바꿔 버린다 → Docker 명령 앞에 `export MSYS_NO_PATHCONV=1`.

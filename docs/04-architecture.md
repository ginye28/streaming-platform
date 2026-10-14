# 아키텍처

## 무엇으로 만들었나

### 백엔드 (`api/`)

| | |
|---|---|
| Java 21 · Spring Boot 4.0.7 | |
| Spring Security 7 | JWT 필터를 끼워서 씀 |
| Spring Data JPA · Hibernate 7.2 | |
| WebSocket (STOMP) | 라이브 채팅. 브로커는 스프링 내장(SimpleBroker) |
| MySQL 8 | 기본. H2 파일 모드로도 돌아감 (`local` 프로필) |
| Gradle 9.5 | |

### 프론트 (`web/`)

| | |
|---|---|
| React 19 · Vite 8 | |
| hls.js | 라이브 재생 |
| `@stomp/stompjs` | 채팅 |
| **`fetch`** | HTTP 클라이언트를 따로 안 씁니다 (`src/api.js` 한 곳) |
| **라우터 라이브러리 없음** | 주소의 `?view=` 로 화면을 고릅니다 (`src/router.js`) |

### 스트리밍 (`streaming/`)

nginx-rtmp. RTMP 를 받아 HLS 로 바꿔 내보냅니다.

---

## 흐름

```
OBS ──RTMP:1935──> nginx-rtmp ──on_publish──> Spring (스트림 키 확인)
                       │                          │
                       │ <──302 공개 이름──────────┘
                       ▼
                 1936 안쪽 통로 (HLS 생성)
                       │
브라우저 <──HLS:8081───┘

브라우저 ──HTTP:8080──> Spring ──> MySQL
        └─WebSocket:8080/ws─> Spring (채팅 · 시청자 수)
```

브라우저에서 바로 방송할 때는 nginx-rtmp 를 거치지 않습니다.

```
방송자 브라우저 ──WebSocket /ingest──> Spring ──> ffmpeg (다시 인코딩 없이 HLS 로 포장만)
                                          │
시청자 브라우저 <──HLS /live-hls/ ─────────┘
```

- 재생 이름이 `b-` 로 시작하면 이 방식의 방송이고, HLS 를 스트리밍 서버가 아니라 API 가 직접 내려 줍니다.
- 다시보기는 남기지 않습니다. 동시 방송은 `INGEST_MAX_SESSIONS`(기본 1)로 막습니다.
- 자세한 설명은 [배포 문서](06-deployment.md#브라우저로-방송하기-내-pcobs-없이)와 [API 명세](03-api-spec.md)의 "브라우저 방송".

- 송출이 들어오면 nginx 가 Spring 에 물어봅니다. 스트림 키가 맞으면 Spring 이
  **공개 이름으로 302** 를 돌려주고, nginx 는 그 이름으로 다시 송출합니다.
  그래서 재생 URL 에 스트림 키가 안 드러납니다.
- 송출 입구(1935)와 HLS 를 만드는 통로(1936)를 **일부러 나눠** 뒀습니다.
  한 곳에서 다 하면 재생목록이 스트림 키 이름으로도 하나 더 생깁니다.
  자세한 이유는 [03-api-spec.md](03-api-spec.md) 참고.

---

## 패키지 구조

기능별로 자릅니다. 계층별(`controller/`, `service/` 를 최상위에 두는 방식)이 아닙니다.

```
com.sp.api
├── auth          로그인 · 토큰
├── user          계정 · 스트림 키
├── stream        올린 영상(VOD)
├── comment       댓글 · 답글
├── like
├── category
├── channel       남의 채널 보기
├── subscribe
├── block
├── live          라이브 방송 · 방송 설정 · RTMP 콜백
├── chat          실시간 채팅 (WebSocket 설정도 여기 안에)
├── notification
├── report
├── admin
├── common        응답 봉투 · 예외 · JWT · 파일 업로드 · 공통 web
└── config        Security · Swagger · Web(정적 파일) · Clock
```

기능 하나는 `controller / service / repository / entity / dto` 를 자기 안에 갖습니다.
`config` 에는 **앱 전체**에 걸리는 것만 둡니다. WebSocket 설정처럼 한 기능에만
쓰이는 것은 그 기능 폴더 안에 둡니다(`chat/config/WebSocketConfig.java`).

---

## 서버 메모리에 있는 것

Redis 를 안 쓰기 때문에, 아래는 **서버 한 대 안에서만** 맞습니다.

| 무엇 | 어디 |
|---|---|
| 동시 시청자 수 | 채팅방 구독 수를 세서 메모리에 |
| 조회수 30분 중복 판정 | 메모리 캐시 |
| 채팅 브로커 | 스프링 내장 SimpleBroker |
| 요청 속도 제한 버킷 · 비정상 접근 기록 | `SlidingWindowRateLimiter`(키가 쌓이면 지난 기록을 쓸어 냄) · `SecurityEventRecorder`(최근 200건) |
| 채팅 도배 방지 · 슬로우 모드 | `ChatFloodGuard` · `SlowModeLimiter` |
| 브라우저 방송 연결 · 동시 방송 수 | `IngestWebSocketHandler` 의 메모리 맵 (조각 파일은 서버 임시 디스크) |

서버를 여러 대로 늘리려면 이것들을 Redis 등으로 옮겨야 합니다. (브라우저 방송은 조각 파일이 그 서버 디스크에만 있어서 따로 손봐야 합니다.)

---

## 실시간 통신

| 무엇 | 어떻게 |
|---|---|
| 영상 | HLS. nginx-rtmp(또는 브라우저 방송의 ffmpeg)가 영상을 2초 안팎의 `.ts` 조각으로 자르고, 시청자의 hls.js 가 재생목록(`.m3u8`)을 받아 조각을 이어서 받는다. JSON 이 아니라 HTTP 로 조각을 내려주는 방식이다 |
| 채팅 | STOMP over WebSocket(`/ws`). `/topic/lives/{id}` 구독, `/app/lives/{id}/chat` 으로 전송 |
| 시청자 수 | 채팅방 구독/해제를 세서 `/topic/lives/{id}/viewers` 로 밀어 준다. 홈·라이브 목록의 카드도 이 토픽을 구독해 목록을 새로 받지 않아도 숫자가 바뀐다(`web/src/liveViewers.js`, WebSocket 하나를 모든 카드가 나눠 씀, 탭이 가려지면 닫음) |
| 카드 호버 | 방송 카드에 마우스를 올리거나 포커스하면 방송 시간·지금 시청·최고 시청이 펼쳐진다 |

WebRTC 는 쓰지 않는다. 시청 지연이 약 10초 생기는 대신, 일반 HTTP·CDN·터널로 그대로 나가서 서버·방화벽 설정이 단순하다. 지연이 중요해지면 LL-HLS 나 WebRTC(SFU) 가 다음 후보다.

---

## 보안 계층

바깥에서 안쪽 순서로 요청이 지나는 곳이다.

1. **브라우저 헤더** — 웹: `vercel.json`/`security-headers.conf` 의 CSP(`script-src 'self'` + 토스), `nosniff`, `frame-ancestors 'none'`. API: `SecurityHeadersFilter`.
2. **속도 제한** — 인증 경로는 `AuthRateLimitFilter`(IP 별)와 `AuthService`(계정별 실패 잠금), 그 밖의 `/api`·WebSocket 은 `RateLimitFilter`(IP 별, 변경·조회·업로드·연결 종류별). 채팅 도배는 `ChatFloodGuard`. 모두 `SlidingWindowRateLimiter`.
3. **인증** — JWT(액세스 토큰) + 회전하는 리프레시 토큰. WebSocket 은 STOMP CONNECT 의 토큰으로 식별하고 구독 시 시청 권한을 다시 본다.
4. **입력 필터** — `@SafeText`(태그·제어 문자 거절) · `@SafeUrl`(`javascript:` 등 거절) · `@Size`. 화면은 글을 항상 텍스트로만 그리고(React), 링크는 http(s) 만 만든다.
5. **기록** — 막힌 요청·로그인 실패·거절된 입력을 `SecurityEventRecorder` 가 모아 관리자 화면(보안 탭)과 WARN 로그에 남긴다.

한계: IP 는 Cloudflare 가 채우는 `CF-Connecting-IP` 를 쓴다(`ClientIp`). Cloudflare 를 거치지 않는 접속에서는 접속 주소를 쓰고, 클라이언트가 꾸밀 수 있는 `X-Forwarded-For` 는 믿지 않는다. 자세한 위협·한계는 [09-security.md](09-security.md).

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

Redis 를 안 쓰기 때문에, 아래 셋은 **서버 한 대 안에서만** 맞습니다.

| 무엇 | 어디 |
|---|---|
| 동시 시청자 수 | 채팅방 구독 수를 세서 메모리에 |
| 조회수 30분 중복 판정 | 메모리 캐시 |
| 채팅 브로커 | 스프링 내장 SimpleBroker |

서버를 여러 대로 늘리려면 이 셋을 Redis 로 옮겨야 합니다.

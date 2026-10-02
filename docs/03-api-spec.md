# API 명세

Base URL: `http://localhost:8080`

---

## 공통 응답 형식

모든 응답은 아래 봉투로 감싸진다. `data`/`message` 는 값이 없으면 생략된다.

성공
```json
{ "success": true, "data": { } }
```

실패
```json
{ "success": false, "message": "영상을 찾을 수 없습니다." }
```

### 상태 코드

| 코드 | 의미 |
|---|---|
| 200 | 성공 |
| 201 | 생성됨 (회원가입, 영상 등록, 댓글 등록) |
| 400 | 검증 실패 / 잘못된 요청 |
| 401 | 미인증 (토큰 없음·만료·위조) |
| 403 | 권한 없음 (남의 리소스 수정/삭제) |
| 404 | 대상 없음 |
| 409 | 중복 (이메일·닉네임) |
| 413 | 업로드 용량 초과 |

### 페이지 응답 형식

목록 API 는 `page`, `size`, `sort` 쿼리 파라미터를 받는다. (기본 `size=20`, `sort=createdAt,desc`)

```json
{
  "success": true,
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "last": true
  }
}
```

### 인증

로그인 후 받은 토큰을 헤더에 담는다.

```
Authorization: Bearer <accessToken>
```

---

## 인증

### 회원가입 — `POST /api/auth/signup` (공개)

```json
{ "email": "test@test.com", "password": "password123", "nickname": "홍길동" }
```
- `password`: 8~20자, `nickname`: 2~20자
- 201 / 409(중복)

### 로그인 — `POST /api/auth/login` (공개)

```json
{ "email": "test@test.com", "password": "password123" }
```
응답: `{ "success": true, "data": { "accessToken": "eyJ...", "refreshToken": "..." } }`
- 401(이메일 또는 비밀번호 불일치)

### 토큰 재발급 — `POST /api/auth/refresh` (공개)

```json
{ "refreshToken": "..." }
```
응답: 로그인과 동일한 형태로 새 `accessToken`/`refreshToken` 쌍을 돌려준다.
- 전달한 리프레시 토큰은 이 호출로 즉시 폐기된다(1회용, 회전 방식). 재사용하면 401.
- 401(유효하지 않거나 만료된 리프레시 토큰)

### 로그아웃 — `POST /api/auth/logout` (공개)

```json
{ "refreshToken": "..." }
```
- 전달한 리프레시 토큰을 무효화한다. 이미 없는 토큰이어도 200(멱등).
- 액세스 토큰 자체는 무상태(stateless) JWT라 즉시 무효화되지 않고 남은 만료 시간까지는 유효하다.
  액세스 토큰 만료 시간을 짧게 잡는 것으로 이 창을 줄인다.

---

## 내 계정 (`/api/users`)

모두 인증이 필요합니다. 공개 채널 조회는 `/api/channels` 에 있습니다.

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/users/me` | 내 정보 (`id`, `email`, `nickname`, `profileImage`, `role`) |
| PATCH | `/api/users/me` | 닉네임·프로필 이미지 수정 (닉네임 중복 시 409) |
| PUT | `/api/users/me/password` | 비밀번호 변경 |
| GET | `/api/users/me/subscriptions` | 내가 구독 중인 채널 목록 (페이지) |
| GET | `/api/users/stream-key` | OBS 송출용 스트림 키 조회 |
| POST | `/api/users/stream-key/regenerate` | 스트림 키 재발급 |
| POST | `/api/users/{userId}/block` | 차단 토글. 응답: `{ "blocked": true }` |
| GET | `/api/users/me/blocks` | 내가 차단한 사용자 목록 (페이지) |

프로필 수정
```json
{ "nickname": "새닉네임", "profileImage": "/uploads/p.png" }
```

비밀번호 변경 — 현재 비밀번호가 틀리면 401, 기존과 같은 값이면 400
```json
{ "currentPassword": "password123", "newPassword": "newpassword456" }
```

차단 — 자기 자신을 차단하면 400. 차단한 채널의 영상·라이브는
`GET /api/streams`, `GET /api/lives` 목록(로그인 상태로 조회 시)에서 빠집니다.
채널 페이지를 직접 방문하거나 구독 피드를 보는 것까지 막지는 않습니다.

---

## 채널 (`/api/channels`)

| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| GET | `/api/channels/{channelId}` | 공개 | 채널 정보 |
| GET | `/api/channels/{channelId}/streams` | 공개 | 채널의 영상 목록 (페이지) |
| POST | `/api/channels/{channelId}/subscribe` | 필요 | 구독 토글 |
| PUT | `/api/channels/{channelId}/subscription` | 필요 | 내 구독 설정 변경 — 등급 · 오시마크 표시 |

채널 정보 응답
```json
{
  "id": 1,
  "nickname": "채널명",
  "profileImage": null,
  "subscriberCount": 12,
  "streamCount": 4,
  "subscribedByMe": true,
  "myTier": "BASIC",
  "myMarkVisible": true,
  "myPaidUntil": null
}
```

`subscribedByMe` 는 비로그인으로 조회하면 항상 `false` 입니다.
`myTier`(`BASIC` · `PAID`)와 `myMarkVisible` 은 내 구독 설정입니다.
구독 중이 아니면 `myTier` 는 `null`, `myMarkVisible` 은 `false` 입니다.
`myPaidUntil` 은 유료 구독이 끝나는 때이고, **유료 기간이 지났으면 `myTier` 는 `BASIC`** 으로 내려갑니다.
`GET /api/users/me/subscriptions` 의 각 채널에도 같은 값이 들어 있습니다.

구독 토글 응답: `{ "subscribed": true, "subscriberCount": 13 }`
자기 자신을 구독하면 400.

### 내 구독 설정 — `PUT /api/channels/{channelId}/subscription`

```json
{ "tier": "PAID", "markVisible": false }
```

**보낸 값만 바꿉니다.** 둘 다 없으면 400, `tier` 에 `BASIC`·`PAID` 가 아닌 값을 주면 400,
**구독 중이 아닌 채널이면 400** 입니다. 응답은 `{ "subscribed": true, "tier": "PAID", "markVisible": false }`.

| 값 | 뜻 |
|---|---|
| `tier` | `BASIC`(일반 구독) · `PAID`(유료 구독). 유료 구독자는 채널의 유료 오시마크를 단다. |
| `markVisible` | 이 채널의 오시마크를 내 이름 옆에 달지. 기본은 `true`. 구독했다고 강제로 달지 않는다. |

- 구독을 해제하면 등급과 표시 설정도 함께 초기화됩니다. 다시 구독하면 `BASIC` · `markVisible: true` 입니다.
- **결제가 켜져 있으면(토스 키가 있으면) `tier: "PAID"` 는 이 API 로 못 바꿉니다(400).**
  유료 구독은 [결제](#유료-구독-결제--토스페이먼츠)로만 시작됩니다. `BASIC` 으로 내리는 것은 언제든 되며,
  남은 유료 기간은 없어지고 환불되지 않습니다.
- 결제가 꺼져 있을 때(키가 없을 때)만 결제 없이 `PAID` 로 바뀌는 자리표시로 동작합니다(개발·데모용).

---

## 영상(Stream)

| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| POST | `/api/streams` | 필요 | 등록 (201) |
| GET | `/api/streams?categoryId=&sortBy=` | 공개 | 목록 (페이지). `categoryId` 로 좁히고 `sortBy` 로 정렬한다. 로그인 상태면 내가 차단한 채널의 영상은 빠진다. |
| GET | `/api/streams/search?keyword=` | 공개 | 제목·설명 검색 (페이지) |
| GET | `/api/streams/subscribed` | **필요** | 구독 중인 채널들의 영상 피드 (페이지) |
| GET | `/api/streams/{id}` | 공개 | 상세 (조회수 +1, 같은 시청자는 30분에 한 번만) |
| PUT | `/api/streams/{id}` | 필요 | 수정 (본인만, 아니면 403) |
| DELETE | `/api/streams/{id}` | 필요 | 삭제 (본인만, 아니면 403) |

#### 정렬 (`sortBy`)

| 값 | 뜻 |
|---|---|
| `LATEST` (기본) | 최신순. 올린 시각이 같으면 id 로 한 번 더 끊는다. |
| `POPULAR` | 조회수순. 조회수가 같으면 최신 영상이 앞에 온다. |

엔티티 필드 이름을 그대로 받지 않고 위 두 값만 허용합니다.
다른 값을 주면 **400** 입니다. (`?sortBy=NO_SUCH` → 400)

파라미터 이름이 `sort` 가 아니라 `sortBy` 인 이유는, `sort` 가 Spring Data 의
페이지 정렬 파라미터와 겹치기 때문입니다.

#### 조회수 집계

`GET /api/streams/{id}` 는 조회수를 올리지만, **같은 시청자는 30분에 한 번만** 셉니다.
새로고침을 반복해도 숫자가 오르지 않습니다.

- 로그인 상태면 **계정** 기준
- 비로그인이면 **접속 IP** 기준 (nginx 뒤라면 `X-Forwarded-For` 의 첫 값)

기록은 서버 메모리에 있습니다. 서버를 다시 켜면 창이 초기화되고, 서버가 여러 대면
대마다 따로 셉니다. 동시 시청자 수와 같은 한계이고, Redis 를 들이면 함께 옮겨 갈 자리입니다.

등록/수정 요청
```json
{
  "title": "제목",
  "description": "설명",
  "thumbnailUrl": "/uploads/xxx.png",
  "videoUrl": "/uploads/xxx.mp4",
  "categoryId": 1
}
```
`title`, `videoUrl` 필수. `categoryId` 는 생략 가능(미분류). 존재하지 않는 id 면 404.

영상 응답
```json
{
  "id": 1,
  "title": "제목",
  "description": "설명",
  "thumbnailUrl": "/uploads/t.png",
  "videoUrl": "/uploads/v.mp4",
  "viewCount": 42,
  "likeCount": 7,
  "commentCount": 3,
  "likedByMe": false,
  "userId": 5,
  "nickname": "채널명",
  "categoryId": 1,
  "categoryName": "저스트채팅",
  "createdAt": "2026-08-21T09:00:00"
}
```

- `likedByMe` 는 비로그인으로 조회하면 항상 `false` 입니다. 토큰을 함께 보내면 실제 값이 옵니다.
- `userId` 로 `/api/channels/{userId}` 채널 페이지에 연결할 수 있습니다.
- `categoryId`/`categoryName` 은 미분류면 둘 다 `null`.
- 영상을 삭제하면 달려 있던 댓글(답글 포함)·좋아요도 함께 정리됩니다.

---

## 카테고리 (`/api/categories`)

| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| GET | `/api/categories` | 공개 | 목록 (이름순) |
| POST | `/api/categories` | **관리자만** | 생성 (201) |

```json
{ "name": "저스트채팅" }
```
- `name` 최대 30자, 중복이면 409.
- 관리자가 아닌 사용자가 생성을 시도하면 403.

---

## 댓글

| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| POST | `/api/streams/{streamId}/comments` | 필요 | 등록 (201). `parentId` 를 주면 답글 |
| GET | `/api/streams/{streamId}/comments` | 공개 | 목록 (페이지). 답글은 원 댓글에 딸려 온다 |
| PUT | `/api/streams/{streamId}/comments/{commentId}` | 필요 | 수정 (본인만) |
| DELETE | `/api/streams/{streamId}/comments/{commentId}` | 필요 | 삭제 (본인만). 답글도 함께 사라진다 |

`{ "content": "댓글 내용" }` — 댓글이 해당 `streamId` 에 속하지 않으면 404.

#### 답글 (대댓글)

등록할 때 원 댓글 id 를 함께 보냅니다.

```json
{ "content": "답글 내용", "parentId": 12 }
```

깊이는 **두 단계까지**입니다. 답글에 다시 답글을 달면 **400**,
다른 영상의 댓글을 `parentId` 로 주면 **404** 입니다.

목록은 **원 댓글만 페이지로 나누고**, 그 페이지에 실린 댓글의 답글은
`replies` 에 통째로 담아 함께 보냅니다. 답글은 쓴 순서(오래된 것부터)입니다.

```json
{
  "content": [
    {
      "id": 12, "content": "원 댓글", "nickname": "긴예",
      "oshiMarkUrl": "/uploads/mark.png", "oshiTier": "BASIC",
      "parentId": null,
      "replies": [
        { "id": 13, "content": "답글", "nickname": "도라",
          "oshiMarkUrl": null, "oshiTier": null, "parentId": 12, "replies": [] }
      ]
    }
  ],
  "page": 0, "size": 20, "totalElements": 1, "totalPages": 1, "last": true
}
```

`oshiMarkUrl` · `oshiTier` 는 [오시마크](#오시마크) 규칙으로 붙습니다. 이 영상의 채널을 구독한 사람만 받고,
아니면 둘 다 `null` 입니다. 댓글 등록·수정 응답에도 같이 옵니다.

`totalElements` 는 원 댓글 수입니다. 영상의 `commentCount` 는 답글까지 센 수라 서로 다를 수 있습니다.

원 댓글을 지우면 거기 달린 답글도 함께 지워집니다.

새 댓글은 영상 주인에게 `STREAM_COMMENT`, 답글은 원 댓글 작성자에게 `COMMENT_REPLY` 알림이 갑니다.
자세한 건 [알림](#알림-apinotifications) 참고.

---

## 좋아요

`POST /api/streams/{streamId}/like` (인증 필요) — 토글

```json
{ "success": true, "data": { "liked": true, "likesCount": 12 } }
```

---

## 파일 업로드

`POST /api/files/upload` (인증 필요, `multipart/form-data`, 파트 이름 `file`)

- 허용 확장자: `mp4`, `mov`, `webm`, `m4v`, `jpg`, `jpeg`, `png`, `webp`, `gif`
- 최대 100MB (초과 시 413)
- 응답: `{ "success": true, "data": { "url": "/uploads/{uuid}.mp4", "thumbnailUrl": "/uploads/{uuid}.jpg" } }`
- 저장된 파일은 `GET /uploads/{파일명}` 으로 공개 제공된다.

### 썸네일 자동 생성

올린 파일이 영상(`mp4`·`mov`·`webm`·`m4v`)이면 서버가 ffmpeg 로 한 장면을 뽑아
같은 이름의 `.jpg` 로 저장하고 `thumbnailUrl` 에 담아 준다.
영상이 아니거나 뽑지 못했으면 `thumbnailUrl` 은 없다(`null`).

- 1초 지점을 먼저 뽑는다. 맨 앞은 검은 화면인 경우가 많아서다.
  영상이 1초보다 짧으면 맨 앞으로 물러선다.
- 가로 640 으로 줄여 저장한다.
- **ffmpeg 이 없어도 업로드는 성공한다.** 썸네일만 안 붙을 뿐이다.
  서버 로그에 `썸네일을 만들지 못했습니다` 가 남는다.
- 설정은 `application.yaml` 의 `file.thumbnail` (`enabled`·`ffmpeg-path`·`timeout-seconds`).
- 업로드 응답을 기다리는 동안 같이 처리되므로, 아주 큰 영상이면 업로드가 그만큼 늦게 끝난다.
  `-ss` 로 건너뛰어 훑지 않기 때문에 보통은 1초 안쪽이고, `timeout-seconds` 를 넘기면 포기한다.

화면에서는 영상 파일을 고르면 썸네일 칸이 이 주소로 자동으로 채워진다.
직접 고른 썸네일이 이미 있으면 덮어쓰지 않는다.

---

## 라이브 방송 (`/api/lives`)

업로드 영상(`/api/streams`)과는 **별개의 자원**입니다.
VOD 는 `videoUrl` 로 재생하고, 라이브는 `hlsUrl` 로 재생합니다.

| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| GET | `/api/lives` | 공개 | 지금 방송 중인 목록 (페이지) |
| GET | `/api/lives/{liveId}` | 공개 | 방송 상세 (고정 메시지·내 역할 포함) |
| GET | `/api/lives/{liveId}/chats` | 공개 | 채팅 내역 (페이지, 최신순). 영상을 볼 수 없는 방송이면 403 |
| GET | `/api/lives/{liveId}/chats/replay` | 공개 | **다시보기 채팅** (끝난 방송만, 오래된 순, `afterId` 로 이어받기) |
| GET | `/api/lives/settings` | 필요 | 다음 방송에 쓸 설정 |
| PUT | `/api/lives/settings` | 필요 | 방송 제목·설명·썸네일·공개 대상·채팅 대상·슬로우 모드 저장 |
| GET | `/api/channels/{channelId}/live` | 공개 | 그 채널의 현재 방송 (아니면 404) |
| GET | `/api/channels/{channelId}/live-history` | 공개 | 그 채널의 지난 방송 (페이지) |

방송 응답
```json
{
  "id": 1,
  "channelId": 5,
  "nickname": "채널명",
  "title": "오늘의 방송",
  "description": "설명",
  "thumbnailUrl": null,
  "hlsUrl": "http://localhost:8081/hls/{공개이름}.m3u8",
  "status": "LIVE",
  "viewerCount": 3,
  "peakViewerCount": 12,
  "startedAt": "2026-08-21T09:00:00",
  "endedAt": null,
  "audience": "ALL",
  "chatAudience": "ALL",
  "slowModeSeconds": 0,
  "locked": false,
  "vodUrl": null,
  "pinnedMessage": null,
  "myRole": null,
  "chatLocked": false
}
```

`status` 는 `LIVE` 또는 `ENDED`. 방송이 끝나면 지난 방송 목록으로 넘어갑니다.

| 필드 | 뜻 |
|---|---|
| `audience` | 영상을 볼 수 있는 사람. `ALL`(누구나) · `SUBSCRIBERS`(구독자) · `PAID`(유료 구독자) |
| `locked` | **이 시청자는 영상을 볼 수 없다.** true 면 `hlsUrl`·`vodUrl` 이 비어 있다 |
| `chatAudience` · `chatLocked` | 채팅할 수 있는 사람 · 이 시청자가 채팅할 수 없는지(방송 하나를 조회할 때만 채워진다) |
| `slowModeSeconds` | 슬로우 모드 대기 시간(초). 0 이면 꺼짐 |
| `vodUrl` | 끝난 방송의 다시보기 주소(스트리밍 서버가 녹화하는 경우, 볼 수 있는 시청자에게만) |
| `pinnedMessage` · `myRole` | 채팅창 위에 고정된 메시지 · 이 채널에서의 내 역할(`OWNER`·`MANAGER`). 방송 하나를 조회할 때만 채워진다 |

> OBS 는 방송 제목을 보내주지 않으므로, 송출 전에 `PUT /api/lives/settings` 로
> 제목을 저장해 두면 송출 시작 시 그 값이 쓰입니다. 저장하지 않으면
> "{닉네임} 님의 방송" 이 기본 제목이 됩니다. 가까운 **방송 예약**이 있으면 설정보다 예약이 먼저 쓰입니다.

### 멤버십 전용 방송

방송을 시작할 때의 `audience` 로 영상을 볼 수 있는 사람을 정합니다(`PUT /api/lives/settings` 나 방송 예약에 적어 둔다).
**방송이 시작된 뒤에는 바꿀 수 없습니다.** 재생 주소가 이미 나갔기 때문입니다.

- 영상을 볼 수 없는 시청자에게는 `hlsUrl`·`vodUrl` 이 **내려가지 않습니다**(`locked: true`). 목록(`GET /api/lives`)도 같습니다.
- 재생 이름이 **방송마다 새로 만든 UUID** 라서, 주소를 모르면 볼 수 없습니다. 다만 주소를 받은 구독자가 남에게 전달하면
  볼 수 있습니다 — 이 서버는 서명된 주소(만료되는 토큰)까지는 하지 않습니다. 더 강하게 막으려면 스트리밍 서버(nginx)에서
  `secure_link` 같은 서명 검증을 붙여야 합니다.
- 채널 주인은 구독하지 않아도 언제나 볼 수 있습니다. 유료 구독은 기간이 끝났으면 일반 구독으로 칩니다.
- 영상을 볼 수 없는 방송의 **채팅 내역·다시보기 채팅도 403** 이고, 채팅방 구독(WebSocket SUBSCRIBE)도 거절됩니다.
- `chatAudience` 는 방송 중에도 주인이 `PUT /api/lives/{liveId}/chat-audience` 로 바꿀 수 있습니다.

### 다시보기

스트리밍 서버(`streaming/nginx.conf` 의 `vod` 애플리케이션)가 방송을 녹화하는 경우, 서버 설정 `VOD_ENABLED=true` 이면
방송이 끝날 때 `vodUrl` 이 생깁니다. 주소는 `{VOD_BASE_URL}/{재생 이름}.m3u8` 입니다.

- 녹화는 실시간 HLS 와 따로 6초 조각으로 쌓이고, 방송이 끝나면 재생목록 끝에 `#EXT-X-ENDLIST` 가 붙어 **처음부터 건너뛰며** 볼 수 있습니다.
  (nginx-rtmp 는 끝 표시를 직접 붙이지 않아서 `exec_publish_done` 으로 붙입니다.)
- **서버는 녹화가 실제로 됐는지 확인하지 않습니다.** 녹화를 안 하는 스트리밍 서버에서 `VOD_ENABLED` 를 켜면 없는 주소를 가리킵니다.
- 조각 파일은 지우지 않습니다. 오래된 것을 정리하는 일은 운영자가 합니다(`docs/06-deployment.md`).

`GET /api/lives/{liveId}/chats/replay?afterId=&size=` — 방송이 끝난 뒤에만 됩니다(방송 중이면 400).
지운 메시지는 빠지고, 각 메시지의 `offsetSeconds`(방송 시작 뒤 몇 초째) 로 영상 재생 위치에 맞춥니다.

```json
{ "messages": [ { "id": 10, "nickname": "채팅유저", "content": "안녕하세요", "offsetSeconds": 79 } ],
  "nextAfterId": 10 }
```

`size` 는 기본 200, 최대 500. `nextAfterId` 가 없으면 끝입니다. 화면은 이어서 받으며 재생 위치까지만 보여 줍니다.

### 방송 예약 (`/api/lives/schedules`)

방송 전에 "언제 한다" 를 올려 두면 구독자에게 알림이 가고, 시청자는 **대기실**(카운트다운)에서 기다립니다.

| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| GET | `/api/lives/schedules` | 공개 | 앞으로의 방송 예약, 가까운 순 (페이지). 내가 차단한 채널은 뺀다 |
| GET | `/api/lives/schedules/{id}` | 공개 | 예약 하나 (대기실이 읽는다. 취소·만료된 것도 읽힌다) |
| GET | `/api/lives/schedules/mine` | 필요 | 내 예약 (방송 전인 것) |
| POST | `/api/lives/schedules` | 필요 | 예약 만들기 (201) |
| PUT | `/api/lives/schedules/{id}` | 필요 | 예약 고치기 (방송 전인 것만) |
| DELETE | `/api/lives/schedules/{id}` | 필요 | 예약 취소 |
| GET | `/api/channels/{channelId}/schedules` | 공개 | 그 채널의 앞으로의 예약 |

```json
{ "title": "내일의 방송", "description": "설명", "scheduledAt": "2026-10-05T20:00:00+09:00",
  "audience": "SUBSCRIBERS", "chatAudience": "ALL" }
```

- **`scheduledAt` 은 시간대가 붙은 시각이어야 합니다.** 시간대 없이 받으면(400) 서버가 도는 곳의 시간대(무료 서버는 UTC)로 읽혀서
  한국에서 20시에 한 예약이 9시간 어긋납니다. 응답도 시간대가 붙어 나갑니다. 알림 문구의 시각은 한국 시간입니다.
- 시각은 지금보다 뒤이고 90일 안이어야 하며, 한 채널이 걸어 둘 수 있는 예약은 20개입니다.
- 만들면 구독자에게 `LIVE_SCHEDULED` 알림, **시각이 바뀌면** 다시 알립니다(제목·설명만 고치면 알리지 않습니다). 취소해도 알립니다.
- 시작 **10분 전부터** `LIVE_REMINDER`("곧 시작합니다")를 한 번 보냅니다. 1분마다 도는 작업이라 서버가 잠들면 쉽니다.
- 송출이 시작되면 **방송 시각이 가장 가까운 예약**(시작 90분 전 ~ 6시간 후 범위) 하나가 그 방송에 이어집니다. 예약에 적은 제목·설명·
  공개 대상·채팅 대상이 방송에 쓰이고, 예약은 `STARTED` + `liveId` 가 됩니다. 대기실이 이걸 보고 방송 화면으로 넘어갑니다.
- 시각이 6시간 넘게 지나도 방송하지 않은 예약은 `EXPIRED` 로 내려갑니다.

---

## 채팅 (WebSocket)

STOMP over WebSocket 을 씁니다.

| 항목 | 값 |
|---|---|
| 접속 주소 | `ws://localhost:8080/ws` |
| 인증 | CONNECT 프레임에 `Authorization: Bearer {토큰}` |
| 보내기 | `/app/lives/{liveId}/chat` — `{ "content": "안녕하세요" }` |
| 받기 | `/topic/lives/{liveId}` 구독 |
| 방 안의 변화 | `/topic/lives/{liveId}/events` 구독 — 삭제·고정·슬로우 모드 |
| 시청자 수 | `/topic/lives/{liveId}/viewers` 구독 |
| 못 보낸 이유 | `/user/queue/chat-errors` 구독 — **보낸 사람에게만** 온다 (`{ "message": "슬로우 모드예요. 4초 뒤에…" }`) |

- 토큰 없이도 **연결과 구독은 가능**합니다(읽기 전용). 채팅 전송은 무시됩니다.
  단 영상을 볼 수 없는 방송(멤버십 전용)의 채팅방은 구독 자체가 거절됩니다(STOMP ERROR).
- 종료된 방송에는 채팅을 보낼 수 없습니다.
- 검사 순서: **제한(일시 정지·강퇴) → 채팅 대상 → 금칙어 → 슬로우 모드.** 슬로우 모드를 맨 뒤에 두는 것은 다른 이유로 거절된 글이
  대기 시간을 쓰지 않게 하려는 것입니다. 채널 주인과 매니저는 전부 건너뜁니다.
- 채팅방 구독 수가 곧 시청자 수로 집계됩니다. 서버를 여러 대로 늘리면
  인스턴스별로 따로 세어지므로 Redis 등으로 옮겨야 합니다.
- 접속 직후 채팅창은 `GET /api/lives/{liveId}/chats` 로 채웁니다.

받는 메시지
```json
{
  "id": 10, "liveId": 1, "userId": 5,
  "nickname": "채팅유저", "content": "안녕하세요",
  "oshiMarkUrl": null, "oshiTier": null,
  "role": "MANAGER",
  "donationAmount": null, "donationTier": null, "donationPinRemainingSeconds": null,
  "deleted": false, "offsetSeconds": null,
  "createdAt": "2026-08-21T09:01:00"
}
```

`role` 은 이 채널에서의 역할(`OWNER`·`MANAGER`, 일반 시청자는 null). 지운 메시지는 `content` 가 null 이고 `deleted: true` 입니다.
후원 메시지는 `donationAmount`(원)·`donationTier`(1~5)가 붙고, `donationPinRemainingSeconds` 는 후원 띠가 채팅창 위에 더 남는 시간입니다.

방 안의 변화(`/events`)
```json
{ "type": "DELETE", "messageId": 10 }
{ "type": "PURGE",  "userId": 7 }
{ "type": "PIN",    "message": { "...": "고정된 메시지. null 이면 고정을 푼다" } }
{ "type": "SETTINGS", "slowModeSeconds": 10, "chatAudience": "SUBSCRIBERS" }
```

바깥으로 나가는 메시지는 **DB 커밋이 끝난 뒤에** 나갑니다(롤백됐는데 화면에는 이미 나간 일이 없도록).

### 채팅 운영

방송 안에서(채널 주인·매니저, 로그인 필요)

| 메서드 | 경로 | 설명 |
|---|---|---|
| DELETE | `/api/lives/{liveId}/chats/{messageId}` | 메시지 삭제. 쓴 사람 본인도 자기 것은 지울 수 있다 |
| PUT | `/api/lives/{liveId}/pin` | `{ "messageId": 10 }` 채팅창 위에 고정. 한 번에 하나, 새로 고정하면 이전 것은 풀린다 |
| DELETE | `/api/lives/{liveId}/pin` | 고정 해제 |
| PUT | `/api/lives/{liveId}/slow-mode` | `{ "seconds": 10 }` — `0`·`3`·`5`·`10`·`30`·`60` 중에서만 |
| PUT | `/api/lives/{liveId}/chat-audience` | `{ "chatAudience": "SUBSCRIBERS" }` — **채널 주인만** |
| POST | `/api/lives/{liveId}/restrictions` | `{ "userId": 7, "minutes": 10, "reason": "도배", "purge": false }` — `minutes` 가 없으면 **강퇴**, `purge` 면 그 방송에서 쓴 메시지도 지운다 |
| DELETE | `/api/lives/{liveId}/restrictions/{userId}` | 제한 풀기 |

- **제한은 방송이 아니라 채널에 걸려서** 다음 방송에도 이어집니다. 일시 정지는 최대 7일, 그보다 길게는 강퇴를 씁니다.
- 주인은 제한할 수 없고, 매니저는 **주인만** 제한할 수 있습니다(매니저끼리·주인 제한은 403).

내 채널의 채팅 도구(`/api/users/me`, 본인만)

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET · POST · DELETE | `/api/users/me/moderators`, `/{userId}` | 매니저 목록·지정(201, 이미 매니저면 그대로)·해제. 최대 10명 |
| GET · POST · DELETE | `/api/users/me/banned-words`, `/{wordId}` | 금칙어 목록·추가(201, 중복 409)·삭제. 최대 100개 |
| GET | `/api/users/me/chat-restrictions` | 지금 채팅이 막혀 있는 사람 |
| DELETE | `/api/users/me/chat-restrictions/{userId}` | 제한 풀기 |

금칙어는 **대소문자와 공백을 무시하고** 비교합니다(`Bad Word` 를 등록하면 `b a d w o r d` 도 막힙니다). 단어가 하나라도 들어 있으면 거절하고,
가리는 방식은 아닙니다.

---

## 채널 정보 — 오시마크 · 팬네임 · 데뷔/졸업 · 모델 크레딧

방송 제목처럼 매번 바뀌는 것이 아니라 **그 사람을 이루는 값들**입니다.

| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| GET | `/api/channels/{channelId}/profile` | 공개 | 채널 페이지에서 보여 줄 정보 |
| GET | `/api/users/me/channel-profile` | 필요 | 내 것 (편집용) |
| PUT | `/api/users/me/channel-profile` | 필요 | 저장 |

```json
{
  "channelId": 1, "nickname": "하늘별", "profileImage": null,
  "oshiMarkUrl": "/uploads/mark.png",
  "paidOshiMarkUrl": "/uploads/mark-paid.png",
  "fanName": "별무리", "subscriberCount": 1,
  "debutOn": "2026-09-10", "daysUntilDebut": 14,
  "graduatedOn": null, "graduated": false,
  "credits": [
    { "role": "ILLUSTRATOR", "name": "그림작가님", "link": "https://x.com/example" },
    { "role": "RIGGER", "name": "리거님", "link": null }
  ]
}
```

**아직 아무것도 안 채운 채널도 같은 형태로 내려옵니다.** 화면이 분기하지 않게 하기 위해서입니다.

### 오시마크

채널이 `/api/files/upload` 로 이미지를 올리고 그 주소를 넣습니다.
이미지는 두 장까지 정할 수 있습니다.

| 필드 | 누가 다나 |
|---|---|
| `oshiMarkUrl` | 이 채널을 구독한 사람 |
| `paidOshiMarkUrl` | 유료로 구독한 사람. 비워 두면 유료 구독자도 `oshiMarkUrl` 을 단다 |

**이름 옆(채팅과 댓글)에 붙는 규칙** — 채팅과 댓글이 같은 함수를 써서 어긋나지 않습니다.

1. 그 채널을 구독하지 않은 사람에게는 붙지 않습니다. 아무나 달 수 있으면 표식이 아니게 됩니다.
2. 구독자가 그 채널의 마크를 껐으면(`markVisible: false`) 붙지 않습니다.
3. 유료 구독자는 유료 마크를, 일반 구독자는 일반 마크를 답니다.
4. 마크는 **그 방송·영상의 채널 기준**입니다. A 채널을 구독했어도 B 채널 영상 댓글에는 A 마크가 붙지 않습니다.
5. 채널 주인 본인은 구독자가 아니므로 붙지 않습니다.

채팅과 댓글 응답에 `oshiMarkUrl` 과 `oshiTier` 가 함께 옵니다. 붙지 않으면 둘 다 `null` 입니다.
`oshiTier` 는 마크 이미지가 아니라 **그 사람의 실제 구독 등급**입니다(유료 마크가 없어 일반 마크를 달아도 `PAID`).
화면은 이 값으로 유료 구독자의 마크에 테두리를 둘러 구분합니다.

```json
{ "id": 1, "nickname": "열혈팬", "content": "오시 최고",
  "oshiMarkUrl": "/uploads/mark-paid.png", "oshiTier": "PAID", "createdAt": "..." }
```

시청자는 [`PUT /api/channels/{channelId}/subscription`](#내-구독-설정--put-apichannelschannelidsubscription) 으로
채널마다 마크를 달지 말지, 유료 구독으로 올릴지를 정합니다.

한 페이지의 마크는 **한 번에 가려냅니다.** 줄마다 물어보면 N+1 이 됩니다.

### 팬네임

채널에서 `구독자 1명` 대신 `별무리 1명` 으로 보입니다. 없으면 `구독자` 라고 씁니다.

### 데뷔 · 졸업

- `debutOn` 이 아직 오지 않았으면 `daysUntilDebut` 에 남은 날수가 옵니다 (화면은 `데뷔 D-14`).
- `graduatedOn` 을 넣으면 그 날부터 `graduated: true` 입니다. **예정일이 미래면 아직 졸업이 아닙니다.**
- 졸업해도 **올린 영상과 지난 방송은 그대로 남습니다.**

### 모델 크레딧

`role` 은 `ILLUSTRATOR` · `RIGGER` · `MODELER_3D` · `LOGO` · `BGM` · `OTHER`.
`link` 는 비워 둘 수 있습니다. 넣은 차례대로 나옵니다.

> **`credits` 를 보내지 않으면 기존 것을 그대로 둡니다.** 지우려면 빈 배열을 보내세요.
> 보내면 줄마다 따지지 않고 **통째로 갈아 끼웁니다** — 그 편이 단순합니다.

---

## 결제 — 유료 구독 · 방송 후원 (토스페이먼츠)

유료 구독은 **30일 이용권**을 한 번 결제하는 방식입니다. 자동으로 갱신되지 않고, 끝나기 전에
다시 결제하면 **남은 기간 뒤에 30일이 이어 붙습니다.** 기간이 지나면 일반(`BASIC`) 구독으로 돌아갑니다.
금액(기본 4,900원)과 기간은 서버 설정이 정하고, 브라우저가 보내는 금액은 믿지 않습니다.

| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| GET | `/api/payments/config` | 공개 | 결제가 켜져 있는지, 클라이언트 키·금액·기간, 후원 금액 목록(`donationAmounts`) |
| POST | `/api/lives/{liveId}/donations/orders` | 필요 | **후원 1단계** 주문 만들기 (201). 승인은 같은 `/api/payments/confirm` |
| POST | `/api/channels/{channelId}/membership/orders` | 필요 | **1단계** 주문 만들기 (201). 결제창에 넘길 값을 돌려준다 |
| POST | `/api/payments/confirm` | 필요 | **3단계** 승인하고 유료 구독 시작 |
| GET | `/api/users/me/payments` | 필요 | 내 결제 내역 (승인된 것과 환불된 것, 최신순) |
| POST | `/api/payments/{paymentId}/cancel` | 필요 | 내 결제 **환불** (승인 뒤 7일 안) |
| GET | `/api/users/me/earnings` | 필요 | 채널 주인의 **수익 장부** |
| POST | `/api/payments/webhook` | 공개(토큰) | **토스 웹훅** 수신 |
| GET | `/api/admin/payments` | 관리자 | 결제 목록 (`?status=DONE\|CANCELED\|FAILED\|READY`) |
| POST | `/api/admin/payments/{paymentId}/cancel` | 관리자 | 어느 결제든 환불 (사유 필요) |

### 흐름

```
① 주문       POST /api/channels/{id}/membership/orders   서버가 금액을 정해 주문(READY)을 만든다
② 결제창     브라우저가 토스 결제창을 연다 (카드·간편결제 / 계좌이체). 우리 서버는 관여하지 않는다
③ 승인       /?view=pay-result&paymentKey=..&orderId=..&amount=..  로 돌아오면
             POST /api/payments/confirm  → 서버가 주문과 대조 → 토스에 승인 → 구독 기간 늘림
```

돈이 빠져나가는 것은 ③ 에서 토스가 승인할 때입니다. 결제창만 열고 닫으면 청구되지 않습니다.

`GET /api/payments/config` 는 결제가 꺼져 있으면 `{ "enabled": false, "clientKey": null, ... }` 입니다.

```json
{ "enabled": true, "clientKey": "test_ck_...", "priceKrw": 4900, "periodDays": 30 }
```

주문 응답 — 결제창에 그대로 넘깁니다. 금액은 서버가 정한 값입니다.

```json
{ "orderId": "sub-1-3f2a...", "orderName": "하늘별 유료 구독 30일", "amount": 4900,
  "customerEmail": "fan@test.com", "customerName": "열혈팬" }
```

승인 요청은 결제창에서 돌아온 주소의 값 그대로입니다. 응답:

```json
{ "channelId": 1, "channelNickname": "하늘별", "tier": "PAID", "paidUntil": "2026-11-01T12:00:00",
  "amount": 4900, "method": "카드", "receiptUrl": "https://..." }
```

### 서버가 지키는 것

- **금액·주문은 서버 것만 믿는다.** 돌아온 `amount` 가 주문과 다르면 토스를 부르지 않고 400.
- **남의 주문은 승인할 수 없다** (403). 없는 주문은 404.
- **결제사 응답도 대조한다.** 상태가 `DONE` 이고 주문번호·결제 키·금액이 모두 맞을 때만 구독을 준다.
  하나라도 다르면 주문을 `FAILED` 로 하고 400.
- **두 번 청구하지 않는다.** 같은 주문의 승인은 한 번만 처리한다. 결제 성공 화면을 새로고침하거나
  요청이 겹쳐도 구독 기간은 한 번만 늘어난다(토스에는 주문번호를 `Idempotency-Key` 로 보내고,
  결과 반영은 주문 행을 잠가서 한다).
- **승인 응답을 놓쳐도 복구한다.** 토스가 `ALREADY_PROCESSED_PAYMENT` 를 주면 결제를 조회해 확인한 뒤 구독을 준다.
- **결제사 장애는 실패로 확정하지 않는다.** 닿지 못했거나 토스 쪽 오류(5xx)면 503 이고 주문은 `READY` 로
  남아 같은 주문으로 다시 시도할 수 있다. 토스가 거절한 경우(4xx)만 `FAILED` 로 하고 사유를 그대로 알려 준다.
- 시크릿 키는 서버 밖으로 나가지 않는다. 설정 응답에는 클라이언트 키만 있다.

### 결제가 꺼져 있을 때

토스 키가 하나라도 없으면 결제는 꺼집니다. 주문·승인은 503이고, 유료 전환은 결제 없는 자리표시로 남아
개발·데모를 막지 않습니다. 키를 넣는 순간 유료는 결제로만 시작됩니다.

### 환불 — `POST /api/payments/{paymentId}/cancel`

```json
{ "reason": "단순 변심" }
```

사유는 비워도 됩니다. 응답은 환불된 결제의 내역 한 줄(`status: "CANCELED"`)입니다.

- **사용자**는 내 결제를, 승인된 날부터 **환불 가능 기간(기본 7일, `MEMBERSHIP_REFUND_WINDOW_DAYS`) 안에** 환불할 수 있다.
  기간이 지났으면 400("관리자에게 문의해 주세요"). 남의 결제는 403.
- **관리자**는 기간과 상관없이 어느 결제든 환불할 수 있다. 사유가 꼭 필요하다(없으면 400).
- 토스에는 **전액 취소**만 요청한다(부분 환불은 없다). 토스가 거절하면 그 사유를 그대로 알려 주고 결제와 유료 구독은
  그대로다. 토스에 닿지 못했거나 토스 쪽 장애면 503 이고 결제는 그대로라 다시 시도할 수 있다.
  토스가 "이미 취소된 결제" 라고 하면(`ALREADY_CANCELED_PAYMENT`) 우리 쪽만 맞춘다.
- **환불하면 그 결제로 늘어났던 유료 기간(30일)만큼만 되돌린다.** 결제를 두 번 했다가 한 번만 환불하면 남은 한 번 몫은 그대로다.
  남은 유료 기간이 그보다 짧으면 일반 구독으로 내려가고, 구독 자체는 남는다.
- 같은 결제는 두 번 환불되지 않는다(이미 환불됐으면 400).

내 결제 내역(`GET /api/users/me/payments`)에는 `status`(`DONE`·`CANCELED`), `refundable`(지금 직접 환불할 수 있는지),
`refundDeadline`(직접 환불할 수 있는 마지막 때)이 있다.

### 웹훅 — `POST /api/payments/webhook`

토스 개발자센터의 웹훅 주소에 `https://{API 주소}/api/payments/webhook?token={TOSS_WEBHOOK_TOKEN}` 을 등록한다.
`PAYMENT_STATUS_CHANGED` 이벤트만 처리하고, 나머지는 200 으로 받고 넘긴다(토스는 200 이 아니면 다시 보낸다).

**본문을 믿지 않는다.** 이 주소는 누구나 부를 수 있으므로 본문은 "이 주문을 다시 보라" 는 신호로만 쓰고,
실제 상태는 서버가 토스에 직접 조회해서 정한다. 그래서 가짜 웹훅으로 할 수 있는 일은 서버가 토스를 한 번 조회하게 만드는 것뿐이다.
`TOSS_WEBHOOK_TOKEN` 을 설정하면 토큰이 없거나 틀린 요청은 401 로 막는다(설정하지 않으면 검사하지 않는다).

하는 일은 두 가지다.

1. **놓친 승인 구제** — 우리는 주문(`READY`)인데 토스에서는 승인(`DONE`)됐고 주문번호·결제 키·금액이 모두 맞으면,
   결제창에서 돌아오지 못했어도 유료 구독을 시작한다(결제창을 닫기 전에 연결이 끊긴 경우).
2. **상점관리자에서 한 취소 반영** — 우리는 승인(`DONE`)인데 토스에서는 취소(`CANCELED`)됐으면 우리 쪽도 취소로 맞추고
   늘어난 유료 기간을 되돌린다. 이때 토스를 다시 취소하지는 않는다.

토스에 닿지 못하면 503 을 돌려 토스가 나중에 다시 보내게 한다. 모르는 주문은 무시한다.

### 수익 장부 — `GET /api/users/me/earnings`

내 채널로 들어온 승인된 결제를 **승인된 달 기준**으로 합산한다. 환불은 승인된 달에서 뺀다.

```json
{
  "feePercent": 10,
  "total":  { "gross": 9800, "refunded": 4900, "fee": 490, "net": 4410, "count": 2, "membership": 0, "donation": 4900 },
  "months": [ { "month": "2026-10", "gross": 9800, "refunded": 4900, "fee": 490, "net": 4410, "count": 2, "membership": 0, "donation": 4900 } ]
}
```

- `membership`·`donation` 은 유료 구독으로 들어온 금액과 후원으로 들어온 금액이다(환불된 것은 뺀다).
- `fee` 는 `(gross - refunded)` 에 수수료율(`MEMBERSHIP_PLATFORM_FEE_PERCENT`, 기본 0%)을 곱한 값, `net` 은 정산 예정액이다.
- **장부일 뿐이다. 실제 송금은 하지 않는다.** 결제 금액은 서비스(가맹점)로 들어오므로 `net` 을 채널 주인에게 보내는 일은 따로 해야 한다.
- **결제한 사람은 어디에도 나오지 않는다.** 채널 주인이 알 필요가 없는 개인 정보라서 금액과 건수만 준다.

### 유료 만료 임박 알림

자동 갱신이 없으므로, 유료 구독이 끝나기 **3일 전부터**(`MEMBERSHIP_EXPIRY_NOTICE_DAYS`) 알림 `PAID_EXPIRING` 을 보낸다.
눌러서 갈 곳은 연장할 채널 페이지(`channelId`)다. 한 시간마다 서버가 확인하며, **같은 만료 시각에 대해서는 한 번만** 보낸다.
연장해서 만료가 바뀌면 다음 만료 때 다시 보낸다. 서버가 잠들어 있으면 확인도 쉬므로, 무료 호스팅이라면 주기적으로 깨워 두는 것이 좋다.

### 방송 후원 (슈퍼챗)

유료 구독과 **같은 결제 흐름**(주문 → 토스 결제창 → 승인)을 씁니다. 주문 종류는 `payments.kind`(`SUBSCRIPTION`·`DONATION`)로 가르고,
`POST /api/payments/confirm` 이 주문의 종류를 보고 승인 뒤에 할 일을 정합니다. 결제가 꺼져 있으면(키 없음) 후원 주문은 503 입니다.

```json
POST /api/lives/{liveId}/donations/orders
{ "amount": 5000, "message": "응원합니다!" }
```

- **금액은 서버가 허용한 목록에서만**(`DONATION_AMOUNTS`, 기본 1,000·3,000·5,000·10,000·30,000·50,000원). 메시지는 100자까지, 비워도 된다.
- **돈을 내기 전에 걸러냅니다.** 주문을 만드는 단계에서 방송 중인지, 자기 방송이 아닌지, 채팅이 막혔거나 채팅 대상에서 빠지지 않았는지,
  금칙어가 없는지를 확인합니다(슬로우 모드만 적용하지 않습니다). 막힌 사람이 결제한 뒤에야 거절되는 일이 없습니다.
- **승인되면** 채팅에 후원 메시지(`donationAmount`)가 올라가고 방송 주인에게 `DONATION` 알림이 갑니다. 구독 기간은 건드리지 않습니다.
  후원 단계(1~5)는 금액으로 정해지며 단계가 높을수록 채팅창 위의 띠로 오래 남습니다(30초 ~ 10분).
- 같은 주문을 다시 승인해도(새로고침) 후원 메시지는 한 번만 올라갑니다.
- **사용자는 후원을 직접 환불할 수 없습니다**(400). 이미 방송에 전달된 메시지라 받고 돌려받는 일이 쉬워지기 때문입니다.
  **관리자가 환불하면** 채팅에서 후원 메시지가 지워지고(고정돼 있었다면 고정도 풀림) 수익 장부에서 빠집니다.
- 내 결제 내역과 관리자 결제 목록에는 `kind` 가 있습니다.

### 아직 없는 것

- **실제 송금(정산 지급)** — 수익은 장부로만 보인다. 채널 주인에게 돈을 보내려면 토스 지급대행 같은 별도 계약과 연동이 필요하다.
- **자동 갱신(정기결제)** — 빌링키를 쓰는 자동결제는 토스와 별도 계약이 필요하고, 키 없이는 돈이 오가는 코드를 검증할 수 없어 붙이지 않았다.
  대신 만료 임박 알림으로 갱신을 놓치지 않게 한다.
- **부분 환불** — 환불은 전액만 한다.
- 가상계좌 등 입금 대기가 필요한 결제수단은 지원하지 않는다(카드·간편결제·계좌이체만).

---

## 인트로 — 처음 들어온 시청자에게 보여 주는 자기소개

낯선 방송에 들어가면 잡담 한복판에 떨어져 그대로 나가 버립니다. 그 사이에 한 겹을 둡니다.

| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| GET | `/api/lives/{liveId}/intro` | 공개 | **방송 화면이 쓰는 통로.** 방송 정보와 나란히 받는다 |
| GET | `/api/channels/{channelId}/intro` | 공개 | 채널 기준 조회 |
| POST | `/api/channels/{channelId}/intro/seen` | **공개** | 무엇을 눌렀는지 기록. `{"action":"SKIP"}` (`WATCHED` · `PASS`) |
| GET | `/api/users/me/intro` | 필요 | 내 인트로 |
| PUT | `/api/users/me/intro` | 필요 | 내 인트로 저장 (`videoUrl` · `headline` · `greeting`) |
| GET | `/api/lives/next?excludeLiveId=` | 공개 | **이어보기.** 아직 안 본 방송 하나. 없으면 404 |

응답의 **`showGate` 가 띄울지 말지를 정합니다.** 규칙을 화면마다 두면 금방 어긋나므로 서버 한 곳에서만 판단합니다.

```json
{
  "channelId": 1, "nickname": "하늘별", "profileImage": null,
  "videoUrl": null, "headline": "주로 게임하고 노래도 합니다",
  "greeting": "처음 오셨나요?", "subscriberCount": 0,
  "subscribed": false, "showGate": true
}
```

`videoUrl` 이 있으면 영상으로, 없으면 나머지 값들로 카드를 만들어 보여 줍니다.

### 띄우는 조건

넷 중 하나라도 어긋나면 `showGate` 는 `false` 이고 화면은 바로 방송을 틉니다.

- 올려 둔 인트로가 있을 것 (셋 다 비어 있으면 없는 것으로 친다)
- 이미 본 채널이 아닐 것
- 구독 중인 채널이 아닐 것 — 이미 아는 사람이다
- 자기 방송이 아닐 것

> **`intro/seen` 이 비로그인도 되는 이유** — 로그인하지 않은 사람에게 인트로가 매번 다시 뜨면
> 그건 광고입니다. 조회수 중복 판정과 같은 방식(계정 또는 접속 IP)으로 기억합니다.

### 끝나면 방송이 자동으로 나온다

인트로는 **끝나면 알아서 방송으로 넘어갑니다.** 누르고 들어가는 광고 같은 화면이 되지 않게 하려는 것입니다.

- `videoUrl` 이 있으면 **영상이 끝났을 때** 넘어갑니다.
- 영상이 없는 카드는 **8초 뒤** 넘어갑니다. 화면에 남은 초가 보이고, [멈추기]로 세기를 멈출 수 있습니다.
  [구독]을 누르면 읽는 중이라는 뜻이므로 저절로 멈춥니다.
- 자동으로 넘어간 것은 `WATCHED` 로 기록합니다. 끝까지 본 것이므로 같은 채널의 인트로는 다시 뜨지 않습니다.

### 스킵과 패스는 다른 동작이다

```
방송 진입 → 인트로 ─ 끝나면 자동으로 방송 (WATCHED)
             ├ [건너뛰고 방송 보기]  SKIP → 바로 이 방송을 본다
             └ [다음 방송]  PASS → 안 본다, /api/lives/next 로 다음 사람에게
```

`PASS` 가 쌓인 채널은 이어보기에서 다시 나오지 않습니다. 넘긴 의미가 없어지기 때문입니다.

---

## 알림 (`/api/notifications`)

모두 인증이 필요합니다.

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/notifications` | 알림 목록 (페이지, 최신순) |
| GET | `/api/notifications/unread-count` | 안 읽은 개수 → `{ "unreadCount": 3 }` |
| PATCH | `/api/notifications/{id}/read` | 하나 읽음 처리 |
| POST | `/api/notifications/read-all` | 전부 읽음 처리 → `{ "updated": 3 }` |

#### 알림 종류

| `type` | 언제 | 받는 사람 | `channelId` | `targetId` |
|---|---|---|---|---|
| `LIVE_START` | 구독한 채널이 방송을 시작 | 구독자 전원 | 방송인 id | 방송 id |
| `STREAM_COMMENT` | 내 영상에 댓글이 달림 | 영상 주인 | 댓글 쓴 사람 id | 영상 id |
| `COMMENT_REPLY` | 내 댓글에 답글이 달림 | 원 댓글 작성자 | 답글 쓴 사람 id | 영상 id |
| `PAID_EXPIRING` | 유료 구독이 곧 끝남 | 그 유료 구독자 | 연장할 채널 id | 없음 |
| `DONATION` | 내 방송에 후원이 들어옴 | 방송 주인 | 내 채널 id | 방송 id |
| `LIVE_SCHEDULED` | 구독한 채널이 방송을 예약하거나 시간을 바꾸거나 취소 | 구독자 전원 | 방송인 id | 방송 예약 id |
| `LIVE_REMINDER` | 예약한 방송이 곧 시작(10분 전) | 구독자 전원 | 방송인 id | 방송 예약 id |

**자기가 자기한테 보내는 알림은 남기지 않습니다** — 자기 영상에 자기가 단 댓글,
자기 댓글에 자기가 단 답글 모두 알림이 없습니다.

**답글은 원 댓글 작성자에게만 갑니다.** 영상 주인에게 따로 알리지 않습니다 —
한 번의 글에 두 사람이 알림을 받지 않게 하려는 겁니다.

알림 응답
```json
{
  "id": 1, "type": "LIVE_START",
  "message": "홍길동 님이 방송을 시작했습니다.",
  "channelId": 5, "targetId": 1,
  "read": false, "createdAt": "2026-08-21T09:00:00"
}
```

`targetId` 로 어디를 가리키는지는 `type` 마다 다릅니다 (위 표 참고).
화면에서는 `LIVE_START` · `DONATION` 은 방송 화면, `STREAM_COMMENT` · `COMMENT_REPLY` 는 영상 화면, `LIVE_SCHEDULED` · `LIVE_REMINDER` 는 방송 대기실,
`PAID_EXPIRING` 은 연장할 채널 페이지로 넘깁니다.

> 알림 종류를 새로 더할 때는 **이미 만들어진 DB 의 `notifications.type` 칸을 넓혀야** 합니다.
> 자세한 건 README 의 [이미 쓰던 DB 가 있다면 한 줄만 바꿔 주세요](../README.md#이미-쓰던-db-가-있다면-한-줄만-바꿔-주세요) 참고.

---

## 관리자 — 사용자 권한 (`/api/admin/users`)

모두 **관리자만** 접근할 수 있습니다.

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/admin/users?role=` | 사용자 목록 (페이지). `role` 생략 시 전체 |
| PATCH | `/api/admin/users/{userId}/role` | 권한 변경 |

권한 변경
```json
{ "role": "ADMIN" }
```
- `role`: `USER` · `ADMIN`
- 자기 자신을 `USER` 로 내리면 400. 되돌릴 수단이 없어지는 것을 막습니다.
  (요청자가 관리자여야 하므로, 이 규칙만으로 마지막 관리자도 함께 보호됩니다.)
- 없는 사용자면 404.

### 최초 관리자 만들기

관리자 승격 API 자체가 `ADMIN` 을 요구하므로, 첫 관리자는 설정으로만 만들 수 있습니다.

1. 평범하게 회원가입합니다.
2. `app.admin.emails` 에 그 이메일을 넣고 서버를 재시작합니다. (쉼표로 여러 개 가능)
   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=local --app.admin.emails=me@example.com'
   ```
   또는 `application-*.yaml` 의 `app.admin.emails` 에 적어 둡니다.
3. 기동 로그에 `관리자로 승격: me@example.com` 이 찍히면 완료입니다.
   계정이 없으면 경고만 남기고 넘어갑니다.

이후로는 `PATCH /api/admin/users/{userId}/role` 로 다른 사람을 승격시킬 수 있습니다.

---

## 신고 (`/api/reports`, `/api/admin/reports`)

| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| POST | `/api/reports` | 필요 | 신고 접수 (201) |
| GET | `/api/admin/reports?status=` | **관리자만** | 신고 목록 (페이지, 최신순). `status` 생략 시 전체 |
| PATCH | `/api/admin/reports/{id}` | **관리자만** | 처리 상태 변경 |

신고 접수
```json
{ "targetType": "STREAM", "targetId": 1, "reason": "부적절한 내용입니다." }
```
- `targetType`: `STREAM` · `LIVE_STREAM` · `COMMENT` · `USER`
- 대상이 존재하지 않으면 404. `reason` 최대 500자.

신고 응답
```json
{
  "id": 1,
  "reporterId": 3,
  "reporterNickname": "신고자",
  "targetType": "STREAM",
  "targetId": 1,
  "reason": "부적절한 내용입니다.",
  "status": "PENDING",
  "createdAt": "2026-08-25T09:00:00"
}
```

처리 상태 변경
```json
{ "status": "RESOLVED" }
```
- `status`: `PENDING` · `RESOLVED` · `REJECTED`

---

## 내부 API (외부 노출 금지)

nginx-rtmp 가 호출하는 콜백. JWT 가 아니라 스트림 키로 인증하므로
배포 시 `/api/internal/**` 는 반드시 내부망으로 제한해야 한다.

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/internal/rtmp/publish` | `name`=스트림 키. 검증 후 방송을 열고 재생 이름으로 **302 리다이렉트**. 키가 틀리면 403 |
| POST | `/api/internal/rtmp/publish-done` | 송출 종료. 방송을 ENDED 로 바꾼다 |

송출이 시작되면 구독자 전원에게 `LIVE_START` 알림이 생성됩니다.

---

## 송출 / 재생

- OBS 서버: `rtmp://localhost:1935/live`
- OBS 스트림 키: `GET /api/users/stream-key` 로 받은 값
- 재생(HLS): `GET /api/lives` 또는 `GET /api/channels/{id}/live` 가 돌려주는 `hlsUrl`

**스트림 키는 재생 URL 에 노출되지 않습니다.** `on_publish` 가 302 리다이렉트로
송출을 재생 이름(**방송마다 새로 만든 UUID**)으로 넘기기 때문입니다. 이름이 방송마다 달라서 멤버십 전용 방송의 주소를 볼 수 있는
사람에게만 내려 줄 수 있고, 다시보기도 방송끼리 덮어쓰지 않습니다.
실제 OBS 로 송출해 확인했습니다.

#### 리다이렉트는 이름을 바꾸는 게 아니라 세션을 하나 더 만든다

nginx-rtmp 는 `on_publish` 가 3xx 를 주면 **재생 이름으로 두 번째 송출 세션을 새로 만듭니다.**
스트림 키로 들어온 원래 세션도 그대로 살아 있습니다.

```
publish: name='bd4e1d90-…'   ← 스트림 키   (세션 1)
publish: name='8c6c7c99-…'   ← 재생 이름   (세션 2)
```

그래서 송출을 받는 application 에 `hls` 를 켜 두면 재생목록이 **두 벌** 생기고,
`/hls/{스트림키}.m3u8` 로도 방송이 그대로 재생됩니다. 인코딩·디스크도 두 배가 됩니다.

`streaming/nginx.conf` 는 이걸 피하려고 둘로 나눠 두었습니다.

| | 포트 | 하는 일 |
|---|---|---|
| `live` application | 1935 (공개) | 송출을 받아 **인증만** 한다. HLS 없음 |
| `hls` application | 127.0.0.1:1936 | 리다이렉트로 넘어온 재생 이름만 **HLS 로 만든다** |

1936 은 루프백에만 열려 있고 `docker-compose.yml` 에서도 공개하지 않으므로,
컨테이너 밖에서는 닿지 않습니다. `app.rtmp.redirect-base` 가 이 주소를 가리킵니다.

> 송출이 거부되면 `app.rtmp.rename-on-publish: false` 로 두고 확인하세요.
> 끄면 송출은 되지만 재생 URL 에 스트림 키가 그대로 드러납니다.

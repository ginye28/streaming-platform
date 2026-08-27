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

채널 정보 응답
```json
{
  "id": 1,
  "nickname": "채널명",
  "profileImage": null,
  "subscriberCount": 12,
  "streamCount": 4,
  "subscribedByMe": true
}
```

`subscribedByMe` 는 비로그인으로 조회하면 항상 `false` 입니다.

구독 토글 응답: `{ "subscribed": true, "subscriberCount": 13 }`
자기 자신을 구독하면 400.

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
      "parentId": null,
      "replies": [
        { "id": 13, "content": "답글", "nickname": "도라", "parentId": 12, "replies": [] }
      ]
    }
  ],
  "page": 0, "size": 20, "totalElements": 1, "totalPages": 1, "last": true
}
```

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
| GET | `/api/lives/{liveId}` | 공개 | 방송 상세 |
| GET | `/api/lives/{liveId}/chats` | 공개 | 채팅 내역 (페이지, 최신순) |
| GET | `/api/lives/settings` | 필요 | 다음 방송에 쓸 설정 |
| PUT | `/api/lives/settings` | 필요 | 방송 제목·설명·썸네일 저장 |
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
  "endedAt": null
}
```

`status` 는 `LIVE` 또는 `ENDED`. 방송이 끝나면 지난 방송 목록으로 넘어갑니다.

> OBS 는 방송 제목을 보내주지 않으므로, 송출 전에 `PUT /api/lives/settings` 로
> 제목을 저장해 두면 송출 시작 시 그 값이 쓰입니다. 저장하지 않으면
> "{닉네임} 님의 방송" 이 기본 제목이 됩니다.

---

## 채팅 (WebSocket)

STOMP over WebSocket 을 씁니다.

| 항목 | 값 |
|---|---|
| 접속 주소 | `ws://localhost:8080/ws` |
| 인증 | CONNECT 프레임에 `Authorization: Bearer {토큰}` |
| 보내기 | `/app/lives/{liveId}/chat` — `{ "content": "안녕하세요" }` |
| 받기 | `/topic/lives/{liveId}` 구독 |
| 시청자 수 | `/topic/lives/{liveId}/viewers` 구독 |

- 토큰 없이도 **연결과 구독은 가능**합니다(읽기 전용). 채팅 전송은 무시됩니다.
- 종료된 방송에는 채팅을 보낼 수 없습니다.
- 채팅방 구독 수가 곧 시청자 수로 집계됩니다. 서버를 여러 대로 늘리면
  인스턴스별로 따로 세어지므로 Redis 등으로 옮겨야 합니다.
- 접속 직후 채팅창은 `GET /api/lives/{liveId}/chats` 로 채웁니다.

받는 메시지
```json
{
  "id": 10, "liveId": 1, "userId": 5,
  "nickname": "채팅유저", "content": "안녕하세요",
  "createdAt": "2026-08-21T09:01:00"
}
```

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
화면에서는 `LIVE_START` 는 방송 화면, 나머지 둘은 영상 화면으로 넘깁니다.

> 알림 종류를 새로 더할 때는 **이미 만들어진 DB 의 `notifications.type` 칸을 넓혀야** 합니다.
> 자세한 건 README 의 [이미 쓰던 DB 라면 알림 칸을 한 번 넓혀야 합니다](../README.md#이미-쓰던-db-라면-알림-칸을-한-번-넓혀야-합니다) 참고.

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
| POST | `/api/internal/rtmp/publish` | `name`=스트림 키. 검증 후 방송을 열고 공개 이름으로 **302 리다이렉트**. 키가 틀리면 403 |
| POST | `/api/internal/rtmp/publish-done` | 송출 종료. 방송을 ENDED 로 바꾼다 |

송출이 시작되면 구독자 전원에게 `LIVE_START` 알림이 생성됩니다.

---

## 송출 / 재생

- OBS 서버: `rtmp://localhost:1935/live`
- OBS 스트림 키: `GET /api/users/stream-key` 로 받은 값
- 재생(HLS): `GET /api/lives` 또는 `GET /api/channels/{id}/live` 가 돌려주는 `hlsUrl`

**스트림 키는 재생 URL 에 노출되지 않습니다.** `on_publish` 가 302 리다이렉트로
송출을 공개 이름(계정마다 다른 UUID)으로 넘기기 때문입니다.
실제 OBS 로 송출해 확인했습니다.

#### 리다이렉트는 이름을 바꾸는 게 아니라 세션을 하나 더 만든다

nginx-rtmp 는 `on_publish` 가 3xx 를 주면 **공개 이름으로 두 번째 송출 세션을 새로 만듭니다.**
스트림 키로 들어온 원래 세션도 그대로 살아 있습니다.

```
publish: name='bd4e1d90-…'   ← 스트림 키   (세션 1)
publish: name='8c6c7c99-…'   ← 공개 이름   (세션 2)
```

그래서 송출을 받는 application 에 `hls` 를 켜 두면 재생목록이 **두 벌** 생기고,
`/hls/{스트림키}.m3u8` 로도 방송이 그대로 재생됩니다. 인코딩·디스크도 두 배가 됩니다.

`streaming/nginx.conf` 는 이걸 피하려고 둘로 나눠 두었습니다.

| | 포트 | 하는 일 |
|---|---|---|
| `live` application | 1935 (공개) | 송출을 받아 **인증만** 한다. HLS 없음 |
| `hls` application | 127.0.0.1:1936 | 리다이렉트로 넘어온 공개 이름만 **HLS 로 만든다** |

1936 은 루프백에만 열려 있고 `docker-compose.yml` 에서도 공개하지 않으므로,
컨테이너 밖에서는 닿지 않습니다. `app.rtmp.redirect-base` 가 이 주소를 가리킵니다.

> 송출이 거부되면 `app.rtmp.rename-on-publish: false` 로 두고 확인하세요.
> 끄면 송출은 되지만 재생 URL 에 스트림 키가 그대로 드러납니다.

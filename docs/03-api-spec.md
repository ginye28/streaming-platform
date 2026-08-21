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
응답: `{ "success": true, "data": { "accessToken": "eyJ..." } }`
- 401(이메일 또는 비밀번호 불일치)

---

## 내 계정 (`/api/users`)

모두 인증이 필요합니다. 공개 채널 조회는 `/api/channels` 에 있습니다.

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/users/me` | 내 정보 (`id`, `email`, `nickname`, `profileImage`) |
| PATCH | `/api/users/me` | 닉네임·프로필 이미지 수정 (닉네임 중복 시 409) |
| PUT | `/api/users/me/password` | 비밀번호 변경 |
| GET | `/api/users/me/subscriptions` | 내가 구독 중인 채널 목록 (페이지) |
| GET | `/api/users/stream-key` | OBS 송출용 스트림 키 조회 |
| POST | `/api/users/stream-key/regenerate` | 스트림 키 재발급 |

프로필 수정
```json
{ "nickname": "새닉네임", "profileImage": "/uploads/p.png" }
```

비밀번호 변경 — 현재 비밀번호가 틀리면 401, 기존과 같은 값이면 400
```json
{ "currentPassword": "password123", "newPassword": "newpassword456" }
```

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
| GET | `/api/streams` | 공개 | 목록 (페이지) |
| GET | `/api/streams/search?keyword=` | 공개 | 제목·설명 검색 (페이지) |
| GET | `/api/streams/popular` | 공개 | 조회수 상위 10 |
| GET | `/api/streams/latest` | 공개 | 최신 10 |
| GET | `/api/streams/subscribed` | **필요** | 구독 중인 채널들의 영상 피드 (페이지) |
| GET | `/api/streams/{id}` | 공개 | 상세 (조회수 +1) |
| PUT | `/api/streams/{id}` | 필요 | 수정 (본인만, 아니면 403) |
| DELETE | `/api/streams/{id}` | 필요 | 삭제 (본인만, 아니면 403) |

등록/수정 요청
```json
{
  "title": "제목",
  "description": "설명",
  "thumbnailUrl": "/uploads/xxx.png",
  "videoUrl": "/uploads/xxx.mp4"
}
```
`title`, `videoUrl` 필수.

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
  "createdAt": "2026-08-21T09:00:00"
}
```

- `likedByMe` 는 비로그인으로 조회하면 항상 `false` 입니다. 토큰을 함께 보내면 실제 값이 옵니다.
- `userId` 로 `/api/channels/{userId}` 채널 페이지에 연결할 수 있습니다.
- 영상을 삭제하면 달려 있던 댓글·좋아요도 함께 정리됩니다.

---

## 댓글

| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| POST | `/api/streams/{streamId}/comments` | 필요 | 등록 (201) |
| GET | `/api/streams/{streamId}/comments` | 공개 | 목록 (페이지) |
| PUT | `/api/streams/{streamId}/comments/{commentId}` | 필요 | 수정 (본인만) |
| DELETE | `/api/streams/{streamId}/comments/{commentId}` | 필요 | 삭제 (본인만) |

`{ "content": "댓글 내용" }` — 댓글이 해당 `streamId` 에 속하지 않으면 404.

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
- 응답: `{ "success": true, "data": { "url": "/uploads/{uuid}.mp4" } }`
- 저장된 파일은 `GET /uploads/{파일명}` 으로 공개 제공된다.

---

## 내부 API (외부 노출 금지)

nginx-rtmp 가 호출하는 콜백. JWT 가 아니라 스트림 키로 인증하므로
배포 시 `/api/internal/**` 는 반드시 내부망으로 제한해야 한다.

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/internal/rtmp/publish` | `name`=스트림 키. 2xx면 송출 허용, 403이면 거부 |
| POST | `/api/internal/rtmp/publish-done` | 송출 종료 알림 |

---

## 송출 / 재생

- OBS 서버: `rtmp://localhost:1935/live`
- OBS 스트림 키: `GET /api/users/stream-key` 로 받은 값
- 재생(HLS): `http://localhost:8081/hls/{스트림키}.m3u8`

> ⚠️ 현재는 송출 이름이 곧 스트림 키라서 재생 URL 에 키가 노출된다.
> 다음 단계로 `on_publish` 응답에서 공개 채널명으로 rename(3xx 리다이렉트)하도록
> 바꿔야 키가 감춰진다.

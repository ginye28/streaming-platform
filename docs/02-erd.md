# ERD

표 17개. `ddl-auto: update` 로 만들어지므로, 아래는 실제로 만들어진 MySQL 8.0 스키마를 그대로 옮긴 것입니다.

```
users ─┬─< streams ─┬─< comments ─┐
       │            ├─< likes     │ (comments.parent_id → comments.id)
       │            └─ categories │
       ├─< live_streams ─< chat_messages
       ├─ live_settings (1:1)
       ├─< subscriptions >─ users
       ├─< blocks >─ users
       ├─< notifications
       ├─< reports
       └─< refresh_tokens
```

---

## users

| 컬럼 | 타입 | | 설명 |
|---|---|---|---|
| id | bigint | PK | |
| email | varchar(100) | UNIQUE | 로그인 아이디 |
| password | varchar(255) | | BCrypt 해시 |
| nickname | varchar(20) | UNIQUE | |
| profile_image | varchar(255) | null 허용 | |
| role | enum | | `USER` · `ADMIN` |
| provider | enum | | `LOCAL` · `GOOGLE` · `KAKAO` · `NAVER` (지금은 `LOCAL` 만 씀) |
| provider_id | varchar(255) | null 허용 | 소셜 로그인용. 아직 안 씀 |
| stream_key | varchar(100) | UNIQUE, null 허용 | OBS 에 넣는 값. **밖으로 내보내면 안 됨** |
| public_name | varchar(100) | UNIQUE, null 허용 | 재생 URL 에 드러나는 이름. 스트림 키를 감추려고 둔 것 |
| created_at · updated_at | datetime(6) | | |

스트림 키와 공개 이름을 나눠 두는 이유는 [03-api-spec.md](03-api-spec.md) 참고.

---

## streams — 올린 영상 (VOD)

| 컬럼 | 타입 | | 설명 |
|---|---|---|---|
| id | bigint | PK | |
| user_id | bigint | FK → users | 올린 사람 |
| category_id | bigint | FK → categories, null 허용 | |
| title | varchar(100) | | |
| description | text | null 허용 | |
| video_url | varchar(255) | | `/uploads/{uuid}.mp4` |
| thumbnail_url | varchar(255) | null 허용 | 영상을 올리면 자동으로 채워짐 |
| view_count | bigint | | 같은 시청자 30분 1회 |
| created_at · updated_at | datetime(6) | | |

---

## comments

| 컬럼 | 타입 | | 설명 |
|---|---|---|---|
| id | bigint | PK | |
| stream_id | bigint | FK → streams | |
| user_id | bigint | FK → users | |
| parent_id | bigint | FK → comments, null 허용 | 있으면 답글 |
| content | text | | |
| created_at · updated_at | datetime(6) | | |

답글은 **두 단계까지만** 입니다. 답글에 다시 답글을 달면 400 입니다.
인덱스: `(stream_id, parent_id)`, `(parent_id)`.

---

## likes

| 컬럼 | 타입 | | |
|---|---|---|---|
| id | bigint | PK | |
| user_id | bigint | FK → users | |
| stream_id | bigint | FK → streams | |

`(user_id, stream_id)` UNIQUE — 한 사람이 한 영상에 한 번.

---

## categories

| 컬럼 | 타입 | | |
|---|---|---|---|
| id | bigint | PK | |
| name | varchar(30) | UNIQUE | 관리자만 만들 수 있음 |
| created_at | datetime(6) | | |

---

## live_streams — 방송 이력

| 컬럼 | 타입 | | 설명 |
|---|---|---|---|
| id | bigint | PK | |
| user_id | bigint | FK → users | |
| title | varchar(100) | | 방송 시작 시점의 `live_settings` 값을 복사 |
| description | text | null 허용 | |
| thumbnail_url | varchar(255) | null 허용 | |
| stream_name | varchar(100) | | 재생 URL 에 쓰이는 공개 이름 |
| status | enum | | `LIVE` · `ENDED` |
| started_at | datetime(6) | | |
| ended_at | datetime(6) | null 허용 | |
| peak_viewer_count | bigint | | 그 방송의 최고 동시 시청자 수 |

nginx-rtmp 의 `on_publish` 로 한 줄이 생기고, `on_publish_done` 으로 `ENDED` 가 됩니다.
인덱스: `(status)`, `(user_id)`.

---

## live_settings — 다음 방송에 쓸 값 (1:1)

| 컬럼 | 타입 | | |
|---|---|---|---|
| id | bigint | PK | |
| user_id | bigint | FK → users, UNIQUE | 사람당 한 줄 |
| title | varchar(100) | | |
| description | text | null 허용 | |
| thumbnail_url | varchar(255) | null 허용 | |

방송을 시작할 때 이 값이 `live_streams` 로 복사됩니다.
설정을 나중에 바꿔도 이미 시작한 방송의 제목은 안 바뀝니다.

---

## chat_messages

| 컬럼 | 타입 | | |
|---|---|---|---|
| id | bigint | PK | |
| live_stream_id | bigint | FK → live_streams | |
| user_id | bigint | FK → users | |
| content | varchar(500) | | |
| created_at | datetime(6) | | |

인덱스: `(live_stream_id, id)` — 방송별 최신순 조회용.

---

## subscriptions — 구독

| 컬럼 | 타입 | | |
|---|---|---|---|
| id | bigint | PK | |
| subscriber_id | bigint | FK → users | 구독하는 사람 |
| channel_id | bigint | FK → users | 구독당하는 사람 |

`(subscriber_id, channel_id)` UNIQUE.

---

## blocks — 차단

| 컬럼 | 타입 | | |
|---|---|---|---|
| id | bigint | PK | |
| blocker_id | bigint | FK → users | 차단한 사람 |
| blocked_id | bigint | FK → users | 차단당한 사람 |

`(blocker_id, blocked_id)` UNIQUE. 차단하면 그 사람 영상이 목록·피드에서 빠집니다.

---

## notifications

| 컬럼 | 타입 | | 설명 |
|---|---|---|---|
| id | bigint | PK | |
| recipient_id | bigint | FK → users | 받는 사람 |
| type | **varchar(30)** | | `LIVE_START` · `STREAM_COMMENT` · `COMMENT_REPLY` |
| message | varchar(200) | | 화면에 그대로 보여 줄 문장 |
| channel_id | bigint | null 허용 | 알림을 일으킨 사람 |
| target_id | bigint | null 허용 | 눌렀을 때 갈 곳 (방송 id 또는 영상 id) |
| is_read | bit(1) | | |
| created_at | datetime(6) | | |

`type` 이 ENUM 이 아니라 varchar 인 것은 **일부러** 입니다.
ENUM 으로 두면 종류를 하나 늘릴 때 `ddl-auto: update` 가 기존 칸을 넓혀 주지 못해
이미 만들어진 DB 에서 터집니다 ([README 의 겪은 사례](../README.md#이미-쓰던-db-라면-알림-칸을-한-번-넓혀야-합니다)).

인덱스: `(recipient_id, is_read)` — 안 읽은 개수 조회용.

---

## channel_intros — 첫 방문자에게 보여 줄 자기소개

| 컬럼 | 타입 | | 설명 |
|---|---|---|---|
| id | bigint | PK | |
| user_id | bigint | FK → users, **UNIQUE** | 사람당 한 줄 |
| video_url | varchar(255) | null 허용 | 짧은 소개 영상. 없으면 카드로 대신 보여 준다 |
| headline | varchar(60) | null 허용 | 한 줄 소개 |
| greeting | text | null 허용 | 소개글 |

**방송이 아니라 사람에게 붙습니다.** 방송마다 제목이 바뀌어도 "이 사람이 누구인가" 는 그대로라,
다음 방송 설정(`live_settings`)과 따로 뒀습니다.

---

## intro_impressions — 누가 인트로를 보고 무엇을 눌렀나

| 컬럼 | 타입 | | 설명 |
|---|---|---|---|
| id | bigint | PK | |
| viewer_key | varchar(120) | | 로그인했으면 계정, 아니면 접속 IP |
| channel_id | bigint | FK → users | |
| action | **varchar(20)** | | `SKIP` · `WATCHED` · `PASS` |
| updated_at | datetime(6) | | |

`(viewer_key, channel_id)` **UNIQUE** — 한 사람의 한 채널에 대한 **마지막 행동만** 남습니다.
인덱스: `(viewer_key, action)`.

두 가지 일을 합니다.

1. 한 번 본 인트로를 다시 띄우지 않습니다. (매번 뜨면 그건 광고입니다)
2. `PASS` 가 쌓인 채널은 "이어보기" 에서 건너뜁니다.

`action` 이 ENUM 이 아니라 varchar 인 것은 `notifications.type` 과 같은 이유입니다.

---

## channel_profiles — 채널의 정체성

| 컬럼 | 타입 | | 설명 |
|---|---|---|---|
| id | bigint | PK | |
| user_id | bigint | FK → users, **UNIQUE** | 사람당 한 줄 |
| oshi_mark_url | varchar(255) | null 허용 | 팬이 채팅에서 달고 다니는 표식 |
| fan_name | varchar(30) | null 허용 | 팬덤 이름 |
| debut_on | date | null 허용 | 데뷔일. 아직 안 왔으면 화면에서 남은 날을 센다 |
| graduated_on | date | null 허용 | 졸업일. 넣으면 졸업으로 표시된다 |

**졸업해도 올린 영상과 지난 방송은 그대로 남습니다.**

---

## model_credits — 모델을 만들어 준 사람들

| 컬럼 | 타입 | | 설명 |
|---|---|---|---|
| id | bigint | PK | |
| user_id | bigint | FK → users | |
| role | **varchar(30)** | | `ILLUSTRATOR` · `RIGGER` · `MODELER_3D` · `LOGO` · `BGM` · `OTHER` |
| name | varchar(60) | | |
| link | varchar(255) | null 허용 | 그 사람의 X · 픽시브 같은 주소 |
| position | int | | 본인이 정한 차례 |

인덱스: `(user_id, position)`.
채널마다 여러 명이 붙으므로 1:N 입니다. 저장할 때는 통째로 갈아 끼웁니다.

---

## reports — 신고

| 컬럼 | 타입 | | |
|---|---|---|---|
| id | bigint | PK | |
| reporter_id | bigint | FK → users | |
| target_type | enum | | `USER` · `STREAM` · `COMMENT` · `LIVE_STREAM` |
| target_id | bigint | | FK 가 아니라 그냥 숫자. 대상 표가 target_type 에 따라 달라져서 |
| reason | varchar(500) | | |
| status | enum | | `PENDING` · `RESOLVED` · `REJECTED` |
| created_at | datetime(6) | | |

---

## refresh_tokens

| 컬럼 | 타입 | | |
|---|---|---|---|
| id | bigint | PK | |
| user_id | bigint | FK → users | |
| token_hash | varchar(64) | UNIQUE | 토큰 원문이 아니라 해시만 저장 |
| expires_at | datetime(6) | | |
| created_at | datetime(6) | | |

로그아웃하면 지웁니다.

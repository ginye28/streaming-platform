# ERD

표 22개. 스키마는 `api/src/main/resources/db/migration/` 의 SQL 이 만듭니다.
아래는 실제로 만들어진 MySQL 8.0 스키마를 그대로 옮긴 것입니다.

```
users ─┬─< streams ─┬─< comments ─┐
       │            ├─< likes     │ (comments.parent_id → comments.id)
       │            └─ categories │
       ├─< live_streams ─< chat_messages
       ├─< live_schedules
       ├─ live_settings (1:1)
       ├─< channel_moderators >─ users
       ├─< chat_restrictions >─ users
       ├─< channel_banned_words
       ├─< subscriptions >─ users
       ├─< payments (user_id, channel_id, live_stream_id)
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
| stream_name | varchar(100) | | 재생 URL 에 쓰이는 이름. **방송마다 새로 만든 UUID** (다시보기도 이 이름으로 남는다) |
| status | enum | | `LIVE` · `ENDED` |
| started_at | datetime(6) | | |
| ended_at | datetime(6) | null 허용 | |
| peak_viewer_count | bigint | | 그 방송의 최고 동시 시청자 수 |
| audience | varchar(20) | 기본 `ALL` | 영상을 볼 수 있는 사람: `ALL` · `SUBSCRIBERS` · `PAID`. 시작할 때 정해지고 방송 중에는 안 바뀐다 |
| chat_audience | varchar(20) | 기본 `ALL` | 채팅할 수 있는 사람. 방송 중에도 주인이 바꾼다 |
| slow_mode_seconds | int | 기본 0 | 슬로우 모드 대기 시간(초). 0 이면 끔 |
| pinned_message_id | bigint | null 허용 | 채팅창 위에 고정한 메시지 |
| vod_available | bit(1) | 기본 0 | 다시보기가 남았는지(스트리밍 서버가 녹화하는 경우) |

nginx-rtmp 의 `on_publish` 로 한 줄이 생기고, `on_publish_done` 으로 `ENDED` 가 됩니다.
`audience`·`chat_audience`·`slow_mode_seconds` 는 가까운 방송 예약이 있으면 그 예약에서, 없으면 `live_settings` 에서 복사됩니다.
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
| audience · chat_audience | varchar(20) | 기본 `ALL` | 다음 방송을 볼 수 있는 사람 · 채팅할 수 있는 사람 |
| slow_mode_seconds | int | 기본 0 | 슬로우 모드로 시작할 때의 대기 시간 |

방송을 시작할 때 이 값이 `live_streams` 로 복사됩니다.
설정을 나중에 바꿔도 이미 시작한 방송의 제목은 안 바뀝니다.

---

## chat_messages

| 컬럼 | 타입 | | |
|---|---|---|---|
| id | bigint | PK | |
| live_stream_id | bigint | FK → live_streams | |
| user_id | bigint | FK → users | |
| content | varchar(500) | | 후원 메시지는 비어 있을 수 있다 |
| created_at | datetime(6) | | |
| donation_amount | int | null 허용 | 후원이면 금액(원). 일반 채팅은 null |
| donation_payment_id | bigint | null 허용 | 후원 메시지가 나온 결제. 환불되면 이 값으로 메시지를 찾아 지운다 |
| deleted | bit(1) | 기본 0 | 지운 메시지. 행을 남기고 표시만 켠다(내용은 어느 응답에도 나가지 않는다) |

인덱스: `(live_stream_id, id)` — 방송별 최신순·다시보기(오래된 순) 조회용.

---

## subscriptions — 구독

| 컬럼 | 타입 | | |
|---|---|---|---|
| id | bigint | PK | |
| subscriber_id | bigint | FK → users | 구독하는 사람 |
| channel_id | bigint | FK → users | 구독당하는 사람 |
| tier | enum('BASIC','PAID') | NOT NULL, 기본 BASIC | 구독 등급. 유료 구독자는 채널의 유료 오시마크를 단다 |
| mark_visible | bit(1) | NOT NULL, 기본 1 | 이 채널의 오시마크를 내 이름 옆에 보일지. 시청자가 채널마다 고른다 |
| expiry_notified_for | datetime(6) | null 허용 | 이 만료 시각에 대해 "곧 끝납니다" 알림을 이미 보냈다는 표시. 연장해서 만료가 바뀌면 다시 보낸다 |
| paid_until | datetime(6) | null 허용 | 유료 구독이 끝나는 때. 결제로 늘어난다. 지나면 tier 가 PAID 여도 일반으로 본다. NULL 은 기한 없음 |

`(subscriber_id, channel_id)` UNIQUE. 구독을 해제하면 행이 지워져 등급과 표시 설정도 사라진다.

---

## payments — 결제 (유료 구독 · 방송 후원)

| 컬럼 | 타입 | | |
|---|---|---|---|
| id | bigint | PK | |
| order_id | varchar(64) | **UNIQUE**, NOT NULL | 토스에 보내는 주문번호. 서버가 만든 무작위 값 |
| payment_key | varchar(200) | UNIQUE, null 허용 | 승인 뒤 토스가 주는 결제 키. 승인 전에는 비어 있다 |
| user_id | bigint | FK → users | 결제한 사람 |
| channel_id | bigint | FK → users | 구독 대상 채널 · 후원받은 방송의 주인 |
| kind | varchar(20) | 기본 `SUBSCRIPTION` | `SUBSCRIPTION`(유료 구독) · `DONATION`(방송 후원) |
| live_stream_id | bigint | null 허용 | 후원이 들어간 방송. 구독 결제는 null |
| donation_message | varchar(100) | null 허용 | 후원과 함께 남긴 말. 승인되면 채팅에 올라간다 |
| amount | int | NOT NULL | 서버가 정한 금액(원) |
| status | enum('DONE','FAILED','READY','CANCELED') | NOT NULL | 승인 완료 · 실패 · 주문 · 환불(취소). 값은 끝에 덧붙여 늘렸다 |
| method | varchar(50) | null 허용 | 카드 · 간편결제 · 계좌이체 등 |
| receipt_url | varchar(500) | null 허용 | 토스 영수증 주소 |
| approved_at | datetime(6) | null 허용 | 승인 시각 |
| canceled_at · cancel_reason | datetime(6) · varchar(200) | null 허용 | 환불한 때와 사유 |
| failure_code · failure_message | varchar | null 허용 | 실패 사유 |
| created_at | datetime(6) | NOT NULL | 주문을 만든 때 |

돈이 오간 기록이라 지우지 않습니다. 인덱스: `(user_id, id)` — 내 결제 내역용, `(live_stream_id)`.
구독과 후원이 같은 승인·환불 흐름을 타고, `kind` 로 승인 뒤에 할 일(유료 기간을 늘린다 / 채팅에 후원 메시지를 올린다)이 갈립니다.

---

## live_schedules — 방송 예약

| 컬럼 | 타입 | | |
|---|---|---|---|
| id | bigint | PK | |
| user_id | bigint | FK → users | 방송할 채널 |
| title | varchar(100) | | |
| description | text | null 허용 | |
| thumbnail_url | varchar(255) | null 허용 | |
| scheduled_at | datetime(6) | | 방송 시각(서버 시간대의 시각으로 저장. API 는 시간대가 붙은 시각으로 주고받는다) |
| audience · chat_audience | varchar(20) | 기본 `ALL` | 방송을 볼 수 있는 사람 · 채팅할 수 있는 사람 |
| status | varchar(20) | | `SCHEDULED` · `STARTED` · `CANCELED` · `EXPIRED` |
| live_stream_id | bigint | null 허용 | 이 예약으로 시작된 방송 |
| reminder_sent_at | datetime(6) | null 허용 | "곧 시작" 알림을 보낸 때. 시간이 바뀌면 비워 다시 보낸다 |
| created_at | datetime(6) | | |

인덱스: `(status, scheduled_at)`, `(user_id, status)`.
방송이 시작되면 가까운 예약 하나(시작 90분 전 ~ 6시간 후 범위)가 그 방송에 이어지고 `STARTED` 가 됩니다.

---

## channel_moderators — 채널 매니저

| 컬럼 | 타입 | | |
|---|---|---|---|
| id | bigint | PK | |
| channel_id | bigint | FK → users | 채널 주인 |
| user_id | bigint | FK → users | 매니저가 된 사람 |
| created_at | datetime(6) | | |

`(channel_id, user_id)` UNIQUE. 채널 한 곳에 최대 10명. 그 채널의 모든 방송에서 같은 권한을 갖습니다.

---

## chat_restrictions — 채팅 제한

| 컬럼 | 타입 | | |
|---|---|---|---|
| id | bigint | PK | |
| channel_id | bigint | FK → users | 제한을 건 채널 |
| user_id | bigint | FK → users | 제한된 사람 |
| restricted_until | datetime(6) | null 허용 | 이 시각까지 막힌다. **null 이면 강퇴**(풀어 줄 때까지) |
| reason | varchar(100) | null 허용 | |
| created_by | bigint | | 제한한 주인·매니저 |
| created_at | datetime(6) | | |

`(channel_id, user_id)` UNIQUE — 한 사람에 한 행이라 다시 제한하면 같은 행을 고칩니다.
제한은 방송이 아니라 **채널**에 걸려서 다음 방송에도 이어집니다.

---

## channel_banned_words — 금칙어

| 컬럼 | 타입 | | |
|---|---|---|---|
| id | bigint | PK | |
| channel_id | bigint | FK → users | |
| word | varchar(30) | | 소문자로 바꾸고 공백을 뗀 값 |
| created_at | datetime(6) | | |

`(channel_id, word)` UNIQUE. 채팅·후원 메시지의 같은 모양(소문자·공백 제거)에 단어가 들어 있으면 거절합니다. 채널당 최대 100개.

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
| type | **varchar(30)** | | `LIVE_START` · `STREAM_COMMENT` · `COMMENT_REPLY` · `PAID_EXPIRING` · `DONATION` · `LIVE_SCHEDULED` · `LIVE_REMINDER` |
| message | varchar(200) | | 화면에 그대로 보여 줄 문장 |
| channel_id | bigint | null 허용 | 알림을 일으킨 사람 |
| target_id | bigint | null 허용 | 눌렀을 때 갈 곳 (방송 id · 영상 id · 방송 예약 id) |
| is_read | bit(1) | | |
| created_at | datetime(6) | | |

`type` 이 ENUM 이 아니라 varchar 인 것은 **일부러** 입니다.
ENUM 으로 두면 종류를 하나 늘릴 때 기존 칸을 넓혀 주지 못해 이미 만들어진 DB 에서 터졌습니다
([겪은 사례](07-troubleshooting.md)).
지금은 스키마를 Flyway 가 관리하므로 칸을 넓히는 일도 SQL 파일로 적어 둡니다
([README 의 마이그레이션 절](../README.md#이미-쓰던-db-가-있다면-한-줄만-바꿔-주세요)).

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
| oshi_mark_url | varchar(255) | null 허용 | 구독자가 이름 옆에 달고 다니는 표식 |
| paid_oshi_mark_url | varchar(255) | null 허용 | 유료 구독자가 다는 표식. 비어 있으면 유료 구독자도 oshi_mark_url 을 단다 |
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

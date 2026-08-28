# 겪은 문제와 해결

만들면서 실제로 부딪힌 것들입니다. 원인까지 적어 두는 이유는, 같은 함정이
다른 자리에서 또 나오기 때문입니다.

> 받아서 돌려 볼 때 나는 문제(자바 경로, OBS, 도커)는 여기가 아니라
> [README 의 자주 겪는 문제](../README.md#자주-겪는-문제) 에 있습니다.

---

## 열거형에 값을 하나 더했더니 500

### 증상

알림 종류에 `COMMENT_REPLY` 를 더한 뒤, 이미 쓰던 DB 에서 답글을 달면
`Value not permitted for column "('LIVE_START')"` 로 터졌습니다.
알림과 댓글이 한 트랜잭션이라 **댓글까지 함께 취소**됐습니다.

### 원인

Hibernate 는 `@Enumerated(EnumType.STRING)` 을 DB 의 **네이티브 `ENUM` 칸**으로 만듭니다.
`ENUM('LIVE_START')` 처럼 그때 있던 값만 적힌 칸이 되는데,
`ddl-auto: update` 는 **이미 만들어진 칸을 넓혀 주지 않습니다.**

### 해결

칸을 문자열로 만들게 했습니다.

```java
@Enumerated(EnumType.STRING)
@JdbcTypeCode(SqlTypes.VARCHAR)
@Column(nullable = false, length = 30)
private Type type;
```

### 통하지 않았던 것

- `hibernate.type.prefer_native_enum_types: false` — 새 DB 로 확인해 봤지만 **아무 효과가 없었습니다.**
  효과 없는 설정을 남겨 두면 다음 사람이 속으므로 지웠습니다.
- `columnDefinition = "varchar(30)"` — 칸은 varchar 가 되지만
  Hibernate 가 붙이는 CHECK 제약은 그대로 남습니다.

### 남은 한계, 그리고 그걸 없앤 방법

varchar 로 바꿔도 Hibernate 는 `type IN (...)` CHECK 를 붙입니다.
종류를 또 늘리면 **그 CHECK 가 같은 자리에서 막습니다.**
`users.role` · `reports.status` · `reports.target_type` · `live_streams.status` ·
`users.provider` 는 아직 네이티브 `ENUM` 칸이라 사정이 같습니다.

즉 칸 타입을 바꾼 건 증상을 옮긴 것이지 원인을 없앤 게 아니었습니다.
**원인은 "스키마를 바꾸는 방법이 `ddl-auto: update` 하나뿐" 이라는 것**이었고,
그래서 Flyway 를 넣었습니다. 이제 칸을 넓히는 일은 SQL 파일 한 장으로 적어 둡니다.

```sql
-- api/src/main/resources/db/migration/V2__notification_type_add_new_subscriber.sql
ALTER TABLE notifications DROP CHECK notifications_chk_1;
ALTER TABLE notifications ADD CONSTRAINT notifications_chk_1
    CHECK (type IN ('LIVE_START', 'STREAM_COMMENT', 'COMMENT_REPLY', 'NEW_SUBSCRIBER'));
```

적어 두는 걸 깜빡하는 것까지가 이 문제의 일부라, CI 에 **스키마 (MySQL 8.0)** 작업을 뒀습니다.
빈 MySQL 에 마이그레이션을 전부 적용한 뒤 `ddl-auto: validate` 로 띄워 보고,
엔티티와 어긋나면 `missing column [...] in table [...]` 로 실패합니다.

[README 의 마이그레이션 절](../README.md#이미-쓰던-db-가-있다면-한-줄만-바꿔-주세요) 참고.

---

## 엔티티에 getter 를 하나 더했더니 쿼리가 깨짐

### 증상

`Comment` 에 `getParentId()` 를 더하자, 멀쩡히 돌던
`findByParentIdInOrderByCreatedAtAsc` 가 `Could not resolve attribute 'parentId'` 로 터졌습니다.

### 원인

Spring Data 는 메서드 **이름**으로 쿼리를 만듭니다. 이름을 풀 때 엔티티의 getter 도 후보로 봅니다.
`getParentId()` 가 생기자 `parentId` 를 **진짜 컬럼**으로 착각했는데,
실제 컬럼은 `parent` 를 타고 가는 `parent.id` 입니다.

### 해결

이름에 맡기지 않고 JPQL 을 직접 적었습니다.

```java
@Query("select c from Comment c where c.parent.id in :parentIds order by c.createdAt asc, c.id asc")
List<Comment> findRepliesOf(@Param("parentIds") Collection<Long> parentIds);
```

**교훈**: 엔티티에 편의 getter 를 더할 때는 이름으로 만든 쿼리가 있는지 먼저 봐야 합니다.

---

## 잘못된 요청이 400 이 아니라 500

### 증상

검색어를 비우고 부르면 400 이어야 하는데 500 이 났습니다.

### 원인

`@Validated` 가 붙은 컨트롤러에서 `@RequestParam` 의 `@NotBlank` · `@Size` 검증은
`MethodValidationInterceptor` 를 거쳐 **`ConstraintViolationException`** 으로 옵니다.
`ResponseEntityExceptionHandler` 가 자동으로 처리해 주는 `MethodArgumentNotValidException`
(요청 본문 검증)과는 **다른 예외**라, 핸들러가 없으면 catch-all 로 떨어져 500 이 됩니다.

### 해결

`GlobalExceptionHandler` 에 따로 받았습니다. 경로가 `search.keyword` 처럼 오므로
마지막 마디만 남겨 파라미터 이름으로 씁니다.

---

## main 에 넣은 설정이 테스트에서만 안 먹음

### 원인

`api/src/test/resources/application.yaml` 은 `main` 쪽 같은 이름 파일을
**보태는 게 아니라 통째로 대체합니다.** main 에만 넣으면 테스트에서는 그 설정이 없는 것과 같습니다.

### 해결

두 파일을 같이 손봅니다. 테스트 쪽에 "main 과 맞춰 둔 값" 이라는 주석을 남겨 뒀습니다.

---

## 송출 하나에 재생목록이 두 개

### 증상

OBS 로 한 번 송출했는데 `/tmp/hls` 에 `.m3u8` 이 두 개 생겼습니다.
그중 하나가 **스트림 키 이름**이라, 감추려던 키가 재생 URL 로 그대로 드러났습니다.

### 원인

`on_publish` 에서 3xx 를 돌려주면 nginx-rtmp 는 이름을 **바꾸는 게 아니라**
그 주소로 **두 번째 송출을 새로 만듭니다.** 원래 송출도 그대로 살아 있어서,
`hls on` 인 application 한 곳에서 둘 다 재생목록을 만듭니다.

### 해결

application 을 둘로 나눴습니다.

| | 포트 | 하는 일 |
|---|---|---|
| `live` | 1935 (바깥) | 스트림 키 확인만. HLS 안 만듦 |
| `hls` | 127.0.0.1:1936 (안쪽) | HLS 만 만듦. 밖에서 닿지 않음 |

`notify_relay_redirect on` 도 시도해 봤지만 재생목록은 여전히 두 개였습니다.

---

## prettier 를 돌렸더니 파일 전체가 뒤집힘

이 저장소에는 prettier 설정이 없습니다. 그냥 돌리면 기본값(2칸 · 세미콜론 · 큰따옴표)으로
바뀌어, 이 프로젝트 규칙(4칸 · 세미콜론 없음 · 작은따옴표)과 **정반대**가 됩니다.
검사는 `npm run lint` 로만 합니다.

---

## ffmpeg 이 멈추면 시간 제한이 안 걸림

### 원인

`process.getInputStream().readAllBytes()` 는 프로세스가 끝날 때까지 막힙니다.
그래서 그 뒤의 `waitFor(timeout, ...)` 은 이미 끝난 프로세스를 기다리는 셈이 되고,
ffmpeg 이 진짜로 멈추면 **읽기에 갇혀 시간 제한이 아무 소용이 없습니다.**

반대로 출력을 아예 안 읽으면 버퍼가 차서 ffmpeg 이 멈춥니다.

### 해결

출력을 `Redirect.DISCARD` 로 버리고 `waitFor(timeout)` 으로 묶었습니다.
진단은 종료 코드로 합니다.

---

## 화면 주소를 바꿨는데 안 바뀜

Vite 는 `VITE_...` 값을 **빌드할 때 코드에 박아 넣습니다.** 컨테이너를 다시 띄우는 것만으로는
안 바뀝니다. `--build` 로 다시 만들어야 합니다.

```bash
VITE_API_BASE_URL=https://api.내도메인 docker compose --profile full up -d --build web
```

---

## 프로필이 원하는 대로 안 걸림

`application.yaml` 의 `spring.profiles.include` 는 항상 더해지고,
`--spring.profiles.active` 로 준 것도 같이 켜집니다.
없는 프로필 파일(`application-secret.yaml` 등)은 그냥 무시됩니다 — **에러가 아닙니다.**

그래서 비밀 설정 파일이 없어서 `jwt.secret` 이 안 풀릴 때는,
파일을 만드는 대신 값이 들어 있는 프로필을 켜면 됩니다.

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

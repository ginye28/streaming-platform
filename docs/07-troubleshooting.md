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

---

## 브라우저 방송이 영상만 가고 소리가 안 감

### 증상

운영 사이트에서 브라우저 방송은 열리고 영상(H.264 1080p)도 HLS 로 나가는데, 시청 쪽에 소리가 없었습니다.

### 원인

서버가 소리를 버린 게 아니었습니다. 브라우저가 **소리 트랙 없이** 보낸 것입니다
(ffmpeg 로그에 `b:a ... not been used for any stream`).
화면 공유에는 소리가 안 들어가는 경우가 많고, 마이크 허용을 거부해도 조용히 영상만 보내고 있었습니다.

### 해결

방송 화면이 소리가 들어갔는지 알려 주고, 없으면 이유(마이크 거부 · 마이크 없음 · 화면 공유엔 소리 없음)와 해결법을 띄웁니다.
방송자가 "마이크 소리 포함"을 켜거나 탭 공유에서 "탭 오디오도 공유"를 체크하면 됩니다.
서버 쪽에서 확인하려면 최신 조각에 오디오가 있는지 `ffprobe <주소>/live-hls/<재생이름>-N.ts` 로 봅니다.

---

## 카드 결제를 하고 나면 폰에 404 가 뜸

### 증상

PC 에서 카드·간편결제를 고르고 폰으로 인증하면, 폰에는 "상점 화면에서 결제를 완료해주세요"가 뜨고
돌아가기를 누르면 `toss.im` 의 **404 Error** 가 보입니다. 그런데 사이트에는 결제가 성공해 있습니다.

### 원인

폰에 뜬 404 는 우리 사이트가 아니라 **토스(`toss.im`)의 화면**입니다. 주소창과 제목이 토스입니다.
결제 결과는 결제를 시작한 쪽(PC 화면)으로 돌아오기 때문에, 폰은 인증만 하고 끝나는 구조입니다.
토스 테스트 환경에서 폰의 "돌아가기" 링크가 갈 곳이 없어 404 로 보이는 것이고, 우리 코드로 바꿀 수 없습니다.

별개로 있던 실제 404 도 있었습니다. Vercel 이 `/` 가 아닌 주소를 새로고침하면 404 를 돌려줘서
`/pay-result` 로 돌아올 때 404 였던 문제는 `web/vercel.json` 의 SPA rewrite 로 고쳤습니다.

### 확인 방법

결제를 시작한 화면에 "결제가 완료되었습니다 · 금액 · 결제수단 · 영수증"과 이용 기간이 보이면 정상입니다.
결제창 안내 문구에도 "이 화면으로 돌아와야 결제가 끝난다"고 적어 두었습니다.

---

## 오시마크·영상 이미지가 깨지고 이름 옆에 글자가 삐져나옴

### 증상

채팅·댓글의 오시마크가 깨진 그림으로 보이고, 원이어야 할 표식이 타원이 되어 안에 "유료 구독자" 같은 글자가 들어갑니다.
영상 썸네일도 같이 깨집니다.

### 원인

이미지가 아니라 **파일이 없는 것**이었습니다. Render 무료 서버는 디스크가 임시라서 재시작·재배포·메모리 초과 뒤
`/app/uploads` 가 비워집니다. DB 에는 `/uploads/…png` 주소가 그대로 남아 있어서 404 가 났습니다
(`curl -I https://…/uploads/<이름>` 으로 확인). 깨진 `<img>` 는 대체 글자(alt)를 보여 주고, 16px 짜리 원 안에서 그 글자가 삐져나와 타원이 됐습니다.

### 해결

- 올릴 때 파일을 DB 에도 담고(`StoredFileBackup`), `/uploads/` 요청 때 디스크에 없으면 DB 에서 되살립니다(`UploadRestoreFilter`).
  서빙은 그대로 디스크에서 하므로 Range 요청(영상 건너뛰기)도 같습니다.
- 화면은 불러오지 못한 표식을 아무것도 그리지 않습니다(`AssetImage`). 표식 칸이 찌그러지지 않게 `aspect-ratio: 1` 도 줬습니다.
- **이 기능이 들어오기 전에 올린 파일은 이미 없어서 복구할 수 없습니다.** 오시마크·영상을 다시 올려야 합니다.

## 같은 학원·사무실에서 접속하면 "요청이 너무 많아요" 가 뜸

### 증상
많은 사람이 한 공유기(NAT)로 나가는 곳에서, 한 사람이 쓰는데도 429 가 뜨거나 로그인이 막힌다.

### 원인
속도 제한이 접속 IP 별이다. 같은 IP 를 쓰는 사람들이 한도(조회 분당 1500, 변경 분당 300, 로그인 10분에 60 등)를 나눠 쓴다.

### 확인 방법
관리자 화면 **보안** 탭의 `RATE_LIMITED` 기록에서 IP 와 주소(`path`), 종류(`AUTH 한도 초과` 등)를 본다. 서버 로그에서는 `보안 이벤트 RATE_LIMITED` 로 검색한다.

### 해결
일반 한도는 환경변수로 올린다(`RATE_LIMIT_READ_PER_MINUTE`, `RATE_LIMIT_WRITE_PER_MINUTE` …, [06-deployment.md](06-deployment.md)). 급하면 `RATE_LIMIT_ENABLED=false`. 로그인 IP 한도와 계정 실패 잠금(15분에 10번)은 [09-security.md](09-security.md) 의 고정 값이고, 한 계정이 잠시 막히는 것은 의도된 동작이다.

---

## 배포가 계속 실패하고 Render 가 재시작만 되풀이함 — "Detected failed migration" (V8)

### 증상
- Render 메일 "deploy failed for sp-api", 로그에 `Detected failed migration to version 8` 과 `Validate failed: Migrations have failed validation`, 인스턴스가 계속 restarted.
- 그 앞쪽 로그(`q=SQL` 로 검색)에 진짜 원인이 있다: `Illegal mix of collations (utf8mb4_unicode_ci,IMPLICIT) and (utf8mb4_0900_ai_ci,IMPLICIT) for operation '='`.

### 원인
- V8(데모 콘텐츠)이 서브쿼리의 글자와 테이블 열을 `=` 로 비교했다. **TiDB 는 두 쪽 정렬 규칙이 다르면 거절한다.** MySQL 은 같은 SQL 을 통과시켜서 CI(MySQL)와 로컬에서는 드러나지 않았다.
- 한 번 실패하면 Flyway 가 "V8 실패" 기록을 남기고, 그 뒤로는 SQL 을 고쳐도 기동 자체를 거부한다.

### 해결
- V8 의 비교에 `COLLATE utf8mb4_unicode_ci` 를 명시하고 "이미 있으면 넣지 않는다" 조건을 달았다(다시 돌려도 중복되지 않는다).
- `FlywayRepairConfig` 가 **실패 기록이 있을 때만** 그 기록을 정리(repair)하고 다시 마이그레이션한다. 그래서 운영 DB 를 손으로 고치지 않아도 다음 배포에서 풀린다. 실패 기록이 없으면 평소처럼 migrate 만 한다. repair 는 이미 성공한 마이그레이션을 건드리지 않는다.
- 앞으로 TiDB 에서 서브쿼리와 열을 비교하는 마이그레이션을 쓸 때는 비교하는 쪽에 `COLLATE` 를 명시한다.

# ERD

## User

|컬럼|타입|설명|
|---|---|---|
|id|BIGINT|PK|
|email|VARCHAR|이메일|
|password|VARCHAR|비밀번호|
|nickname|VARCHAR|닉네임|

---

## Stream

|컬럼|타입|설명|
|---|---|---|
|id|BIGINT|PK|
|title|VARCHAR|제목|
|status|ENUM|LIVE,END|
|streamer_id|BIGINT|FK|

---

## Chat

...

---

## Follow

...

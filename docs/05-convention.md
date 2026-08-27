# 개발 컨벤션

## 브랜치

기능별로 `feature/…` · `feat/…` 를 파서 작업하고 `main` 으로 PR 을 올립니다.
`develop` 브랜치는 안 씁니다.

## 커밋 메시지

`타입: 무엇을 했는지` — 본문은 **한국어**로 씁니다.

| 타입 | |
|---|---|
| `feat` | 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 동작은 그대로, 구조만 |
| `docs` | 문서 |
| `test` | 테스트 |
| `chore` | 설정 · 잡일 |

제목은 "무엇을 했다" 보다 **"무엇이 어떻게 됐다"** 에 가깝게 씁니다.
버그 수정이면 무엇이 잘못돼 있었는지가 제목에 들어가는 편이 나중에 찾기 좋습니다.

> `fix: 송출 중 재생목록이 스트림 키 이름으로도 만들어지던 문제`

---

## 자바

### 패키지

기능별로 자릅니다. 자세한 건 [04-architecture.md](04-architecture.md).

```
com.sp.api.{기능}.{controller|service|repository|entity|dto}
```

### 응답

컨트롤러는 항상 `ApiResponse` 로 감싸 돌려줍니다.

```java
return ResponseEntity.ok(ApiResponse.ok(data));
```

페이지는 `PageResponse` 로 감쌉니다. 스프링 `Page` 를 그대로 내보내지 않습니다.

### 예외

`BadRequestException` · `NotFoundException` 같은 것을 던지고,
`GlobalExceptionHandler` 한 곳에서 상태 코드와 문장으로 바꿉니다.
컨트롤러에서 `try/catch` 하지 않습니다.

### 엔티티

- `@Setter` 를 붙이지 않습니다. 바꿀 일이 있으면 뜻이 드러나는 메서드를 만듭니다
  (`update(...)`, `end()` 처럼).
- 연관은 전부 `LAZY`.
- 생성자는 필요한 것만 받는 것으로 만들고, JPA 용 기본 생성자는 `protected`.

### 주석

**무엇을 하는지가 아니라 왜 그렇게 했는지**를 씁니다. 한국어로 씁니다.
코드를 읽으면 알 수 있는 것은 안 씁니다.

```java
/**
 * 이 페이지에 실린 원 댓글들의 답글을 한 번에 가져온다.
 * 이름으로 만드는 쿼리(findByParentIdIn...)를 쓰면 Comment.getParentId() 때문에
 * parentId 를 컬럼으로 착각하니 JPQL 을 직접 적는다.
 */
```

---

## 자바스크립트 · JSX

| | |
|---|---|
| 들여쓰기 | **스페이스 4칸** |
| 세미콜론 | **안 붙임** |
| 따옴표 | **작은따옴표** |
| 컴포넌트 | 함수형만. 클래스 안 씀 |

### prettier 를 돌리지 마세요

이 저장소에는 prettier 설정이 없습니다. 그냥 돌리면 기본값(2칸 · 세미콜론 · 큰따옴표)으로
파일 전체가 바뀌어 위 규칙과 정반대가 됩니다. 검사는 `npm run lint` (eslint) 로 합니다.

### 서버 호출

전부 `src/api.js` 한 곳에 모읍니다. 컴포넌트에서 `fetch` 를 직접 부르지 않습니다.

데이터 읽기는 `useAsyncData(fetcher, deps)` 를 씁니다.

```js
const { data, error, loading, reload, fail } = useAsyncData(
    () => getStream(id),
    [id]
)
```

---

## 테스트

`api/src/test/java/com/sp/api/integration/` 에 통합 테스트로 씁니다.
`IntegrationTestSupport` 를 상속하면 `signupAndLogin` · `createStream` 같은 준비물이 딸려 옵니다.

- `@DisplayName` 은 **한국어 한 문장**으로, 무엇이 보장되는지 적습니다.
- 목(mock)보다 실제로 호출해 보는 쪽을 택했습니다. H2 로 돌아서 빠릅니다.

> `@DisplayName("같은 사람이 여러 번 열어도 조회수는 한 번만 오른다")`

### 설정 파일 주의

`api/src/test/resources/application.yaml` 은 `main` 쪽 `application.yaml` 을
**통째로 대체합니다.** main 에만 설정을 넣으면 테스트에서는 그 설정이 아예 없는 것과 같습니다.
둘 다 손봐야 합니다.

---

## 문서

`docs/` 아래 번호순으로 둡니다. 코드가 바뀌면 같은 PR 에서 문서도 고칩니다.
README 는 "받아서 돌려 보는 사람" 을 위한 문서이고, `docs/` 는 "안을 보는 사람" 을 위한 것입니다.

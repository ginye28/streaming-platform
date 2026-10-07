---
name: 스트리밍 플랫폼 (SP)
description: 밝은 낮의 서재 같은 화면에 초록 한 가지 포인트, 한쪽 모서리만 둥근 SP 시그니처 모양을 쓰는 라이브 중심 스트리밍 사이트.
colors:
  study-green: "#17604f"
  study-green-deep: "#114c3e"
  green-wash: "#e6f0ec"
  live-red: "#c2352c"
  live-red-wash: "#fbecea"
  ink: "#161a18"
  pencil-gray: "#6a716c"
  daylight: "#fbfcfb"
  paper: "#ffffff"
  sunken-paper: "#f1f4f2"
  hairline: "#e2e6e3"
  hairline-strong: "#c7d0cb"
  field-line: "#868e88"
  banner-black: "#0c100f"
  banner-text: "#f4f7f5"
  admin-ok: "#17795f"
  admin-ok-wash: "#e3f1eb"
  admin-warn: "#8a6100"
  admin-warn-wash: "#f9f0d8"
  night-canvas: "#101413"
  night-paper: "#171d1b"
  night-sunken: "#0b0f0e"
  night-hairline: "#28312d"
  night-hairline-strong: "#3b4642"
  night-field-line: "#6b7771"
  night-ink: "#e8ecea"
  night-muted: "#929c97"
  mint: "#56c6a6"
  mint-bright: "#74d6bb"
  night-green-wash: "#16302a"
  coral: "#ef8479"
  night-ink-on-coral: "#0c1211"
  night-coral-wash: "#2f1e1c"
typography:
  display:
    fontFamily: "'Gothic A1', system-ui, -apple-system, 'Segoe UI', 'Malgun Gothic', sans-serif"
    fontSize: "34px"
    fontWeight: 800
    lineHeight: 1.2
    letterSpacing: "-0.02em"
  headline:
    fontFamily: "'Gothic A1', system-ui, -apple-system, 'Segoe UI', 'Malgun Gothic', sans-serif"
    fontSize: "22px"
    fontWeight: 700
    lineHeight: 1.6
    letterSpacing: "-0.01em"
  title:
    fontFamily: "'Gothic A1', system-ui, -apple-system, 'Segoe UI', 'Malgun Gothic', sans-serif"
    fontSize: "16px"
    fontWeight: 700
    lineHeight: 1.6
  body:
    fontFamily: "'Gothic A1', system-ui, -apple-system, 'Segoe UI', 'Malgun Gothic', sans-serif"
    fontSize: "15px"
    fontWeight: 400
    lineHeight: 1.6
  label:
    fontFamily: "'Gothic A1', system-ui, -apple-system, 'Segoe UI', 'Malgun Gothic', sans-serif"
    fontSize: "13px"
    fontWeight: 600
    lineHeight: 1.6
rounded:
  2xs: "0 6px 0 6px"
  xs: "0 10px 0 10px"
  pill: "0 14px 0 14px"
  sm: "0 18px 0 18px"
  md: "0 22px 0 22px"
  lg: "0 44px 0 44px"
  circle: "50%"
spacing:
  gap: "20px"
  rail-gap: "14px"
  section: "28px"
  page-gutter: "20px"
  nav-height: "64px"
  sidebar: "248px"
components:
  button-default:
    backgroundColor: "{colors.sunken-paper}"
    textColor: "{colors.ink}"
    rounded: "{rounded.xs}"
    padding: "9px 11px"
  button-primary:
    backgroundColor: "{colors.study-green}"
    textColor: "{colors.paper}"
    rounded: "{rounded.xs}"
    padding: "9px 11px"
  button-primary-hover:
    backgroundColor: "{colors.study-green-deep}"
  input:
    backgroundColor: "{colors.paper}"
    textColor: "{colors.ink}"
    rounded: "{rounded.xs}"
    padding: "9px 11px"
  chip:
    backgroundColor: "{colors.paper}"
    textColor: "{colors.pencil-gray}"
    rounded: "{rounded.pill}"
    padding: "7px 14px"
  chip-on:
    backgroundColor: "{colors.study-green}"
    textColor: "{colors.paper}"
  nav-mark:
    backgroundColor: "{colors.study-green}"
    textColor: "{colors.paper}"
    rounded: "{rounded.pill}"
    size: "40px"
  side-link:
    textColor: "{colors.ink}"
    rounded: "{rounded.sm}"
    height: "44px"
  side-link-on:
    backgroundColor: "{colors.green-wash}"
    textColor: "{colors.study-green}"
  live-badge:
    backgroundColor: "{colors.live-red}"
    textColor: "{colors.paper}"
    rounded: "{rounded.pill}"
  tile-thumb:
    backgroundColor: "{colors.sunken-paper}"
    rounded: "{rounded.sm}"
  hero-banner:
    backgroundColor: "{colors.banner-black}"
    textColor: "{colors.banner-text}"
    rounded: "{rounded.lg}"
---

# Design System: 스트리밍 플랫폼 (SP)

## Overview

**Creative North Star: "낮의 서재" (The Daytime Study)**

코드의 스타일시트 첫머리가 이미 스스로를 "디자인 방향 B '낮의 서재'"라고 부른다. 이 이름을 그대로 가져왔다. 밝고 조용한 책상 위에 영상 썸네일이 놓여 있고, 화면은 그 콘텐츠가 주인공이 되도록 한 걸음 물러선다. 색은 거의 흰색에 가까운 바탕과 한 가지 깊은 초록뿐이다. 그 초록은 "확정"할 때와 "지금 여기"를 가리킬 때만 나온다. 나머지는 잉크색 글자와 머리카락 같은 얇은 선이다.

개성은 색이 아니라 **모양**에서 나온다. 왼쪽 위와 오른쪽 아래는 각지고, 오른쪽 위와 왼쪽 아래만 둥근 SP 시그니처 모서리가 로고, 버튼, 입력칸, 썸네일, 배너까지 크기만 달리하며 반복된다. 한 번 눈에 익으면 어느 화면에서도 이 사이트임을 알아볼 수 있다. 나이트 모드는 같은 색 계열을 어둡게 뒤집은 한 벌이고, 사용자가 직접 켤 때만 나온다. OS 설정은 따르지 않는다.

이 시스템은 한국어 화면을 전제로 한다. 줄바꿈은 단어 단위(`keep-all`)로 하고, 제목과 목록의 글은 두 줄에서 잘라 낸다.

**Key Characteristics:**
- 밝은 화면이 기본이고, 나이트 모드는 사용자가 켤 때만 쓴다.
- 포인트 색은 초록 하나. 빨강은 "LIVE"와 오류, 알림 배지라는 신호 전용이다.
- 시그니처 모서리: 대각선 두 곳만 둥글다.
- 깊이는 그림자보다 얇은 선과 면의 색 차이로 만든다. 그림자는 떠 있는 카드와 배너 위의 것에만 쓴다.
- 평문 CSS와 CSS 변수. 프레임워크나 유틸리티 클래스에 기대지 않는다.

## Colors

바탕은 거의 흰색, 포인트는 깊은 초록 하나. 모든 값은 `app.css` 의 `:root` 변수에 있고, `:root[data-theme='dark']` 가 같은 이름으로 어두운 값을 덮어쓴다.

### Primary
- **서재의 초록 Study Green** (#17604f, `--accent`): 확정하는 버튼(저장, 보내기, 채팅 전송), 선택된 칩, 로고 마크, 현재 메뉴 글자, 링크, 키보드 포커스 링에 쓴다. 한 화면에서 이 색이 차지하는 면은 작아야 한다.
- **깊은 서재 초록 Study Green Deep** (#114c3e, `--accent-hover`): 초록 버튼에 마우스를 올렸을 때.
- **초록 번짐 Green Wash** (#e6f0ec, `--accent-soft`): 현재 메뉴의 바탕, 아바타 빈 칸 바탕, 나이트 모드의 선택 면.

### Secondary (신호색)
- **라이브 레드 Live Red** (#c2352c, `--danger`): 방송 중 표시(LIVE 배지, 아바타 테두리, 방송 점), 오류 글자와 테두리, 알림 숫자 배지. 위험하거나 지금 일어나고 있는 일을 가리킨다. 장식에 쓰지 않는다. 이 색을 바탕으로 쓰는 배지의 글자는 `--on-danger`(라이트 흰색 5.5:1)다.
- **붉은 번짐 Live Red Wash** (#fbecea, `--danger-soft`): 오류 상자의 바탕.

### Neutral
- **잉크 Ink** (#161a18, `--text`): 본문과 제목 글자. 순수한 검정이 아니라 초록기가 도는 먹색이다.
- **연필 회색 Pencil Gray** (#6a716c, `--muted`): 메타 정보, 설명, 선택 안 된 칩, 빈 상태 안내. 눌린 종이 위에서도 4.5:1을 넘도록 맞춘 값이다.
- **낮빛 Daylight** (#fbfcfb, `--bg`): 페이지 바탕.
- **종이 Paper** (#ffffff, `--surface`): 머리 줄, 사이드바, 카드, 입력칸 같은 올라앉는 면.
- **눌린 종이 Sunken Paper** (#f1f4f2, `--sunken`): 기본 버튼, 검색 입력칸, 썸네일 빈 칸, 코드 조각처럼 한 칸 눌린 면.
- **머리카락 선 Hairline** (#e2e6e3, `--border`) / **진한 머리카락 선 Hairline Strong** (#c7d0cb, `--border-strong`): 면을 나누는 1px 선과 마우스를 올렸을 때의 선.
- **입력칸 선 Field Line** (#868e88, `--field-border`): 입력칸·선택칸·글상자의 테두리. 비어 있어도 칸의 경계가 보이도록 배경과 3:1 이상이다. 나이트에서는 #6b7771. 버튼과 칩은 계속 머리카락 선을 쓴다.
- **배너 먹색 Banner Black** (#0c100f)과 **배너 글자색 Banner Text** (#f4f7f5): 홈 상단 배너 전용. 테마와 상관없이 항상 어둡다. 어떤 썸네일이 와도 글자가 읽히게 하기 위해서다.

### 나이트 모드
같은 이름의 변수가 어두운 값을 가진다: 바탕 #101413, 면 #171d1b, 눌린 면 #0b0f0e, 선 #28312d / #3b4642, 글자 #e8ecea, 보조 글자 #929c97. 포인트 초록은 **민트** (#56c6a6, 호버 #74d6bb)로 밝아지고 초록 번짐은 #16302a, 빨강은 **코랄** (#ef8479, 번짐 #2f1e1c)이 되고, 코랄 배지의 글자는 흰색이 아니라 어두운 #0c1211(7.4:1)이다. 흰 글자는 2.6:1이라 읽히지 않는다.

### 관리자 전용
관리자 화면(`admin.css`)만 성공과 경고 색을 따로 가진다: 성공 #17795f (번짐 #e3f1eb), 경고 #8a6100 (번짐 #f9f0d8). 시청자 화면에서는 쓰지 않는다.

### Named Rules
**The One Green Rule.** 포인트는 초록 하나다. 새 포인트 색을 들이지 않는다. 채널마다 다른 색(하트 색 `--heart`)은 그 채널의 것이고, 사이트의 색이 아니다.
**The Red Means Live Rule.** 빨강은 방송 중이거나 잘못된 것일 때만 쓴다. 강조하고 싶다는 이유로 빨강을 쓰지 않는다.
**The Light By Default Rule.** 첫 화면은 항상 밝다. `prefers-color-scheme` 을 따르지 않고, 사용자가 켠 `localStorage` 의 선택만 따른다.

## Typography

**Display / Headline / Body / Label Font:** Gothic A1 (Google Fonts 에서 400 · 500 · 700 · 800 을 불러오고, 실패하면 `system-ui` → `Segoe UI` → `Malgun Gothic` → `sans-serif`)

**Character:** 서체는 한 가지뿐이다. 굵기(400/600/700/800)로만 위계를 만든다. 둥글고 정직한 한글 고딕이라 조용한 화면에서 제목이 크게 외치지 않고도 읽힌다.

### Hierarchy
- **Display** (800, 34px → 모바일 24px, 행간 1.2, 자간 -0.02em): 홈 배너의 방송 제목 하나뿐이다.
- **Headline** (700, 22px, 자간 -0.01em): 페이지 제목(`h2`).
- **Title** (700, 16px): 섹션 제목(`h3`), 채팅 머리글, 접이식 칸의 제목.
- **Body** (400, 15px, 행간 1.6): 본문, 입력칸, 버튼 글자. 설명 글은 줄바꿈을 보존한다.
- **Label** (600–800, 11–14px): 메타 정보 14px, 칩 13px, 타일 제목 14px(600), 채널 이름 14px(800), LIVE 배지 10–12px(800, 자간 0.04–0.05em).

로고 글자는 17px 800, 자간 -0.02em. 좁은 화면에서는 로고 글자를 숨기고 마크만 남긴다.

### Named Rules
**The Weight Is The Hierarchy Rule.** 위계는 크기보다 굵기로 만든다. 본문 15px 위로 올라가는 글자는 제목과 배너뿐이다.
**The Keep-All Rule.** 한글은 어절 중간에서 끊지 않는다. `body`에 `word-break: keep-all; overflow-wrap: break-word`를 전역으로 두고, 너무 긴 한 덩어리(주소 등)만 끊는다.

## Layout

화면은 **고정 머리 줄 + 왼쪽 사이드바 + 본문** 세 겹이다. 머리 줄은 높이 64px로 위에 붙고(sticky), 사이드바는 폭 248px로 그 아래에 붙으며, 본문은 최대 1140px에서 가운데 정렬하고 안쪽 여백 28px 20px 64px을 둔다. 간격의 기본 단위는 20px(`--gap`)이고, 섹션 사이는 28px, 가로 줄 안의 카드 사이는 14px이다.

- 카드 목록은 `repeat(auto-fill, minmax(220px, 1fr))` 격자. 홈의 가로 캐러셀 카드는 210px 고정 폭.
- 라이브 시청 화면은 900px 이상에서 영상과 채팅(320px)을 나란히 두고, 그 아래에서는 세로로 쌓는다. 채팅 높이는 72vh(최소 460px).
- **900px 이하**: 사이드바를 접고 머리 줄에 메뉴 링크를 보인다. 로고 글자는 숨긴다.
- **860px 이하**: 홈 배너가 420px → 320px로 낮아지고 제목이 24px이 되며, 도는 카드 바퀴는 접힌다. 바퀴가 도는 "링"은 겹쳐 도는 대신 가로 한 줄로 넘기는 형태(`ring--flat`, 78vw, 최대 300px, 스냅 스크롤)로 바뀐다.
- 그리드 칸에는 `minmax(0, 1fr)` 를 써서 안쪽 가로 줄이 길어져도 페이지가 옆으로 밀리지 않게 한다.

모바일에서 가로 넘침이 생기면 안 된다(375px 기준).

**라이브 시청 화면(900px 미만)**은 페이지를 스크롤하지 않고 한 화면에 쌓는다. 머리 줄은 검색을 숨겨 한 줄(65px)이 되고, 아래로 영상(16:9, 가장자리까지) · 제목 줄(제목과 채널 한 줄, 오른쪽에 "정보" 버튼) · 채팅이 이어지며, 채팅 목록만 안에서 스크롤하고 입력줄은 화면 맨 아래에 붙는다. 구독과 설명은 "정보"를 누르면 채팅을 밀어내지 않고 그 위에 시트로 얹힌다. 가로로 눕히면(높이 520px 이하) 영상은 왼쪽, 채팅은 오른쪽 2열이 된다. 채팅 연결 상태는 "연결 중…", 5초 넘으면 "서버를 깨우는 중이에요", 끊기면 "연결이 끊겼어요. 다시 연결하는 중…"으로 헤더에 보인다. 900px 이상에서는 영상 옆에 채팅 320px을 두고 제목·구독·설명을 영상 아래에 모두 펼친다.

## Elevation & Depth

기본은 **납작하다.** 면은 `1px` 머리카락 선과 바탕색 차이(낮빛 → 종이 → 눌린 종이)로 구분한다. 그림자는 상태가 바뀌거나 무언가 떠 있을 때만 나온다.

### Shadow Vocabulary
- **채팅 상자** (`box-shadow: 0 1px 2px rgb(22 26 24 / 0.05)`, 나이트에서는 `0 1px 2px rgb(0 0 0 / 0.4)`, `--shadow`): 유일한 상시 그림자. 선만으로 부족한 큰 상자에 숨결만 더한다.
- **링 카드** (`0 6px 16px rgb(12 18 16 / 0.08)`): 도는 카드의 기본 부유감.
- **맨 앞 링 카드** (`0 18px 40px rgb(12 18 16 / 0.16)`): 가장 앞에 온 카드가 한 단계 떠오른다.
- **배너 위 바퀴 카드** (`0 10px 30px rgb(0 0 0 / 0.45)`): 어두운 배너 위라 진하다.
- **"▶ 보러 가기" 버튼** (`0 6px 16px rgb(0 0 0 / 0.18)`): 맨 앞 링 카드 위로 튀어나온 잉크색 버튼.

### 모션
- 링 카드는 놓았을 때 **살짝 튕기며** 자리에 붙는다: `560ms cubic-bezier(0.34, 1.36, 0.64, 1)` (너비, 위치) + `400ms ease` (투명도). 끄는 동안에는 전환을 끈다.
- 하트 버튼은 마우스를 올리면 `scale(1.08)`, `0.15s ease`.
- 방송 중 점은 `1.4s ease-in-out` 으로 투명도 0.35까지 깜빡인다.
- 그 밖의 호버는 즉시 바뀌는 색·선 변화다. 눈에 띄는 입장 애니메이션은 없다.
- **동작 줄이기**: OS 의 `prefers-reduced-motion: reduce` 에서는 링·바퀴 카드와 하트의 전환, 방송 점 깜빡임, 가로 줄의 부드러운 스크롤을 모두 끈다. 카드는 걸리는 시간 없이 바로 자리를 바꾼다. 새 모션을 넣을 때는 같은 블록(`app.css` 맨 끝)에 끄는 규칙을 함께 넣는다.

### Named Rules
**The Flat-By-Default Rule.** 면은 납작하게 둔다. 그림자는 떠 있어야 할 이유가 있는 카드, 배너 위의 것, 상시 필요한 채팅 상자에만 쓴다.

## Shapes

SP의 모든 사각형은 같은 비율의 **시그니처 모서리**를 따른다. 왼쪽 위와 오른쪽 아래는 각지게 두고, 오른쪽 위와 왼쪽 아래만 둥글게 한다. 크기에 따라 반지름만 달라진다.

| 이름 | 값 | 어디에 |
|---|---|---|
| `--corner-2xs` | `0 6px 0 6px` | 아주 작은 라벨: LIVE 표시, 방송 순서 번호, 코드 조각 |
| `--corner-xs` | `0 10px 0 10px` | 버튼, 입력칸, 채팅 상자, 오류 상자, 접이식 칸, 알림 배지 |
| `--corner-pill` | `0 14px 0 14px` | 칩, 로고 마크, 뱃지(`pill`), 정체성 태그, 사이드바 채널 줄, 화살표 버튼, 배너의 "지금 보기" 버튼, "▶ 보러 가기" |
| `--corner-sm` | `0 18px 0 18px` | 타일 썸네일, 바퀴 카드, 사이드 메뉴 링크, 검색 입력칸 |
| `--corner-md` | `0 22px 0 22px` | 링 카드 |
| `--corner-lg` | `0 44px 0 44px` | 홈 배너 |

원은 사람과 상태에만 쓴다: 아바타, 하트 버튼, 오시마크, 방송 점. 사이드바의 28px 아바타는 xs 모서리를 쓴다. 테두리는 모두 1px이고, 강조할 때만 2px(포커스 링, 호버된 타일 윤곽)나 3px(하트 누른 카드)이 된다.

모서리 값은 모두 `--corner-*` 변수로 쓴다. 예외는 링 카드 안쪽 썸네일과 정보 칸의 한쪽만 둥근 모서리(`0 20px 0 0`, `0 0 0 20px`)로, 카드 md 모서리(22px)에서 테두리 두께를 뺀 값이라 직접 적는다.

### Named Rules
**The Signature Corner Rule.** 큰 면이든 작은 면이든 사각형이면 대각선 두 모서리만 둥글다. 네 모서리를 모두 둥글게(`border-radius: 8px`) 하지 않는다.

## Components

### Buttons
- **Shape:** 시그니처 모서리 xs (`0 10px 0 10px`), 안쪽 여백 9px 11px, 1px 선, 15px 글자.
- **Default:** 눌린 종이(#f1f4f2) 바탕에 잉크 글자. 호버하면 선이 진해질 뿐이다(`--border-strong`). 비활성은 투명도 0.45에 `not-allowed` 커서.
- **Primary:** `type="submit"` 과 채팅 전송 버튼만 초록(#17604f) 바탕, 흰 글자, 600. 호버는 #114c3e.
- **세로 폼 안에서:** 버튼은 입력칸 너비로 늘어나지 않고 왼쪽에 붙으며 좌우 여백이 20px로 늘어난다.
- 검색 버튼은 매번 누르기 때문에 일부러 색을 빼 두었다. **무언가를 확정하는 버튼만 색을 쓴다.**
- **확정 링크 `.cta`:** 로그인으로 보내는 "로그인하고 구독하기", "로그인하고 채팅 참여하기"처럼 링크지만 확정 행동인 것은 Primary 버튼과 같은 모양(초록 바탕, xs 모서리)이다. 작은 것은 `.cta--sm`, 줄 전체를 채우는 것은 `.cta--block`.

### Chips
- **Style:** 종이 바탕, 연필 회색 글자, 1px 머리카락 선, 시그니처 모서리 pill (`0 14px 0 14px`), 안쪽 여백 7px 14px, 13px 600.
- **State:** 호버 시 선과 글자가 진해진다. 선택되면(`chip--on`) 초록 바탕에 흰 글자, 선은 투명. 홈 필터는 드롭다운 대신 이 칩을 눌러 켠다.

### Cards / Containers
- **타일(홈 가로 줄, 목록):** 16:9 썸네일을 sm 모서리로 자르고, 아래에 두 줄까지의 제목(14px 600)과 12px 메타. 호버하면 썸네일에 2px 초록 윤곽이 2px 떨어져 생긴다. 썸네일이 없으면 눌린 종이 바탕에 연필 회색 "썸네일 없음" 글자.
- **카드 목록(`.card__thumb`):** 1px 선과 xs 모서리의 16:9 썸네일.
- **링 카드:** 종이 바탕, 1px 선, md 모서리, 위쪽 썸네일은 오른쪽 위만 20px로 둥글고 아래쪽 정보 칸은 왼쪽 아래만 20px로 둥글다. 채널 하트를 누르면 카드가 3px 채널 색 테두리와 그 색의 옅은 바탕으로 바뀐다.
- **채팅 상자:** 종이 바탕, 1px 선, xs 모서리, `--shadow`. 머리글 · 목록 · 입력 줄이 선으로 나뉜다. 채팅 닉네임은 초록 700.
- **접이식 칸(`details`):** 종이 바탕, 1px 선, xs 모서리. 요약 줄이 칸 전체를 덮고 호버하면 글자가 초록이 된다.

### Inputs / Fields
- **Style:** 종이 바탕, 1px 입력칸 선(`--field-border`), xs 모서리, 안쪽 여백 9px 11px. 호버하면 연필 회색으로 진해진다. 검색 입력칸은 눌린 종이 바탕에 `0 16px 0 16px`.
- **Focus:** 선이 초록으로 바뀌고 2px 초록 포커스 링이 나타난다(`outline-offset: 0`). 호버는 선이 진해진다.
- **Error:** 따로 입력칸 상태를 만들지 않고, 오류 상자(붉은 번짐 바탕 + 라이브 레드 선·글자, xs 모서리, 14px)를 폼 근처에 둔다.

### Navigation
- **머리 줄:** 높이 64px 이상, 종이 바탕, 아래 1px 선, 위에 붙는다. 왼쪽에 로고(40px 초록 마크에 흰 글자 15px 800 + 이름 17px 800), 가운데에 검색(최대 620px), 오른쪽에 알림·계정·테마 토글. 링크는 연필 회색이고 호버하면 잉크색.
- **사이드바:** 폭 248px, 종이 바탕, 오른쪽 1px 선. 메뉴 링크는 높이 44px, 15px 600, sm 모서리. 현재 위치는 초록 번짐 바탕에 초록 800 글자. 아래쪽 "구독 채널" 줄은 28px 아바타(방송 중이면 2px 빨간 윤곽)와 채널 이름, 방송 중이면 LIVE 표시.
- **모바일:** 사이드바 없이 머리 줄에 메뉴 링크를 보인다.

### Hero Banner (시그니처)
홈 맨 위의 큰 배너. 420px 높이, `--corner-lg`, 배너 먹색 바탕에 썸네일 이미지를 깔고 아래쪽을 짙은 그라디언트(`rgb(8 12 11 / 0.92 → 0.12)`)로 덮어 글자를 지킨다. 왼쪽 아래에 LIVE 배지와 제목(34px 800), 채널, 흰 "▶ 지금 보기" 버튼이 놓이고, 오른쪽 위에는 지금 방송 중인 채널의 카드가 **바퀴처럼 도는** 층이 얹힌다. 배너 자체는 돌지 않고 카드만 돈다. 좁은 화면에서는 바퀴를 접는다.

### Live Player Box (시청 화면)
영상은 16:9로 자리를 고정하고(`aspect-ratio`), 왼쪽 위에 배지를 얹는다. 송출이 오기 전에는 "곧 시작"(중립), 재생이 시작되면 LIVE 배지다. 송출이 아직 오지 않을 때는 빨간 오류가 아니라 영상 면 안에 "방송 준비 중"과 점 세 개를 띄운다(`role="status"`, 동작 줄이기에서는 점이 멈춘다). 진짜 재생 오류만 빨간 오류 상자와 `role="alert"`를 쓴다. 영상 아래는 제목(h1) · 채널명 · 채널 줄(아바타 40px, 구독자 수, 구독 막대) · 설명 순이다.

### Video Watch Screen (VOD)
올려 둔 영상도 라이브와 같은 영상 상자(`FilePlayer`)를 쓴다. 불러오는 동안은 영상 면 안에 "영상을 불러오는 중"(5초가 넘으면 "서버를 깨우는 중이에요"), 실패하면 이유와 "다시 시도"를 `role="alert"`로 띄운다. 썸네일이 `poster`다. 영상 아래는 라이브와 같은 순서(h1 제목, 채널 · 조회 · "3일 전" · 카테고리, 채널 줄, 반응 줄, 설명)이고, 좋아요는 누른 상태가 초록 하트와 `aria-pressed`로 보인다. 로그아웃 상태에서 좋아요를 누르면 보던 영상으로 돌아오는 로그인으로 보낸다. 신고·삭제·구독 취소·댓글 삭제의 확인은 브라우저 `prompt/confirm/alert`가 아니라 그 자리에 펼쳐지는 인라인 확인(`.stream__panel`, `.comment__actions`)이다. 시각은 "3일 전"처럼 상대 시간으로 쓰고 `<time>`의 `title`에 절대 시각을 둔다. 실패 안내의 "다시 시도"는 확정 행동이라 초록이고, 누르면 영상을 다시 받으면서 포커스를 영상 상자로 돌린다. 좋아요·신고·삭제가 실패해도 영상과 댓글은 그대로 두고 그 줄 옆에 `role="alert"` 오류를 보이며, 페이지 전체를 오류로 바꾸는 것은 영상 정보 자체를 못 받았을 때뿐이다. 댓글 불러오기 오류에는 "다시 시도"가 붙는다. 영상 상자는 높이 상한(60vh)에 걸려도 16:9를 지키도록 폭을 `min(100%, 60vh × 16/9)`로 제한한다. 900px 미만에서는 라이브와 같은 결로 머리 줄을 한 줄(검색 숨김)로 줄이고 영상을 가장자리까지 채우며 제목은 18px로 줄인다.

#### 영상 아래 정보와 "다른 영상"
채널 줄은 한 줄에 `[아바타] 이름 · 구독자 N명`과 구독 막대를 묶는다(이름과 얼굴이 따로 놀지 않는다). 로그아웃 상태의 "로그인하고 구독하기"와 "로그인하고 댓글 남기기"는 초록 바탕이 아니라 초록 윤곽과 옅은 바탕(`.cta--soft`)이라 제목과 좋아요보다 먼저 눈을 끌지 않는다. 확정하는 곳에는 여전히 초록 바탕을 쓴다. 로그아웃 좋아요 옆에는 "로그인하면 누를 수 있어요"를 눈에 보이게 적는다. 설명은 눌린 종이 바탕의 메모 상자이고, 길면 세 줄만 보이며 "더보기"로 펼친다. 답글이 3개를 넘는 댓글은 "답글 N개 보기"로 접는다. 시청이 끝난 뒤 갈 곳으로 같은 채널의 다른 영상을 보여 준다: 1200px 이상에서는 오른쪽 320px 열에 6개, 그보다 좁으면 영상 정보 아래에 4개를 가로 썸네일 목록으로 두고, 없으면 홈으로 가는 길을 준다.

### Donation Chips (채팅)
후원 줄은 두꺼운 색 테두리 대신 **금액 칩**(등급색 바탕에 `--on-tier` 글자, 12px 700, 2xs 모서리)으로 구분하고, 금액이 클수록 크고 굵게 눈에 띈다. 1~2등급은 줄 위에 칩만, 3등급은 1px 선과 옅은 틴트의 카드, 4등급은 1.5px 선과 13px 800 칩, 5등급은 2px 선과 더 진한 틴트, 14px 800 칩이다. 등급색은 1~3등급이 사이트의 초록을 점점 짙게(#2f7a68, #17604f, #0f4a3d, 흰 글자), 4~5등급(큰 후원)만 따뜻한 호박색(#c98a00, #d9690a, 어두운 글자)이다. 파랑·청록·빨강은 쓰지 않는다(빨강은 LIVE·오류 신호색). 모든 조합의 글자 대비는 4.5:1 이상이다. 위로 올려 읽는 중에 새 메시지가 쌓이면 "새 메시지 N개" 칩이 뜨고, 누르면 맨 아래로 돌아간다.

### Badges (LIVE · 알림)
- **LIVE 배지:** 라이브 레드 바탕에 흰 글자, 10–12px 800에 자간 0.04–0.05em, 작은 것은 `0 6px 0 6px`, 배너 위의 것은 pill 모서리와 앞쪽 흰 점.
- **알림 배지:** 라이브 레드, 흰 11px 800, xs 모서리, 최소 폭 18px.
- **오시마크:** 채팅 닉네임 옆 16px 원형 이미지. 유료 구독자는 1.5px 초록 테두리 링으로 구분한다.

## Do's and Don'ts

### Do:
- **Do** 새 사각형은 `--corner-xs / pill / sm / md / lg` 중 크기에 맞는 변수를 쓴다. 변수를 쓰면 시그니처 모양이 저절로 따라온다.
- **Do** 후원·강조는 두꺼운 한쪽 색 테두리가 아니라 1px 선, 틴트 바탕, 색 바탕 칩으로 표현한다.
- **Do** 위험색(`--danger`) 바탕의 배지 글자는 `#fff` 대신 `var(--on-danger)` 를 쓴다.
- **Do** 색은 `:root` 변수(`var(--accent)`, `var(--surface)` 등)로만 쓴다. 값을 직접 쓰면 나이트 모드에서 깨진다.
- **Do** 확정하는 버튼에만 초록을 쓴다. 나머지 버튼은 눌린 종이 바탕으로 조용히 둔다.
- **Do** 새 색을 만들면 라이트와 `:root[data-theme='dark']` 두 벌을 함께 만든다.
- **Do** 한국어 제목과 이름에 `word-break: keep-all`, 길면 두 줄에서 `-webkit-line-clamp: 2` 로 자른다.
- **Do** 썸네일이 없는 빈 칸도 디자인한다: 눌린 종이 바탕에 "썸네일 없음" 같은 흐린 글자, 같은 비율(16:9)과 같은 모서리.
- **Do** 바탕이 이미지인 곳(배너)은 테마와 상관없는 고정된 어두운 그라디언트로 글자를 보호한다.
- **Do** 확인이 필요한 행동(삭제, 신고, 구독 취소)은 브라우저 `prompt/confirm/alert` 대신 그 자리에서 펼쳐지는 인라인 확인을 쓴다.
- **Do** 로그인으로 보내는 링크는 `loginTo()` 로 돌아올 곳을 함께 넘긴다. 로그인 뒤에는 보던 화면으로 돌아오고, 사이트 안의 화면만 허용한다.
- **Do** 하트 색 번호 배지의 글자는 `heartInk()` 가 고른다. 흰 글자가 4.5:1을 못 넘는 밝은 색(호박, 초록, 분홍, 청록)에는 어두운 글자를 쓴다.
- **Do** 새 전환·애니메이션·부드러운 스크롤은 `prefers-reduced-motion` 에서 꺼지게 한다.
- **Do** 새 카드나 목록은 375px 폭에서 가로로 넘치지 않는지 확인한다.

### Don't:
- **Don't** 네 모서리를 모두 같은 반지름으로 둥글게 하지 않는다(`border-radius: 8px`). SP 의 모양이 사라진다.
- **Don't** 초록 외의 포인트 색을 새로 들이지 않는다. 채널별 하트 색은 예외로, 그 채널 안에서만 쓴다.
- **Don't** 빨강을 장식이나 강조에 쓰지 않는다. 방송 중이거나 오류일 때만 쓴다.
- **Don't** OS 다크 모드 설정을 따라가지 않는다. 나이트 모드는 사용자가 직접 켠다.
- **Don't** 그림자로 위계를 만들지 않는다. 선과 면의 색 차이가 먼저다.
- **Don't** Tailwind 나 유틸리티 클래스, CSS-in-JS 를 들이지 않는다. 이 시스템은 평문 CSS 와 변수로 되어 있다.
- **Don't** 서체를 더하지 않는다. Gothic A1 한 가지의 굵기로 위계를 만든다.

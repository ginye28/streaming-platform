---
name: 스트리밍 플랫폼 (SP)
description: 흰 바탕에 먹색 글자, 그린은 행동에만, 퍼플은 보조에만, 빨강은 LIVE에만 쓰는 차분한 라이브 중심 스트리밍 사이트. 홈은 위에 어두운 시네마 슬라이더, 아래에 카드 격자다.
colors:
  action-green: "#10b981"
  action-green-deep: "#34d399"
  green-text: "#047857"
  green-wash: "#e6f5ee"
  green-wash-hover: "#d2eee1"
  green-ink: "#047857"
  violet: "#a78bfa"
  violet-wash: "#f1ecfd"
  violet-ink: "#5b21b6"
  violet-line: "#ddd3fa"
  green-line: "#cfe6da"
  live-red: "#cf1c3b"
  live-red-wash: "#fdeaee"
  ink: "#191f28"
  slate: "#4e5968"
  slate-faint: "#5f6b7a"
  white: "#ffffff"
  sunken-gray: "#f2f4f6"
  hover-gray: "#e8ebef"
  hairline: "#e5e8eb"
  hairline-strong: "#d1d6db"
  field-line: "#8b95a1"
  night-canvas: "#0f0f12"
  night-paper: "#16161a"
  night-sunken: "#1e1e24"
  night-hover: "#2a2a32"
  night-hairline: "#26262e"
  night-hairline-strong: "#3a3a45"
  night-field-line: "#7b8490"
  night-ink: "#f1f1f3"
  night-slate: "#a8b0bb"
  night-slate-faint: "#8d96a3"
  night-green: "#10b981"
  night-green-deep: "#34d399"
  night-green-wash: "#0c2a21"
  night-green-ink: "#6ee7b7"
  night-on-green: "#04130d"
  night-violet: "#a78bfa"
  night-violet-wash: "#1b1533"
  night-violet-ink: "#c4b5fd"
  night-violet-line: "#2c2354"
  night-live: "#d92d46"
  night-danger: "#ff6b81"
  night-danger-wash: "#3a1620"
  night-on-danger: "#1a0a0e"
  tint-blue: "#e8f3ff"
  tint-rose: "#fde8ee"
  tint-green: "#e6f6ec"
  tint-amber: "#fff1db"
  tint-violet: "#efe9ff"
  tint-teal: "#e2f6f8"
  tint-pink: "#fbe9f6"
  tint-gray: "#eef0f3"
typography:
  display:
    fontFamily: "'Pretendard Variable', Pretendard, -apple-system, BlinkMacSystemFont, system-ui, 'Apple SD Gothic Neo', 'Malgun Gothic', 'Noto Sans KR', sans-serif"
    fontSize: "24px"
    fontWeight: 700
    lineHeight: 1.3
    letterSpacing: "-0.02em"
  headline:
    fontFamily: "'Pretendard Variable', Pretendard, -apple-system, BlinkMacSystemFont, system-ui, 'Apple SD Gothic Neo', 'Malgun Gothic', 'Noto Sans KR', sans-serif"
    fontSize: "20px"
    fontWeight: 700
    lineHeight: 1.3
    letterSpacing: "-0.02em"
  title:
    fontFamily: "'Pretendard Variable', Pretendard, -apple-system, BlinkMacSystemFont, system-ui, 'Apple SD Gothic Neo', 'Malgun Gothic', 'Noto Sans KR', sans-serif"
    fontSize: "15px"
    fontWeight: 600
    lineHeight: 1.4
    letterSpacing: "-0.01em"
  body:
    fontFamily: "'Pretendard Variable', Pretendard, -apple-system, BlinkMacSystemFont, system-ui, 'Apple SD Gothic Neo', 'Malgun Gothic', 'Noto Sans KR', sans-serif"
    fontSize: "15px"
    fontWeight: 400
    lineHeight: 1.55
  label:
    fontFamily: "'Pretendard Variable', Pretendard, -apple-system, BlinkMacSystemFont, system-ui, 'Apple SD Gothic Neo', 'Malgun Gothic', 'Noto Sans KR', sans-serif"
    fontSize: "13px"
    fontWeight: 600
    lineHeight: 1.4
rounded:
  xs: "4px"
  md: "8px"
  lg: "12px"
  xl: "16px"
  pill: "999px"
  circle: "50%"
spacing:
  gap: "20px"
  page-pad: "24px (모바일 16px)"
  header-height: "56px"
  main-max: "1140px (넓은 화면 1680px)"
components:
  button-default:
    backgroundColor: "{colors.sunken-gray}"
    textColor: "{colors.ink}"
    rounded: "{rounded.md}"
    height: "40px"
    padding: "0 16px"
  button-default-hover:
    backgroundColor: "{colors.hover-gray}"
  button-primary:
    backgroundColor: "{colors.action-green}"
    textColor: "{colors.white}"
    rounded: "{rounded.md}"
    height: "40px"
    padding: "0 16px"
  button-primary-hover:
    backgroundColor: "{colors.action-green-deep}"
  button-tonal:
    backgroundColor: "{colors.green-wash}"
    textColor: "{colors.green-ink}"
    rounded: "{rounded.md}"
    height: "40px"
    padding: "0 16px"
  button-tonal-hover:
    backgroundColor: "{colors.green-wash-hover}"
  button-outline:
    backgroundColor: "transparent"
    textColor: "{colors.green-ink}"
    rounded: "{rounded.md}"
    height: "40px"
    padding: "0 16px"
  button-large:
    height: "48px"
    padding: "0 24px"
  chip:
    backgroundColor: "{colors.sunken-gray}"
    textColor: "{colors.ink}"
    rounded: "{rounded.pill}"
    height: "34px"
    padding: "0 14px"
  chip-on:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.white}"
  search:
    backgroundColor: "{colors.white}"
    rounded: "{rounded.pill}"
    height: "40px"
  nav-link:
    textColor: "{colors.slate}"
    rounded: "{rounded.pill}"
    height: "36px"
    padding: "0 14px"
  nav-link-on:
    backgroundColor: "{colors.sunken-gray}"
    textColor: "{colors.ink}"
  topbar:
    backgroundColor: "{colors.white}"
    height: "56px"
  menu:
    backgroundColor: "{colors.white}"
    rounded: "{rounded.xl}"
    width: "280px"
  live-badge:
    backgroundColor: "{colors.live-red}"
    textColor: "{colors.white}"
    rounded: "6px"
  video-thumb:
    backgroundColor: "{colors.sunken-gray}"
    rounded: "{rounded.lg}"
  empty-panel:
    backgroundColor: "{colors.sunken-gray}"
    rounded: "{rounded.xl}"
    padding: "40px 24px"
---

# Design System: 스트리밍 플랫폼 (SP)

> 이 문서는 새 체계(`base.css` · `shell.css` · `home.css`, 홈과 상단바)를 출고된 코드에서 도출해 적었다. 옛 체계(에메랄드 · 나뭇잎 모서리 · 유리 · 독 · Gothic A1)는 폐기됐다. 아직 새 체계로 옮기지 않은 화면은 맨 아래 "아직 새 체계로 옮기지 않은 화면"에 적었다.

## Overview

**Creative North Star: "평평한 방송국 로비" (The Flat Broadcast Lobby)**

흰 바탕, 먹색 글자, 차가운 중성 회색 몇 단계. 기준은 YouTube의 구조다. 맨 위 한 줄(상단바)이 길 안내의 전부이고, 그 아래로 칩 줄 · 대표 방송 · 카드 격자가 흘러내린다. 카드에는 테두리도 그림자도 없고 썸네일만 둥글며 글은 그 아래에 놓인다. 콘텐츠(썸네일, 방송 화면)가 늘 주인공이고 인터페이스는 조용하다.

색은 세 가지 일만 한다. 그린은 **행동**(버튼, 링크, 키보드 포커스)에, 퍼플(`--violet`)은 칩·보조 버튼·프로필 고리 같은 **보조**에, 빨강은 **방송 중**과 알림 숫자·오류에. 선택된 상태는 그린이 아니라 중성의 반전(먹색 바탕, 흰 글자)이나 한 단계 눌린 회색 면으로 알린다. 밝은 화면이 기본이고 나이트 모드는 사용자가 켤 때만 나온다(OS 설정을 따르지 않는다).

이 시스템은 한국어 화면을 전제로 한다. 줄바꿈은 어절 단위(`keep-all`)이고 카드 제목은 두 줄에서 자른다. 모바일 웹도 일급이라 720px 이하에서는 썸네일이 화면 끝까지 차오른다.

**Key Characteristics:**
- 흰 바탕과 중성 회색 면 3단계(눌림 · 호버 · 선). 장식용 색 면이 없다.
- 그린 하나(#10b981, 밤·낮 같음)가 행동을 맡고, 퍼플(#a78bfa)이 보조 버튼·칩·고리를 맡는다. 흰 바탕 위 그린 글자는 #047857. 선택 상태는 그린이 아니라 반전 중성이다.
- 빨강은 LIVE · 알림 숫자 · 오류 신호 전용.
- 평평하다. 그림자는 떠 있는 것(계정 메뉴, 안내 상자)과 세그먼트의 선택 조각뿐이다.
- 알약(`999px`)과 8–16px 둥근 사각형. 사람과 상태는 원.
- 서체는 Pretendard Variable 하나, 굵기로 위계를 만든다.
- 평문 CSS와 CSS 변수. 프레임워크나 유틸리티 클래스에 기대지 않는다.

## Colors

바탕은 순백(#ffffff), 글자는 먹색, 포인트는 그린 하나(보조 퍼플). 밤·낮의 포인트 색이 같다. 모든 값은 `base.css` 의 `:root` 변수에 있고 `:root[data-theme='dark']` 가 같은 이름으로 어두운 값을 덮어쓴다.

### Primary
- **행동 그린 Action Green** (#10b981, `--accent`. 밤·낮 같음): 확정 버튼(`.btn--primary`), 구역 머리의 "더 보기" 링크, 키보드 포커스 링(2px, 간격 2px), 검색창 포커스 테두리, 로고 마크, 글자 선택 색, 스피너의 도는 호. 흰 글자와 5.4:1.
- **밝은 그린** (#34d399, `--accent-hover`): primary 버튼 호버.
- **그린 번짐 Green Wash** (#e6f5ee, `--accent-soft`. 밤에는 #0c2a21) / 호버 (#d2eee1, `--accent-soft-hover`): 톤 버튼 바탕, 검색창 포커스 후광(3px).
- **그린 글자 Green Ink** (#047857, `--accent-ink`. 밤에는 #6ee7b7): 흰 바탕·그린 번짐 위의 글자(5.5:1 이상), 외곽선 버튼 글자, 예정 방송 시각. 밤에는 번짐 위 글자가 밝다.

### Secondary (신호색)
- **라이브 레드 Live Red** (#cf1c3b, `--live` / `--danger`. 밤에는 `--live` #d92d46, `--danger` #ff6b81): LIVE 배지, 아바타의 방송 중 고리, 구독 채널의 "LIVE" 글자, 구역 머리의 방송 수 배지, 켜진 하트, 알림 숫자 배지, 오류. 지금 일어나고 있는 일이나 잘못된 것을 가리킨다. 흰 글자 5.4:1. 밤에 `--danger` 바탕 위 글자는 흰색이 아니라 `--on-danger`(#1a0a0e)다.
- **붉은 번짐** (#fdeaee, `--danger-soft`. 밤에는 #3a1620): 오류 상자, 켜진 큰 하트 바탕.

### Neutral
- **잉크 Ink** (#191f28, `--text`. 밤에는 #f1f1f1): 본문, 제목. 선택된 칩의 바탕이기도 하다.
- **슬레이트 Slate** (#4e5968, `--muted`): 보조 글자. 흰 바탕 7.1:1.
- **옅은 슬레이트** (#5f6b7a, `--subtle`): 조회수, 시각, placeholder. 흰 바탕 5.4:1, 눌린 면 위 4.9:1.
- **흰색 White** (#ffffff, `--bg` / `--surface`. 밤에는 #0f0f0f / #1a1a1a): 페이지 바탕과 올라앉는 면이 같다. 면의 구분은 색 차이가 아니라 선이다.
- **눌린 회색 Sunken Gray** (#f2f4f6, `--sunken`): 기본 버튼, 칩, 썸네일 빈 칸, 현재 메뉴, 빈 상태 상자, 세그먼트 트랙.
- **호버 회색** (#e8ebef, `--hover`): 눌린 면 위에 마우스를 올렸을 때, 스켈레톤 반짝임.
- **머리카락 선 Hairline** (#e5e8eb, `--border`) / **진한 선** (#d1d6db, `--border-strong`): 상단바 아래 선, 메뉴 테두리, 예정 방송 카드 테두리, 검색창 테두리(진한 선), 스크롤바.
- **입력칸 선 Field Line** (#8b95a1, `--field-border`): 입력칸·선택칸 테두리. 흰 바탕과 3:1 이상. 밤에는 #7b8490.

### 사람 색조 (아바타 · 썸네일 빈 칸)
사람(채널)마다 고정된 옅은 색조 8개(`--tint-0..7-bg/fg`): 파랑 · 장미 · 초록 · 호박 · 보라 · 청록 · 분홍 · 회색. 글자는 모두 바탕과 5.7:1 이상이고 밤에는 짙은 바탕에 밝은 글자로 뒤집힌다. 채널 이름을 `tint.js` 가 0–7로 바꿔 `data-tint` 로 준다. 이 색조는 신원을 나눌 뿐 사이트의 포인트 색이 아니다.

### Named Rules
**The Green Is Action Rule.** 그린은 누르거나 따라갈 수 있는 것에만 쓴다. 선택된 세그먼트, 현재 메뉴는 그린이 아니라 중성이다. 그린을 장식이나 강조 면에 쓰지 않는다.
**The Red Means Live Rule.** 빨강은 방송 중이거나 잘못된 것일 때만 쓴다. 강조하고 싶다는 이유로 쓰지 않는다.
**The Light By Default Rule.** 첫 화면은 항상 밝다. `prefers-color-scheme` 을 따르지 않고, 사용자가 켠 `localStorage` 의 선택만 따른다.

## Typography

**Font:** Pretendard Variable (npm `pretendard` 의 dynamic-subset 으로 직접 싣는다. 실패하면 `-apple-system` → `system-ui` → `Apple SD Gothic Neo` → `Malgun Gothic` → `Noto Sans KR` → `sans-serif`)

**Character:** 서체는 한 가지뿐이다. 글자 크기는 고정 척도(12 · 13 · 14 · 15 · 17 · 20 · 24px)이고 화면 폭으로 늘어나지 않는다. 굵기(500/600/700/800)와 -0.01~-0.03em 자간으로 위계를 만든다.

### Hierarchy
- **Display** (700, 24px, 행간 1.3, 자간 -0.02em): 홈 대표 방송의 제목 하나뿐. 720px 이하에서는 20px.
- **Headline** (700, 22px, 행간 1.3, 자간 -0.02em): 페이지 제목(`h1`, `h2` 기본값). 홈 구역 제목(`.shead h2`)은 20px, 모바일 17px.
- **Title** (600, 15px, 행간 1.4, 자간 -0.01em): 카드 제목(두 줄까지), 예정 방송 제목. 소제목 `h3` 기본값은 700 16px.
- **Body** (400, 15px, 행간 1.55): 본문, 버튼 글자(600), 메뉴 항목(500).
- **Label** (500–700, 12–14px): 메타 정보 13px, 조회수·시각 13px, 칩 14px 600, LIVE 배지 12px 700 자간 0.04em, 알림 숫자 11px 700.

로고 글자는 17px 800, 자간 -0.03em. 1100px 미만에서는 로고 글자를 숨기고 마크만 남긴다. 숫자가 흔들리지 않도록 `.num` 이 고정폭 숫자를 쓴다.

### Named Rules
**The Weight Is The Hierarchy Rule.** 위계는 크기보다 굵기와 자간으로 만든다. 본문 15px 위로 올라가는 글자는 제목뿐이다.
**The Keep-All Rule.** 한글은 어절 중간에서 끊지 않는다. `body` 에 `word-break: keep-all; overflow-wrap: break-word` 를 전역으로 두고, 카드 제목처럼 긴 한 덩어리가 올 수 있는 곳만 `overflow-wrap: anywhere` 로 끊는다.

## Layout

**상단바 한 줄 + 본문** 두 겹이다. 사이드바나 독은 없다. 상단바는 높이 56px(+1px 선)으로 위에 붙고(sticky) 흰 바탕이다. 본문은 최대 1140px로 가운데 정렬하고, 목록이 주인공인 화면(홈 · 라이브 · 구독 · 검색)만 1680px까지 넓힌다. 좌우 여백 24px(모바일 16px), 아래 여백 64px.

- **상단바(넓은 화면):** 로고 · 홈/라이브/구독 알약 링크 · 가운데 검색(최대 640px) · 오른쪽에 영상 올리기 · 방송하기 · 알림 · 계정(로그인 전에는 밝게/어둡게 · 로그인).
- **홈:** 칩 줄이 상단바 바로 밑에 붙어 따라온다(sticky, 본문 위 여백 0). 이어서 구독 채널 줄 → 대표 방송 → 지금 방송 중 격자(방송 수 배지) → 곧 시작하는 방송 → 영상 격자(정렬 세그먼트, "더 보기"). 구역 간격 28px(모바일 20px).
- **카드 격자:** `repeat(auto-fill, minmax(min(100%, 300px), 1fr))`, 세로 24px 가로 16px. 예정 방송은 260px 최소폭, 간격 12px.
- **대표 방송:** 1100px 이상에서 영상(최대 840px)과 글을 두 열로, 그 아래에서는 세로로 쌓는다.
- **720px 이하:** 상단바 검색 · 방송하기 · 올리기가 사라지고 검색 아이콘이 생긴다(누르면 상단바 전체가 검색창). 그 아래 44px 높이의 한 줄 탭(홈 · 라이브 · 구독 · 방송하기, 현재 탭은 2px 그린 밑줄)이 붙는다. 영상을 보는 화면 · 올리기 · 방송 · 로그인에서는 탭을 접는다. 카드는 한 줄에 하나, 썸네일은 화면 끝까지 차고(모서리 0) 글에만 안쪽 여백을 준다. 입력칸은 16px(iOS 확대 방지).
- **1100px 미만:** 로고 글자를 숨긴다.
- 그리드 칸에는 `minmax(0, 1fr)` 를 써서 안쪽이 길어져도 옆으로 밀리지 않게 한다. 375px 폭에서 가로 넘침이 없어야 한다.

## Elevation & Depth

**납작하다.** 카드와 썸네일에는 그림자가 없다. 면은 흰 바탕과 눌린 회색 면, 1px 선으로 구분한다. 그림자는 떠 있는 것에만 나온다.

### Shadow Vocabulary
- **팝오버** (`--shadow-pop`: `0 8px 24px rgb(25 31 40 / 0.14), 0 1px 3px rgb(25 31 40 / 0.08)`. 밤에는 `0.5 / 0.4`): 계정 메뉴, 화면 효과 안내 상자.
- **숨결** (`--shadow`: `0 1px 2px rgb(25 31 40 / 0.06)`): 선택된 세그먼트 조각 하나뿐.
- **후광** (`0 0 0 3px var(--accent-soft)`): 검색창 포커스.
- **아바타 고리** (`0 0 0 2px var(--surface), 0 0 0 4px var(--live)`): 방송 중인 채널. 그림자가 아니라 겹 고리다.

### 모션
- 전환은 150ms `--ease-out`(`cubic-bezier(0.23, 1, 0.32, 1)`), 눌림은 120ms `scale(0.97)`(아이콘 버튼 0.92). 메뉴는 `scale(0.97) translateY(-4px)` 에서 150ms 에 나타난다(`@starting-style`).
- 썸네일은 카드에 마우스를 올리면 `scale(1.03)` 으로 300ms. 카드 위 방송 시간 · 최고 시청 띠는 180ms 로 올라온다.
- **들어올 때 한 번 떠오른다(`rise`, 520ms).** 카드·대표 방송·빈 화면은 `translateY(14px)` 와 투명에서 제자리로, 카드는 앞 12장이 55ms 간격으로 이어서 들어온다. 새 화면은 `page-in`(260ms), 아래 구역은 스크롤로 들어오는 순간 `reveal`(`animation-timeline: view()`, 지원하는 브라우저만).
- **호버에서 카드가 3px 뜬다.** 이때만 `--shadow-lift` 그림자가 나오고 가운데에 재생 버튼(`thumb__play`)이 나타난다. 대표 방송 영상은 `scale(1.03)`, 시작 방법 카드는 2px 뜨며 그린 테두리와 화살표 이동.
- **살아 있는 표시.** LIVE 배지의 점은 1.6s 로 깜빡이고(`pulse`), 빈 화면 그림은 재생 버튼 고리·말풍선·하트가 둥둥 뜬다(`float` 5s). 하트를 켜면 `pop` 380ms.
- 스피너는 700ms, 스켈레톤 반짝임은 1.4s. 움직이는 것은 모두 transform·opacity 뿐이다.
- **호버는 마우스가 있는 기기에서만** 건다(`@media (hover: hover) and (pointer: fine)`).
- **동작 줄이기:** `base.css` 가 모든 전환·애니메이션을 사실상 끈다. 스피너만 1.6s 로 느리게 돈다("불러오는 중"이 전달돼야 해서). 화면 효과 '가볍게'(`data-perf='lite'`)는 전환과 스켈레톤·확대를 끈다.

### Named Rules
**The Flat-By-Default Rule.** 카드, 썸네일, 칩, 빈 상태 상자는 가만히 있을 때 그림자가 없다. 그림자는 위에 뜨는 것(메뉴)과, 마우스를 올려 떠오른 카드·시작 방법 카드에만 나온다.

## Shapes

네 모서리가 같은 둥근 사각형과 알약이다. 반지름 변수는 `--radius`(8px) · `--radius-lg`(12px) · `--radius-xl`(16px), 알약은 `999px`.

| 값 | 어디에 |
|---|---|
| `4px` (`--corner-2xs`) | 스켈레톤 글줄, 계기판 |
| `6px` | LIVE · 시청자 배지 |
| `8px` (`--radius`) | 버튼, 건너뛰기 링크 |
| `12px` (`--radius-lg`) | 카드 썸네일, 구독 채널 항목, 예정 방송 카드, 스켈레톤, 안내 상자 |
| `16px` (`--radius-xl`) | 대표 방송 영상, 계정 메뉴, 빈 상태 상자 |
| `999px` | 칩, 세그먼트, 상단바 링크, 검색창, 방송 수 배지, 알림 숫자, 건너뛰기 버튼 |
| `50%` | 아바타, 하트 버튼, 아이콘 버튼, 계정 버튼, 스피너 |

원은 사람과 상태에만 쓴다. 테두리는 1px(상단바 · 메뉴 · 예정 방송 · 검색창)이고, 강조는 포커스 링 2px뿐이다. 모바일(720px 이하)에서는 카드 썸네일과 대표 영상의 모서리를 0으로 하고 화면 끝까지 채운다.

### Named Rules
**The Whole Corner Rule.** 사각형의 네 모서리는 같은 반지름이다. 한쪽만 둥글거나 대각선만 둥근 모서리를 새로 만들지 않는다. 아이콘은 24px 격자에 선 두께 1.75의 선 아이콘(`Icon.jsx`)이고, 이모지나 글자 기호로 아이콘을 대신하지 않는다.

## Components

### Buttons
- **Shape:** 8px 둥근 사각형, 높이 40px, 좌우 16px, 15px 600. 눌리면 `scale(0.97)`.
- **Default** (`.btn`): 눌린 회색 바탕에 잉크 글자, 선 없음. 호버는 호버 회색.
- **Primary** (`.btn--primary`): 그린 바탕, 흰 글자. 호버 깊은 그린. 확정 행동(예: 대표 방송의 "지금 보기")에만.
- **Tonal** (`.btn--tonal`): 그린 번짐 바탕, 그린 글자. 상단바 "방송하기", 빈 상태의 "방송하기"처럼 권하는 행동. 호버 그린 번짐 호버.
- **Outline** (`.btn--outline`): 투명 바탕, 진한 선, 그린 글자. 로그인. 호버는 그린 번짐.
- **Large** (`.btn--lg`): 높이 48px, 좌우 24px, 16px. 모바일의 대표 방송 버튼은 줄을 채운다.
- 아이콘 버튼(`.icon-btn`): 40px 원, 투명, 호버 눌린 회색. 알림 숫자는 빨강 알약 17px.

### Chips
- **Style:** 눌린 회색 바탕, 잉크 글자, 선 없음, 알약, 높이 34px, 좌우 14px, 14px 600. 호버는 호버 회색.
- **State:** `aria-pressed="true"` 면 **바탕이 `--text`(먹색), 글자가 `--bg`(흰색)로 반전**된다. 그린이 아니다("그린은 행동에만"). 호버해도 반전은 유지된다. 칩은 카테고리 필터이고 한 줄로 옆 스크롤한다(스크롤바 숨김). 홈 맨 앞의 "전체"는 필터 해제다.
- **세그먼트** (`.seg`): 눌린 회색 알약 트랙 안에 조각 둘 이상. 선택된 조각은 흰 바탕, 잉크 글자, `--shadow`. 정렬 선택에 쓴다.

### Cards / Containers
- **영상 카드** (`.vcard`): 테두리도 그림자도 없다. 16:9 썸네일(12px 모서리) 아래에 36px 아바타와 글(제목 15px 600 두 줄까지, 채널 13px 슬레이트, 조회·시각 13px 옅은 슬레이트)이 놓인다. 제목 링크의 `::after` 가 카드 전체를 덮어 어디를 눌러도 이동하고, 하트와 채널 링크는 그 위에 있다. 썸네일 위에 왼쪽 위 LIVE 배지, 오른쪽 아래 시청자 수 배지(검은 72% 바탕), 오른쪽 위 하트. 호버에서 방송 시간 · 최고 시청 띠가 아래에서 올라온다.
- **썸네일이 없을 때:** 이름 첫 글자를 40px 700으로 크게, 그 사람의 색조 바탕 위에(`.thumb__blank`) 같은 16:9와 같은 모서리로.
- **대표 방송** (`.feature`): 16px 모서리의 큰 영상, 옆에 채널(아바타 + 이름) · 24px 제목 · 메타 · 설명 3줄 · 행동 줄(큰 primary "지금 보기" + 48px 하트). 영상 위 왼쪽 위 LIVE 배지.
- **예정 방송** (`.sched`): 흰 바탕, 1px 선, 12px 모서리, 안쪽 16px. 시각(그린 글자 13px 600) → 제목(두 줄) → 채널. 호버에서 진한 선과 눌린 회색 바탕. 이 선 상자가 새 체계의 유일한 테두리 카드다.
- **빈 상태** (`.empty-panel`): 눌린 회색 바탕, 16px 모서리, 가운데 정렬, 17px 제목 + 슬레이트 설명(최대 440px) + 다음 행동 버튼. 같은 회색 위에서 기본 버튼은 흰 바탕으로 올린다. 빈 상태는 항상 다음 행동을 안내한다.
- **구독 채널 줄** (`.subs__row`): 84px 폭 항목(아바타 + 이름 13px + 방송 중이면 빨간 "LIVE"). 방송 중인 채널이 앞에 오고 아바타에 빨간 고리.
- **스켈레톤:** 눌린 회색 → 호버 회색의 반짝임. 실제 카드와 같은 크기(16:9, 36px 아바타, 14px 글줄)라 불러온 뒤 자리가 튀지 않는다.

### Inputs / Fields
- **검색창:** 흰 바탕, 1px 진한 선, 알약, 높이 40px, 오른쪽에 60px 눌린 회색 검색 버튼(왼쪽 1px 선). 포커스하면 선이 그린으로 바뀌고 3px 그린 번짐 후광이 생긴다. 안쪽 입력칸은 자체 테두리가 없다.
- **일반 입력칸:** 입력칸 선(`--field-border`)을 쓴다. 모바일에서는 16px.
- **오류:** 오류 상자(붉은 번짐 바탕 + 라이브 레드)를 근처에 `role="alert"` 로 둔다.

### Navigation
- **상단바:** 흰 바탕, 아래 1px 선, 높이 56px, sticky. 알약 링크(높이 36px, 좌우 14px, 슬레이트 15px 600): **현재 메뉴는 색이 아니라 눌린 회색 면과 잉크 글자**로 알린다(`aria-current="page"`). 호버도 같은 면이다.
- **모바일 탭 줄:** 44px 높이의 가로 탭. 현재 탭은 2px 그린 밑줄과 잉크 글자(밑줄만 그린).
- **계정 메뉴:** 40px 원 아바타 버튼 → 280px 폭 흰 팝오버(16px 모서리, `--shadow-pop`). 이름 · 이메일 머리 → 44px 높이 항목들 → 1px 구분선. 모바일에서는 상단바 아래 고정 위치. 항목 포커스는 눌린 회색 바탕에 안쪽 2px 그린 링.
- **아바타:** 원, 기본 36px, 이미지 없으면 첫 글자 + 사람 색조. 방송 중이면 빨간 고리(`.avatar--live`).
- **건너뛰기 링크:** 포커스 때 왼쪽 위에 그린 바탕으로 나타난다.
- **키보드 포커스:** 전역 `:focus-visible` 은 2px 그린 링, 간격 2px.

### Badges (LIVE · 시청자 · 알림)
- **LIVE 배지:** 라이브 레드 바탕, 흰 글자, 12px 700, 자간 0.04em, 높이 22px, 6px 모서리. 썸네일의 왼쪽 위 8px.
- **시청자 배지:** 검은 72% 바탕, 흰 글자. 썸네일의 오른쪽 아래.
- **방송 수 배지:** 구역 제목 옆 빨강 알약. **알림 숫자:** 아이콘 위 빨강 알약.

### Heart Button
36px 원, 썸네일 위에서는 검은 55% 바탕에 흰 아이콘, 켜지면 라이브 레드 바탕. 대표 방송의 큰 것(`--solid`, 48px)은 눌린 회색 바탕에 잉크 아이콘, 켜지면 붉은 번짐 바탕에 빨간 아이콘.

## Do's and Don'ts

### Do:
- **Do** 색은 `base.css` 의 변수(`var(--accent)`, `var(--surface)`, `var(--sunken)` 등)로만 쓴다. 새 색은 라이트와 `:root[data-theme='dark']` 두 벌을 함께 만든다.
- **Do** 선택된 상태(칩, 현재 메뉴, 세그먼트)는 중성 반전이나 눌린 회색 면으로 알린다.
- **Do** 확정 행동만 `.btn--primary`, 권하는 행동은 `.btn--tonal`, 나머지는 기본 `.btn`.
- **Do** 새 반지름은 `--radius` · `--radius-lg` · `--radius-xl` · `999px` 중에서 고른다.
- **Do** 위험색(`--danger`) 바탕의 글자는 `var(--on-danger)` 를 쓴다.
- **Do** 카드 제목은 `-webkit-line-clamp: 2` 와 `overflow-wrap: anywhere`, 한국어는 `keep-all`.
- **Do** 이미지가 없는 썸네일 빈 칸도 디자인한다(사람 색조 바탕 + 첫 글자, 같은 16:9와 모서리).
- **Do** 호버는 `@media (hover: hover) and (pointer: fine)` 안에 둔다. 새 전환·애니메이션은 150–250ms `--ease-out` 으로 하고, 화면 효과를 더하면 `:root[data-perf='lite']` 에도 끄는 규칙을 넣는다.
- **Do** 빈 상태에는 이유와 다음 행동 버튼을 둔다. 한계(서버가 잠듦, 지원하지 않는 브라우저)는 화면이 이유와 해결법을 말한다.
- **Do** 새 카드나 목록은 375px 폭에서 가로로 넘치지 않는지 확인한다. 아이콘은 `Icon.jsx` 의 선 아이콘만 쓴다.

### Don't:
- **Don't** 그린을 장식이나 선택 상태에 쓰지 않는다(예외: 모바일 탭의 현재 탭 밑줄, 검색창 포커스).
- **Don't** 빨강을 장식이나 강조에 쓰지 않는다. 방송 중이거나 오류일 때만.
- **Don't** 카드, 썸네일, 칩에 그림자나 테두리를 더하지 않는다(예정 방송 카드의 1px 선은 있는 그대로).
- **Don't** OS 다크 모드를 따라가지 않는다. 나이트 모드는 사용자가 켠다.
- **Don't** 서체를 더하지 않는다. Pretendard 한 가지의 굵기로 위계를 만든다.
- **Don't** Tailwind, 유틸리티 클래스, CSS-in-JS 를 들이지 않는다.
- **Don't** 이모지나 글자 기호를 아이콘으로 쓰지 않는다.
- **Don't** 방송 · 영상 · 시청자 수 · 후기를 지어내지 않는다(운영 사이트는 콘텐츠가 비어 있다).

## 아직 새 체계로 옮기지 않은 화면

새 체계는 상단바(`Layout.jsx`)와 홈(`HomePage.jsx` · `MediaCards.jsx`)까지다. 아래 화면은 `app.css` 의 옛 규칙이 일부 남아 있고, 화면별 규칙이 `base.css` 끝의 호환 변수(`--corner-2xs/xs/pill/sm/md/lg`, 각각 4 · 8 · 999 · 12 · 16 · 20px)를 통해 새 반지름을 받는다. 이 화면들을 고칠 때는 위 규칙으로 옮기고, 옛 이름이나 옛 모양(대각선 모서리, 에메랄드, 유리)을 새 화면에 가져가지 않는다.

- 채널 · 라이브 시청 · 영상 시청(VOD) · 라이브 목록 · 구독 · 검색 · 알림
- 올리기 · 수정 · 브라우저 방송하기(`/ingest`) · 로그인 · 회원가입 · 계정
- 관리자(`admin.css` 의 성공 · 경고 색 포함)
- 라이브 채팅과 후원 칩(등급 색 포함)
- 플레이어 상자(`HlsPlayer`)의 안내 문구와 배지 일부

이 화면들의 기존 동작 규칙(인라인 확인, 로그인 후 돌아오기, 업로드 진행률 등)은 시각 체계와 별개로 유지된다. 옮기는 시점에 이 문서에 해당 구성 요소를 더한다.

## 시네마 무대와 카드 격자

홈은 위아래 두 부분이다(`CinemaHome.jsx`, `cinema.css`).

- **위: 어두운 시네마 무대.** 가로로 넓은 슬라이더(화면의 약 3/4)와 오른쪽 "곧 시작하는 방송" 패널. 화면이 밝아도 이 구역은 나이트 팔레트로 그린다(`base.css` 의 `.night-scope` 가 같은 변수 이름으로 어두운 값을 덮어쓴다). 방송이 있으면 방송을, 없으면 가장 많이 본 영상을 올리고, 7초마다 넘어가며 마우스·포커스에서 멈춘다(움직임 줄이기에서는 저절로 넘기지 않는다). 방송이 없을 때는 위에 어두운 그린 안내 띠가 붙는다.
- **아래: 칩 줄 + 영상 카드 격자.** 흰 바탕(나이트 모드에서는 어두운 바탕)에 YouTube 구조의 카드(`vcard`, 썸네일 + 아바타·제목·채널·조회수)와 카테고리 칩 줄이다. 선택된 칩은 반전 중성이다. 슬라이더에 다 못 오른 방송은 "다른 방송" 카드로 이 아래에 놓인다.
- 라이브·구독·검색·채널의 목록도 같은 카드 격자를 쓴다. 채널 머리·영상 틀·로그인 카드 등은 `skin.css` 가 둥근 면과 옅은 선 테두리를 입힌다.
- 색은 밤·낮이 같다: 그린 `#10b981`(면)·퍼플 `#a78bfa`(보조). 흰 바탕 위의 그린 글자는 대비가 모자라서 `--accent-ink`(#047857)를 쓴다.
- 서버에 없는 값(실시간 채팅, 지어낸 시청자 수 등)은 넣지 않는다. 불러오는 동안은 같은 모양의 회색 뼈대가 맥동한다.

## 시청 화면과 폼 화면

- **영상 아래쪽**(`watch.css`): 제목(24px 800) → 통계 한 줄 → 채널 줄(얼굴 | 이름·구독자 | 구독 버튼, 1px 선의 12px 카드) → 반응 줄(좋아요 알약 | 신고·수정·삭제는 테두리만 있는 가벼운 단추) → 설명(눌린 회색 카드, "더보기"는 글자 단추) → 댓글. 칸 사이 간격은 12~16px, 모서리는 12px로 맞춘다.
- **댓글:** 얼굴(36px) + 이름·시각 + 본문 + 글자 단추(답글·삭제). 입력은 알약 칸 하나와 그린 "등록". 답글은 얼굴 폭(48px)만큼 들여 원 댓글에 묶고 세로줄은 쓰지 않는다. 댓글이 없으면 한 줄 안내만 둔다.
- **폼·설정 화면**(`skin.css`): 페이지 제목은 24px 800으로 같다. 내 계정의 접었다 펴는 칸은 한 장의 카드(오른쪽 화살표), 알림은 한 건이 한 줄 카드(안 읽은 건 옅은 그린 면), 대기실의 남은 시간은 어두운 시네마 면이다.

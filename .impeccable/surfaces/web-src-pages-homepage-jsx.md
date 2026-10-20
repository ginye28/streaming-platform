---
version: 1
slug: "web-src-pages-homepage-jsx"
primary_target: "web/src/pages/HomePage.jsx"
related_targets: ["web/src/components/Layout.jsx"]
---

# 홈 화면과 전체 쉘 (Operate)

## 범위와 방문자
Operate. 시청자가 지금 볼 것을 찾아 한 번에 재생한다. 스트리머는 방송하기·올리기가 한 번에 닿는다. 사용자가 확정한 것: 기준은 YouTube, 포인트 파랑, 상단바 구조, "더 깔끔하고 대기업 서비스 같은" 완성도, 기존 시각 체계는 전부 교체해도 됨. 라이트가 기본, 나이트는 사용자가 켤 때만.

## Direction contract

THESIS: 홈은 "지금 볼 것"에 답한다. 유튜브급으로 익숙한 구조(상단바·칩·16:9 썸네일 그리드)를 군더더기 없이 정밀하게. 거절하는 것: 유리·블러 장식, 나뭇잎 모서리, 풀블리드 히어로, 가로 캐러셀, 카드 속 카드.

OWN-WORLD: 흰 바탕, 먹색 #191f28 글자, 차가운 중성 회색(칩 #f2f4f6, 선 #e5e8eb). 파랑 #1b64da 는 행동·선택·링크에만, LIVE 빨강은 방송 중 배지에만. 서체 Pretendard Variable(자체 호스팅). 반지름 8/12/16, 그림자는 메뉴·팝오버에만. 아바타는 원형이고 사람마다 고정된 옅은 색조. 썸네일은 16:9 반지름 12, 없으면 사람의 첫 글자.

STORY: 3초 안에 지금 방송 중인 것이 눈에 들어오고 한 번 눌러 재생한다. 아래로 내리면 영상이 훑기 쉬운 그리드로 이어진다. 로그인하면 구독 채널이 방송 중인지 한눈에 보인다. 비어 있으면 이유와 다음 행동을 알려 준다.

FIRST VIEWPORT: 1440×900 에서 56px 상단바(로고, 홈·라이브·구독, 가운데 검색, 방송하기, 올리기, 알림, 계정) → 52px 칩 줄(전체+카테고리, 오른쪽 정렬) → 라이브 구역: 왼쪽 16:9 대형 라이브(약 60%), 오른쪽 채널·제목·시청자·경과 시간·"시청하기" 기본 버튼. 그 아래 다른 방송 줄과 영상 그리드 첫 줄 제목까지 보인다. 주 행동은 대형 라이브의 시청하기. 390px 에서는 컴팩트 상단바 + 탭 줄 + 칩 줄, 카드는 1열 전폭.

FORM: 사용자가 정한 표준형(YouTube급)을 그대로 충실히. 추첨은 하지 않음(브리프로 고정). seed: none (unavailable)

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance

# 인수인계 (다른 PC·새 세션에서 이어서 하기)

작성: 2026-10-06. 저장소에 들어 있어서 `git pull` 만 하면 따라온다. 루트의 `CLAUDE.md` 가 이 파일을 먼저 읽게 한다.
**비밀 값(토큰·키·비밀번호)은 이 파일에 적지 않는다.** 공개 저장소다.

## 1. 프로젝트 한 줄

유튜브형, 라이브 중심 스트리밍 플랫폼(SP). 무료 배포: 웹 Vercel · API Render(Docker) · DB TiDB Cloud Starter.

| | 주소 |
|---|---|
| 웹 | https://sp-web.vercel.app |
| API | https://sp-api-5mal.onrender.com (무료라 잠들면 첫 요청이 30~85초) |
| 저장소 | https://github.com/ginye28/streaming-platform |

## 2. 지금까지 끝난 것

| PR | 내용 |
|---|---|
| #35 | 후원(슈퍼챗) · 채팅 운영 · 다시보기+채팅 리플레이 · 방송 예약/대기실 · 멤버십 전용 방송 |
| #36 | TiDB 에서 결제 승인이 실패하던 행 잠금 쿼리(`for update of p1_0`)를 네이티브 SQL 로 수정 |
| #37 | 송출 콜백 보호(`RTMP_CALLBACK_TOKEN`), README 낡은 안내 정리, `docs/plan.md` 추가, 홈 빈 상태 링크 |
| #38 | 예정 방송 링크가 오류 화면으로 가던 문제 수정 |
| #39 | 운영 사이트 라이브(내 PC 스트리밍 서버 + 임시 터널, `scripts/start-live.ps1`) |
| #40 | 라이브 스크립트: 다시보기 녹화 권한 수정, 송출·시청 포트를 이 PC 에서만 열기 |
| #41 | **브라우저로 바로 방송하기** — OBS·내 PC 없이 웹의 "방송하기"로 화면 공유/카메라 방송 (아래 A-1) |
| #42 | 브라우저 방송에서 소리가 안 들어갈 때 이유를 화면에 알려 줌 (아래 A-1) |

> 모두 main 에 병합됨(2026-10-06 확인). `git log --oneline -6` 에 #42 가 보이면 최신이다.

검증한 것: 백엔드 테스트 전체 통과(269+α), 로컬 실제 RTMP/OBS 송출→HLS 시청→다시보기, 토스 **테스트 키** 결제창 열림, 모바일 폭(375px)에서 가로 넘침 없음, nginx-rtmp 가 콜백에 `token`·`hls` 인자를 그대로 전달하는 것까지.

## 3. 아직 남은 것 (우선순위 순)

### A. 운영 사이트 라이브

**A-1. 브라우저로 방송하기 (PC·Docker 불필요, 누구나)** ← 우선 이쪽
- 사이트에 로그인 → 상단 **방송하기** → 화면 공유/카메라 선택 → **방송 시작**. Chrome·Edge 필요(H.264 녹화).
- 원리: 브라우저가 WebSocket(`/ingest`)으로 영상 조각을 보내면, Render 의 API 안 ffmpeg 가 **다시 인코딩하지 않고 HLS 로 포장만** 바꿔 API 가 `/live-hls/` 로 내려 준다. 재생 이름이 `b-` 로 시작하면 브라우저 방송.
- Render 설정은 따로 필요 없음(`RENDER_EXTERNAL_URL` 자동 사용). 끄려면 `BROWSER_INGEST_ENABLED=false`.
- 한계: 다시보기 없음 · 동시 방송 1개(메모리 512MB, `INGEST_MAX_SESSIONS`) · 시청자 적은 시연용 · 탭을 닫으면 방송 종료 · 시청 지연 약 10초.
- 검증: 실제 ffmpeg+Chromium 으로 방송→HLS(H.264 720p+AAC)→시청 화면 재생→종료→파일 정리, Render 와 같은 제한(힙 300MB, 컨테이너 512MB)에서 76초 방송 유지 확인.
- **운영 확인(2026-10-06)**: 실제 사이트에서 방송이 열리고(목록 `LIVE`), 영상 H.264 1080p 가 `/live-hls/` 로 나가고, 시청자 2명이 붙고, 조각 다운로드 속도도 충분했다. Render 로그는 정상.
- **소리는 안 들어갔었다**(영상만 감). 서버가 버린 게 아니라 브라우저가 소리 트랙 없이 보낸 것(ffmpeg 로그 `b:a ... not been used for any stream`). 화면 공유에는 소리가 안 들어가는 경우가 많고, 마이크 허용을 거부해도 조용히 넘어갔다. #42 로 화면에 이유와 해결법을 띄우게 했다.
- **아직 못 한 것**: ① "마이크 소리 포함"을 켜고 마이크를 허용해서 **소리가 실제로 서버까지 가는지** 확인(ffprobe 로 최신 조각에 aac 가 있는지: `ffprobe https://sp-api-5mal.onrender.com/live-hls/<재생이름>-N.ts`). ② 시청 화면에서 영상이 끊김 없이 재생되는지 **사람 눈으로** 확인(AI 의 브라우저 탭은 백그라운드라 자동재생이 안 돼서 못 봄).

**A-2. OBS 로 방송 (내 PC 가 스트리밍 서버)**

원리: Render 는 RTMP 를 못 받으므로 **내 PC 가 스트리밍 서버**, 시청용 HLS 포트(8081)만 Cloudflare 임시 터널로 공개. 터널 주소는 켤 때마다 바뀌지만 방송을 시작할 때 스트리밍 서버가 `hls=` 로 API 에 알려 주므로 Render 설정을 바꿀 필요가 없다. 자세한 설명은 `docs/06-deployment.md` 의 "운영 사이트에서 라이브 켜기".

1. **Render 환경변수** `RTMP_CALLBACK_TOKEN`(저장 완료) · `VOD_ENABLED=true`(저장 완료). ⚠ 토큰 값은 비밀이라 여기 적지 않음 — 노트북에서 스크립트를 돌릴 때는 Render 에 넣은 **같은 값**이 필요(모르면 Render → `sp-api` → Environment 에서 눈 아이콘으로 보거나 새 값으로 바꿔 둘 다 갱신).
2. 노트북 준비물: **Docker Desktop**(실행 중), **cloudflared**(`winget install Cloudflare.cloudflared`)
3. 방송할 때마다
   ```powershell
   cd <저장소 폴더>
   .\scripts\start-live.ps1 -Token <Render 에 넣은 값>
   ```
   끝나면 `.\scripts\stop-live.ps1`
4. OBS 설정: 서버 `rtmp://localhost:1935/live`, 스트림 키는 사이트 로그인 → **내 계정 → 스트림 키**. (OBS 는 스크립트를 돌린 **같은 노트북**에서 켜야 합니다.)
5. 확인: 홈의 "Live 중인 채널"에 뜨는지, 시청 화면에서 영상이 나오는지.

이 방식으로 **운영(Render+터널) 실제 방송은 확인함**(2026-10-06): 목록·재생·채팅 WebSocket 정상. Render 가 재시작하면 API 가 기억한 터널 주소가 지워져서 **방송을 다시 시작**해야 복구된다.

### B. 토스 결제 운영 재확인
- PR #36 이후 운영에서 결제 승인이 되는지 **아직 확인 못 함**.
- 아까 실패한 결제 화면(`?view=pay-result&…`)을 새로고침하거나, 유료 구독 → 카드 결제를 다시 시도. 테스트 키(`test_ck_` / `test_sk_`)라 **실제 청구 없음**. 성공하면 "결제가 완료되었습니다"와 영수증 링크가 뜨고, 내 계정의 결제 내역에 DONE 으로 보여야 함.
- 키는 반드시 "API 개별 연동 키"(결제위젯 키 `gck/gsk` 는 안 됨). 이미 Render 에 들어 있음.

### C. 과제 제출 (ALEPH Studio)
- `docs/plan.md` 를 **plan.md 선택 → 새 버전 저장**으로 업로드(주제 변경 사유 포함).
- 프로젝트 목표 4칸을 SP 내용으로 교체(문구는 `docs/plan.md` 의 1~4절 참고) → **목표 저장**.
- plan.md 의 "완성의 기준" 중 미완 항목(운영 결제 승인 확인, 문서 현행화)은 끝나는 대로 체크 표시 갱신.
- **plan.md 는 브라우저 방송(#41·#42)이 들어오기 전에 쓴 글이다.** 제출 전에 "핵심 기능"·"알려진 한계"·"진행 현황"에 브라우저 방송(PC 없이 운영 사이트에서 방송, 다시보기 없음, 무료 서버 한계)을 반영할 것.

### D. 사이트가 비어 있음
- 운영에 방송·영상이 하나도 없어 처음 보면 미완성처럼 보임. 직접 가입해서 영상 1~2개 올리거나, A 가 되면 짧게 방송을 켜 두기. (계정 생성은 AI 가 대신 못 함)

### E. 그 밖의 알려진 한계
- 옛 PC 에만 있던 로컬 브랜치 `feat/intro-autoplay`(인트로 자동재생 변경, 커밋 1개)는 GitHub 에 안 올라갔다. 필요하면 원래 PC 에서 `git push -u origin feat/intro-autoplay`.
- 슬로우 모드·시청자 수·채팅 브로커는 서버 메모리라 서버 여러 대로 못 늘림(Redis 필요).
- 운영 DB(TiDB)의 `flyway_schema_history` 가 `5:1` 인지 직접 본 적 없음(앱이 정상 동작하는 것으로 간접 확인).
- 토스 웹훅 등록(`TOSS_WEBHOOK_TOKEN`)은 선택 사항이라 안 함. 환불·웹훅 경로는 운영에서 실제로 돌려 보지 않음.

## 4. 노트북 환경 세팅 체크리스트

```powershell
git clone https://github.com/ginye28/streaming-platform.git   # 이미 있으면 git pull
cd streaming-platform
git checkout main
git pull
```

- 라이브만 할 거면 필요한 것: Git, Docker Desktop, cloudflared, OBS.
- 코드를 고치려면 추가로: JDK 21, Node 20+.
  - 백엔드 테스트: `cd api; .\gradlew.bat test` (Docker 로 돌리는 방법도 있음 — JDK 가 25 처럼 너무 높으면 21 로 맞추세요)
  - 웹: `cd web; npm ci; npm run lint; npm run build`
- 로그인이 필요한 곳(전부 직접 로그인): GitHub, Render, Vercel, TiDB Cloud, 토스 개발자센터, ALEPH Studio.
- Claude Code 사용 시: "Claude in Chrome" 확장이 연결돼야 GitHub PR 생성·병합을 AI 가 대신할 수 있음(안 되면 PR 은 직접). GitHub CLI(`gh`)는 설치돼 있지 않음.
  - PR 흐름: `git push -u origin <브랜치>` → `https://github.com/ginye28/streaming-platform/pull/new/<브랜치>` → 제목·설명 확인 → **Create pull request** → CI 7개 통과 확인 → **Merge pull request** → **Confirm merge**.
  - 버튼 클릭이 한 번에 안 먹히면 좌표로 누르고, 탭이 멈추면 닫고 새 탭을 연다. CI 상태는 `https://api.github.com/repos/ginye28/streaming-platform/commits/<sha>/check-runs` 로 확인할 수 있다(공개 저장소).
  - 비밀 값 입력, 로그인, 계정 생성, 결제는 AI 가 대신 하지 않는다. 사람이 직접.

## 5. 알아 두면 좋은 설계 메모

- 방송마다 재생 이름(`stream_name`)이 새 UUID. 멤버십 전용 방송의 주소는 볼 자격이 있는 사람에게만 내려감.
- 후원은 `payments.kind=DONATION` 으로 구독과 같은 결제 흐름. 사용자 환불 불가, 관리자만.
- 방송 예약 시각은 `OffsetDateTime`(서버가 UTC 라서), 알림 문구는 Asia/Seoul.
- 새 enum 은 DB ENUM 이 아니라 varchar. Jackson 3 에서 record 의 primitive boolean 이 빠지면 400 → `Boolean` 사용.
- TiDB 는 Hibernate 의 `for update of <alias>` 를 못 알아들음 → 행 잠금은 네이티브 `select … for update`.
- 콜백(`/api/internal/rtmp/**`)은 `RTMP_CALLBACK_TOKEN` 이 비어 있으면 검사하지 않음(로컬 개발용). 공개 서버에서는 반드시 채울 것.
- PowerShell 5.1 은 BOM 없는 한글 `.ps1` 을 잘못 읽음 → `scripts/*.ps1` 은 UTF-8 BOM 으로 저장돼 있으니 편집기에서 BOM 을 지우지 마세요.

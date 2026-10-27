# 배포

컨테이너 세 개(`api` · `web` · `streaming`)와 MySQL 로 돕니다.
루트 `docker-compose.yml` 의 `full` 프로필이 그 전부를 한 번에 띄웁니다.

---

## 필요한 것

| | |
|---|---|
| Docker | 이미지 빌드·실행 |
| Java 21 · Node.js 20+ | 컨테이너 없이 직접 돌릴 때만 |

빌드는 컨테이너 안에서 하므로, 배포하는 기계에는 Docker 만 있으면 됩니다.

---

## 한 번에 띄우기

```bash
echo "JWT_SECRET=$(openssl rand -hex 32)" > .env
docker compose --profile full up -d --build
```

| 주소 | 무엇 |
|---|---|
| http://localhost:3000 | 화면 (nginx 가 정적 파일 서빙) |
| http://localhost:8080 | API |
| http://localhost:8081/hls | HLS 재생 |
| `rtmp://localhost:1935/live` | OBS 송출 |

`JWT_SECRET` 은 기본값이 없습니다. 안 넣으면 API 가
`jwt.secret 은 최소 32바이트여야 합니다` 로 멈춥니다. 일부러 그렇게 뒀습니다.

평소 개발할 때 쓰는 `docker compose up -d` 는 그대로 MySQL 과 스트리밍 서버만 띄웁니다.
`api` · `web` 은 `full` 프로필에만 들어 있습니다.

---

## 이미지 구성

### `api/Dockerfile`

```
eclipse-temurin:21-jdk  →  ./gradlew bootJar -x test
eclipse-temurin:21-jre  →  java -jar app.jar
```

- 의존성 목록(`gradle/`, `build.gradle`)을 소스보다 먼저 넣어, 소스만 바뀌면 다시 내려받지 않습니다.
- 테스트는 CI 에서 이미 돌았으므로 이미지 빌드에서는 건너뜁니다.
- 실행 이미지에 **ffmpeg** 을 넣습니다. 영상 썸네일을 뽑는 데 씁니다.
  빼면 업로드는 되지만 썸네일이 안 붙습니다.
- 루트가 아닌 `app` 사용자로 돕니다.
- 업로드 파일은 `/app/uploads` 에 쌓이고, compose 가 `api-uploads` 볼륨을 붙여 둡니다.
- **Render 무료 서버는 디스크가 임시**라서 재시작·재배포·잠들었다 깰 때 `/app/uploads` 가 비워집니다.
  그래서 업로드한 파일은 DB(`stored_files`, `stored_file_chunks`, 1MB 조각)에도 담아 두고,
  `/uploads/{이름}` 요청 때 디스크에 없으면 DB 에서 되살립니다(`UploadRestoreFilter`).
  파일 한 개 최대 100MB 이고, TiDB 무료 저장 공간(5GiB)을 같이 씁니다. 이 기능이 들어오기 전에 올린 파일은 복구할 수 없습니다.
  `docker compose down` 으로는 지워지지 않습니다. 지우려면 `down -v`.

### `web/Dockerfile`

```
node:22-alpine  →  npm ci && npm run build
nginx:1.27-alpine  →  dist/ 를 :80 으로
```

- `web/nginx.conf` 가 SPA 새로고침을 처리합니다(`try_files ... /index.html`).
- `/assets/` 는 파일 이름에 해시가 붙으므로 1년 캐시, `index.html` 은 `no-cache`.

### `streaming/Dockerfile`

`tiangolo/nginx-rtmp` 에 `streaming/nginx.conf` 를 얹습니다.
송출 입구(1935)와 HLS 를 만드는 안쪽 통로(1936)가 나뉘어 있습니다 —
이유는 [03-api-spec.md](03-api-spec.md) 참고.

---

## 설정

값을 바꾸는 곳은 `docker-compose.yml` 의 `api.environment`,
그 값을 읽는 곳은 `api/src/main/resources/application-docker.yaml` 입니다.
이미지 안에는 비밀을 넣지 않습니다.

| 환경변수 | 기본값 | 설명 |
|---|---|---|
| `JWT_SECRET` | 없음 (필수) | HS256 서명 키. 32바이트 이상 |
| `JWT_EXPIRATION` | `3600000` | 액세스 토큰 만료(ms) |
| `JWT_REFRESH_EXPIRATION` | `1209600000` | 리프레시 토큰 만료(ms) |
| `DB_URL` | `jdbc:mysql://mysql:3306/streaming?...` | 컨테이너끼리는 서비스 이름으로 찾아갑니다 |
| `DB_USERNAME` · `DB_PASSWORD` | `streaming` | |
| `DDL_AUTO` | `update` | |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | 화면 주소 |
| `HLS_BASE_URL` | `http://localhost:8081/hls` | **시청자 브라우저가 직접 여는 주소** |
| `ADMIN_EMAILS` | 비어 있음 | 기동 시 ADMIN 을 줄 계정 (쉼표 구분) |
| `TOSS_CLIENT_KEY` | 비어 있음 | 토스페이먼츠 **API 개별 연동 키**의 클라이언트 키(`test_ck_…` / `live_ck_…`). 브라우저에 내려가도 되는 값 |
| `TOSS_SECRET_KEY` | 비어 있음 | 토스페이먼츠 시크릿 키(`test_sk_…` / `live_sk_…`). **서버에만 둔다.** 둘 다 있어야 결제가 켜진다 |
| `MEMBERSHIP_PRICE_KRW` | `4900` | 유료 구독 한 번의 금액(원) |
| `MEMBERSHIP_PERIOD_DAYS` | `30` | 유료 구독 한 달(1개월)의 기간(일). 여러 달 상품은 이 값에 달 수를 곱한다 |
| `MEMBERSHIP_PLAN_MONTHS` | `1,3,6,12` | 고를 수 있는 구독 기간(달). 처음 값이 기본 상품이다 |
| `MEMBERSHIP_PLAN_DISCOUNTS` | `0,5,10,15` | 위와 같은 순서의 할인율(%). 오래 구독할수록 한 달 값이 싸진다 |
| `MEMBERSHIP_REFUND_WINDOW_DAYS` | `7` | 사용자가 직접 환불할 수 있는 기간(일). 관리자는 기간과 상관없이 환불한다 |
| `MEMBERSHIP_PLATFORM_FEE_PERCENT` | `0` | 채널 주인 수익 장부에서 떼는 서비스 수수료율(%). 장부일 뿐 송금은 하지 않는다 |
| `MEMBERSHIP_EXPIRY_NOTICE_DAYS` | `3` | 유료 구독이 끝나기 며칠 전부터 알릴지 |
| `TOSS_WEBHOOK_TOKEN` | 비어 있음 | 토스 웹훅 주소에 `?token=` 으로 붙이는 비밀 값. 비워 두면 검사하지 않는다 |
| `DONATION_AMOUNTS` | `1000,3000,5000,10000,30000,50000` | 방송 후원으로 고를 수 있는 금액(원, 쉼표 구분). 이 목록에 없는 금액은 주문을 만들어 주지 않는다 |
| `VOD_ENABLED` | `false` (compose 는 `true`) | 스트리밍 서버가 방송을 녹화하는지. 켜면 방송이 끝날 때 다시보기 주소를 내려 준다. **녹화하지 않는 서버에서 켜면 없는 주소를 가리킨다** |
| `VOD_BASE_URL` | 비어 있음 | 다시보기 재생 주소의 앞부분. 비우면 `HLS_BASE_URL` 옆의 `/vod` 를 쓴다 |
| `RTMP_CALLBACK_TOKEN` | 비어 있음 | nginx 가 송출 시작·종료 콜백 주소에 `?token=` 으로 붙이는 비밀 값. **스트리밍 서버와 API 에 같은 값**을 넣는다. 비워 두면 검사하지 않으므로 콜백이 바깥에 열린 서버에서는 반드시 채운다(영문·숫자만 쓸 것). 틀리면 403 |
| `RATE_LIMIT_ENABLED` | `true` | 요청 속도 제한 전체(로그인 IP 한도 · 계정 잠금 · 채팅 도배 · 아래 일반 한도)를 켤지. 끄면 아무것도 막지 않는다(부하 시험·로컬 개발용) |
| `RATE_LIMIT_WRITE_PER_MINUTE` · `RATE_LIMIT_READ_PER_MINUTE` | `300` · `1500` | 변경 요청 · 조회, IP 하나당 분당 |
| `RATE_LIMIT_UPLOAD_PER_MINUTE` · `RATE_LIMIT_CONNECT_PER_MINUTE` | `20` · `300` | 파일 업로드 · WebSocket 연결 |

로그인·가입·토큰 갱신(10분에 60·20·120), 계정별 로그인 실패(15분에 10번), 채팅 도배(10초에 10)는 [09-security.md](09-security.md) 의 고정 값이다.

> 학원·학교·사무실처럼 많은 사람이 한 IP 로 나가면 IP 한도에 같이 걸릴 수 있다. 막힌다는 제보가 오면 관리자 화면 **보안** 탭에서 `RATE_LIMITED` 기록을 보고 해당 한도만 올린다.

### 비밀 값은 `.env` 에

`docker-compose.yml` 옆의 `.env` 를 compose 가 자동으로 읽는다. [`.env.example`](../.env.example) 을 복사해 값을 채운다. `.env` 는 `.gitignore` 에 들어 있어 커밋되지 않는다.

```bash
cp .env.example .env     # JWT_SECRET 에 32바이트 넘는 아무 문자열
docker compose --profile full up -d --build
docker compose --profile full ps      # api · web 이 healthy 가 되면 준비 끝
```

- `api` 에는 상태 확인(healthcheck)이 있고, `web` 은 `api` 가 healthy 가 된 뒤에 뜬다. 화면만 먼저 떠서 "서버에 연결하지 못했어요" 가 보이는 일이 없다.
- 컨테이너로 묶는 이유: ffmpeg(썸네일·브라우저 방송)·Java·MySQL 버전을 PC 마다 맞출 필요가 없고, OS(Windows·macOS·Linux)가 달라도 같은 이미지가 돈다.

### 보안 헤더

| 어디 | 무엇 | 파일 |
|---|---|---|
| 운영 웹(Vercel) | CSP · `nosniff` · Referrer-Policy · Permissions-Policy, `/assets` 장기 캐시 | `web/vercel.json` |
| Docker 웹(nginx) | 같은 헤더(로컬 주소 http 허용) | `web/security-headers.conf` |
| API | CSP(`default-src 'none'`) · `nosniff` · Referrer-Policy · Permissions-Policy | `SecurityHeadersFilter` |

CSP 에 외부 주소가 들어 있는 곳: 결제(`*.tosspayments.com`, `*.toss.im`), 폰트(Google Fonts). **새 외부 스크립트·서비스를 붙이면 CSP 에 그 주소를 더해야** 브라우저가 막지 않는다. 막히면 개발자 도구 콘솔에 `Content Security Policy` 위반 메시지가 나온다.

### 결제(토스페이먼츠) 켜기

토스 개발자센터의 **"API 개별 연동 키"**(결제위젯 연동 키가 아니다. `test_ck_…` / `test_sk_…`)를 받아 `TOSS_CLIENT_KEY` · `TOSS_SECRET_KEY` 두 환경변수로 넣으면
결제가 켜집니다. 처음에는 **테스트 키**(`test_…`)로 시작하세요. 테스트 키로는 실제로 돈이 빠져나가지 않습니다.
하나라도 비어 있으면 결제는 꺼지고, 유료 구독은 결제 없는 자리표시로 남습니다.

- 시크릿 키는 이미지·코드·저장소에 넣지 않습니다. 환경변수로만 줍니다.
- 결제창이 돌아오는 주소는 웹이 열려 있는 주소(`/?view=pay-result`)이므로 따로 등록할 것은 없습니다.
- 실제 결제(`live_…` 키)는 사업자 등록과 토스 가맹 심사를 거쳐야 쓸 수 있습니다.
- **웹훅(권장)** — 토스 개발자센터에서 웹훅 주소를 `https://{API 주소}/api/payments/webhook?token={TOSS_WEBHOOK_TOKEN}` 으로 등록하고
  `PAYMENT_STATUS_CHANGED` 이벤트를 켭니다. 결제창에서 돌아오기 전에 연결이 끊긴 결제와, 토스 상점관리자에서 직접 한 취소를 자동으로 맞춥니다.
  토큰은 아무 긴 문자열이면 되고, 환경변수와 웹훅 주소에 같은 값을 넣습니다.
- 유료 만료 임박 알림은 서버가 한 시간마다, 방송 예약의 "곧 시작" 알림은 1분마다 확인합니다. 무료 호스팅에서 서버가 잠들어 있으면 확인도 쉽니다.
- **방송 후원도 같은 키로 켜집니다.** 별도 설정이 없고, 후원 금액만 `DONATION_AMOUNTS` 로 바꿉니다.

### 다시보기 녹화 (스트리밍 서버)

`streaming/nginx.conf` 가 방송을 두 갈래로 흘립니다. `hls` 는 실시간 재생용(2초 조각, 10초만 유지. OBS 의 키프레임 간격이 2초일 때 맞다), `vod` 는 녹화용입니다.

- `vod` 는 6초 조각을 지우지 않고 쌓고, 재생목록은 12시간 길이까지 이어집니다(`hls_playlist_length 12h`). 이보다 긴 방송은 앞부분이 다시보기에서 잘립니다.
- nginx-rtmp 는 방송이 끝나도 재생목록에 끝 표시를 붙이지 않아서, `exec_publish_done` 이 2초 뒤에 `#EXT-X-ENDLIST` 를 덧붙입니다. 끝 표시가 없으면 플레이어가
  "아직 방송 중" 으로 알고 맨 끝만 따라갑니다.
- 파일은 컨테이너의 `/tmp/vod` 에 쌓입니다. compose 는 `streaming-vod` 볼륨으로 붙여 두었습니다. **자동으로 지우지 않으니** 디스크가 차기 전에 오래된 것을 정리하세요.

  ```bash
  # 30일 지난 다시보기 조각 삭제 (cron 등으로)
  docker exec sp-streaming find /tmp/vod -type f -mtime +30 -delete
  ```
- API 는 녹화가 실제로 됐는지 확인하지 않습니다. `VOD_ENABLED` 는 위 nginx 설정을 쓰는 서버에서만 켜세요.
- 무료로 운영하는 Render API 와 이 PC 의 스트리밍 서버를 잇는 경우에도 같습니다. `VOD_BASE_URL` 은 시청자 브라우저가 직접 여는 주소여야 합니다.

### 브라우저로 방송하기 (내 PC·OBS 없이)

운영 사이트(Render 무료)는 RTMP 를 받지 못하지만, **웹 화면에서 화면 공유나 카메라를 바로 보내는 방식**은 된다. 누구나 로그인하고 **방송하기**를 누르면 방송이 열린다.

```
브라우저(MediaRecorder, H.264) ──WebSocket /ingest──▶ API(ffmpeg 가 포장만 바꿈) ──HLS /live-hls/──▶ 시청자
```

- 연결하고 첫 메시지로 로그인 토큰을 보낸다(WebSocket 은 헤더를 못 붙인다). 서버가 방송을 열고 조각을 받는 대로 ffmpeg 에 넣는다. 연결이 끊기면 방송이 끝난다.
- 영상은 **다시 인코딩하지 않고**(copy) 소리만 AAC 로 바꾼다. 그래서 CPU 를 거의 안 쓰지만, 브라우저가 **H.264 로 녹화**할 수 있어야 한다(Chrome·Edge 가능, Firefox·Safari 불가).
- 재생 이름이 `b-` 로 시작하면 브라우저 방송이다. 이 방송의 HLS 는 스트리밍 서버가 아니라 **API 가 직접** 내려 준다(`/live-hls/<이름>.m3u8`). DB 에 따로 표시하지 않아서 마이그레이션이 필요 없다.
- 서버가 꺼졌다 켜지면 끊긴 브라우저 방송은 켜질 때 자동으로 닫는다.
- **다시보기는 남지 않는다.** 디스크가 작고(무료 서버의 임시 디스크) 영상이 시간당 1GB 가까이 쌓이기 때문이다. 다시보기가 필요하면 OBS + 스트리밍 서버 방식을 쓴다.

| 환경변수 | 기본값 | 설명 |
|---|---|---|
| `BROWSER_INGEST_ENABLED` | `true` | 끄면 `/ingest` 연결을 받지 않는다. ffmpeg 가 없는 서버에서는 끈다 |
| `API_PUBLIC_URL` | `RENDER_EXTERNAL_URL`, 없으면 `http://localhost:8080` | API 자신의 바깥 주소. 시청자에게 내려 주는 재생 주소가 이 아래에 놓인다. Render 는 `RENDER_EXTERNAL_URL` 을 자동으로 넣어 준다 |
| `INGEST_MAX_SESSIONS` | `1` | 동시에 받을 수 있는 방송 수. 무료 서버(메모리 512MB)에서는 자바 약 350MB + 방송 하나당 ffmpeg 약 60MB 라 하나가 한계다. 메모리가 넉넉한 서버에서만 올린다 |
| `INGEST_MAX_KBPS` | `6000` | 한 방송이 보낼 수 있는 속도(10초 평균의 두 배까지). 넘기면 끊는다. 화면은 1500kbps(최대 1280×720)로 보낸다 |
| `INGEST_LOW_LATENCY` | `true` | 브라우저 방송을 HLS 와 함께 0.1초 단위 조각으로도 중계해 시청 지연을 1~2초로 줄인다([10-performance.md](10-performance.md)). 끄면 HLS 만(지연 약 6~8초). 서버가 못 버티면 이걸 끈다 |
| `INGEST_LOW_LATENCY_MAX_VIEWERS` | `30` | 저지연으로 받는 시청자 수 상한. 넘는 시청자는 HLS 로 본다. 512MB 서버에서 50명은 메모리가 한계였다 |
| `INGEST_HLS_DIR` | 임시 폴더의 `sp-ingest-hls` | HLS 조각이 놓이는 곳. 켜질 때 비운다 |

한계
- 무료 서버(메모리 512MB)에서 방송 한 개(2.5Mbps)를 받는 동안 메모리를 재 보니 자바 약 350MB + ffmpeg 약 60MB(총 약 410MB, 자바 힙 300MB 제한 기준)로 버텼다. 여유가 작아서 동시 방송은 1개로 막아 두었다.
- 무료 서버에서 시청자가 많아지면 API 가 영상 조각까지 내려 주느라 느려진다. 시청자가 수 명인 시연·과제용으로 본다.
- **저지연 시청(기본)**: 브라우저로 본 시청자는 WebSocket(`/watch`)으로 0.1초 단위 조각을 바로 받아 지연이 **약 1.6~1.8초**다. 지원하지 않는 브라우저·연결 실패·상한 초과는 아래 HLS 로 이어서 본다.
- HLS(대체 경로) 시청 지연은 **7~9초 정도**다. 조각 길이는 ffmpeg 가 아니라 **Chrome 이 키프레임을 내는 간격(약 100프레임 ≈ 3.4초)**이 정하고(복사 모드라 키프레임에서만 자를 수 있다), 재생기는 그 2개 뒤를 따라간다. 로컬에서 재 보니 움직이는 화면·정지 화면 모두 조각이 3.37초였다.
- 브라우저 탭을 닫거나 절전에 들어가면 방송이 끝난다.
- 네트워크가 느려 보내지 못한 데이터가 8MB 쌓이면 화면에서 방송을 멈춘다.

### 운영 사이트에서 라이브 켜기 (내 PC + 임시 터널)

무료 호스팅(Render)은 RTMP 같은 TCP 포트를 받지 못해서, 운영 사이트의 라이브는 **내 PC 가 스트리밍 서버 역할**을 합니다.

```
OBS ──RTMP──▶ 내 PC(nginx-rtmp, docker) ──콜백(https, 비밀 값)──▶ Render API
                       │
                       └─HLS(8081)──Cloudflare 임시 터널──▶ 시청자 브라우저(sp-web.vercel.app)
```

- 송출(1935)은 이 PC 안에서만 받습니다. 바깥에 여는 것은 영상 조각을 내려 주는 HLS 포트(8081)뿐이고, 임시 터널로만 닿습니다.
- 터널 주소는 켤 때마다 바뀝니다. 스트리밍 서버가 **방송을 시작할 때 자기 공개 주소를 API 에 알려 주고**(`hls=` 인자), API 는 그 주소를 시청자에게 내려 줍니다. 그래서 Render 환경변수를 매번 바꿀 필요가 없습니다.
- 이 알림은 **`RTMP_CALLBACK_TOKEN` 이 맞을 때만** 받습니다(`https` 주소 모양일 때만, 틀리면 무시). 남이 재생 주소를 바꿔 끼울 수 없게 하려는 것입니다. 값은 서버 메모리에만 있어서 API 가 재시작되면 다음 방송을 시작할 때 다시 받습니다.

**처음 한 번**
1. Render → `sp-api` → Environment 에 `RTMP_CALLBACK_TOKEN` 추가(영문·숫자 16자 이상). 저장하면 재배포됩니다.
2. (다시보기를 쓰려면) `VOD_ENABLED=true` 도 추가. 다시보기 주소는 같은 터널의 `/vod` 입니다.
3. PC 에 Docker Desktop 과 cloudflared(`winget install Cloudflare.cloudflared`) 를 설치합니다.

**방송할 때마다**
```powershell
.\scripts\start-live.ps1 -Token <1번에서 정한 값>
```
출력된 안내대로 OBS 서버 `rtmp://localhost:1935/live`, 스트림 키는 사이트의 **내 계정 → 스트림 키** 를 씁니다. 끝나면 `.\scripts\stop-live.ps1`.

한계: PC 가 꺼져 있거나 터널이 끊기면 시청할 수 없고, 임시 터널은 가용성 보장이 없습니다. 상시 운영하려면 Oracle Cloud Always Free VM 에 `streaming/` 을 올리고 `HLS_BASE_URL` 을 그 주소로 고정하는 편이 맞습니다.

### 수업·여럿이 볼 때 (한 반 10~30명)

시청자가 늘면 병목은 영상 조각을 내려주는 쪽의 **업로드 대역폭**과 **CPU** 다. 시청자 한 명이 방송 비트레이트만큼을 계속 받으므로
`비트레이트 × 시청자 수` 가 서버(또는 내 집 업로드)를 지나간다.

| 시청자 | 1.5Mbps | 2.5Mbps | 4Mbps |
|---|---|---|---|
| 13명 | 약 20Mbps | 약 33Mbps | 약 52Mbps |
| 30명 | 약 45Mbps | 약 75Mbps | 약 120Mbps |

- **브라우저 방송**: 영상은 무료 Render(0.1 CPU) 가 모두 내려준다. 화면은 1.5Mbps·최대 720p 로 보낸다.
- **OBS 방송**: 영상은 **내 PC 의 집 인터넷 업로드**로 나간다(터널). 집 업로드가 시청자 수 × 비트레이트보다 작으면 끊긴다. OBS 에서
  - 출력 → 비트레이트 **1500 Kbps**, 키프레임 간격 **2초**, 해상도 **1280×720**, 30fps, 인코더 x264 `veryfast` 또는 하드웨어
  - 키프레임 간격을 비워 두면(자동) 조각이 길어져 지연이 커지고 재생목록이 짧아져 끊긴다.
- 시청 화면에 `?fps=1` 을 붙이면 지연·버퍼·대역폭·끊긴 횟수가 보인다. 끊긴다는 제보가 오면 이 값을 먼저 본다.
  - 지연이 계속 커지면 서버가 못 따라가는 것, 버퍼가 0 근처이고 대역폭이 방송 비트레이트보다 낮으면 시청자의 망 문제다.
- 서버를 키우는 방법은 [아직 안 한 것](#아직-안-한-것)의 "영상 조각을 API 가 아닌 곳에서 내려주기".

### 데모 콘텐츠 (V8)

운영 사이트가 비어 보이지 않도록 `V8__demo_content.sql` 이 데모 채널 3개(`데모 …`)와 영상 6개, 카테고리 3개(애니메이션·단편 영화·자연)를 넣는다.
- 영상은 Blender Foundation 오픈 무비 예고편(CC BY 3.0)과 MDN CC0 샘플을 **원본 주소 그대로** 건다(서버 용량을 쓰지 않는다). 서로 다른 원본은 3개라 같은 영상을 다른 채널에서 다시 쓴다. 설명에 "[데모 영상]" 이라고 밝혔다.
- 시청 수·좋아요·댓글·구독자는 0 이다(지어내지 않는다). 데모 계정은 로그인할 수 없다(비밀번호가 BCrypt 값이 아님).
- 지우기(운영 DB 에서 직접):
  ```sql
  DELETE s FROM streams s JOIN users u ON u.id = s.user_id WHERE u.email LIKE 'demo-%@demo.invalid';
  DELETE FROM users WHERE email LIKE 'demo-%@demo.invalid';
  ```
  카테고리는 쓰는 사람이 생겼을 수 있으니 관리자 화면에서 지운다.

### 다른 주소로 서비스할 때

Vite 는 API 주소를 **빌드할 때 코드에 박아 넣습니다.** 컨테이너를 다시 띄우는 것만으로는 안 바뀝니다.

```bash
VITE_API_BASE_URL=https://api.내도메인 docker compose --profile full up -d --build web
```

같이 바꿔야 하는 것:

- `api.environment.CORS_ALLOWED_ORIGINS` → 화면 주소
- `api.environment.HLS_BASE_URL` → 바깥에서 닿는 HLS 주소
- `streaming/nginx.conf` 의 `on_publish` 콜백 주소 (API 가 컨테이너 밖에 있을 때만)

---

## GitHub Actions

| 워크플로 | 언제 | 하는 일 |
|---|---|---|
| `ci.yml` | PR · main | API `./gradlew build`, Web `lint` → `build` |
| `docker.yml` | PR · main | 이미지 세 개를 실제로 빌드. main 이면 GHCR 로 푸시 |

`docker.yml` 은 PR 에서 **만들기만 하고 올리지 않습니다.** Dockerfile 이 깨졌는지 여기서 걸러집니다.
`main` 에 들어가면 `ghcr.io/ginye28/streaming-platform/{api,web,streaming}` 으로 올라갑니다.
태그는 `latest` 와 커밋 SHA 두 가지입니다.

푸시는 `GITHUB_TOKEN` 으로 하므로 따로 넣어 둘 비밀값이 없습니다.

---

## 아직 안 한 것

- **서버에 올리는 자동화(CD).** 이미지는 GHCR 까지 올라가지만,
  그걸 받아서 다시 띄우는 건 아직 손으로 합니다.
- **HTTPS.** 인증서와 리버스 프록시가 없습니다.
  실제 도메인에 붙일 때는 `web` 앞에 프록시를 하나 더 두는 게 편합니다.
- **DB 백업.** `mysql-data` 볼륨에만 있습니다.
- **영상 조각을 API 가 아닌 곳에서 내려주기.** 지금은 브라우저 방송의 조각을 무료 Render(0.1 CPU) 의 자바가 직접 내려 주어, 시청자가
  10명을 넘으면 CPU·대역폭이 먼저 한계에 닿는다(위 "수업·여럿이 볼 때"). 근본 해결은 조각 파일만 다른 곳(Cloudflare R2·Backblaze B2 + CDN,
  또는 Oracle Always Free VM 에 nginx)에 올려 거기서 내려주는 것이다. 계정·키가 필요해서 아직 하지 않았다.

### 메모리 (무료 서버 512MB)

Render 무료 서비스는 메모리가 512MB 를 넘으면 서버가 죽고 다시 켜진다(기동에 약 3분 — 그동안 화면에는 "서버를 깨우는 중" 이 뜬다).
실제로 겪은 일과 그에 대한 조치.

- **영상 썸네일**: 4K HEVC(아이폰 `.mov` 등) 영상 한 장을 뽑는 데 ffmpeg 가 기본 설정으로 **약 510MB** 를 써서 서버가 죽었다.
  `-threads 1 -skip_frame nokey` 로 약 50MB 가 된다(`VideoThumbnailService`). 키프레임만 읽는 방식이 안 맞는 파일은 스레드만 1개로 줄인 방식으로 한 번 더 시도한다.
- **자바**: 힙은 `JAVA_TOOL_OPTIONS=-Xmx300m …` 이 정한다. 이미지(`api/Dockerfile`)에서 JIT 를 C1 만 쓰고 스레드 스택·직접 메모리를 줄였다.
- **API 문서**: 운영에서는 끈다(`SWAGGER_ENABLED`).
- **웹 방송**: ffmpeg 가 방송당 약 60MB. 동시 1개로 막아 두었다.
- 서버가 메모리로 죽었는지는 Render → 서비스 → Events 의 "Ran out of memory" 로 확인한다.

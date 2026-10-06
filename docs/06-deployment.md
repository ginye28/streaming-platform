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
| `MEMBERSHIP_PERIOD_DAYS` | `30` | 유료 구독 한 번으로 이용하는 기간(일) |
| `MEMBERSHIP_REFUND_WINDOW_DAYS` | `7` | 사용자가 직접 환불할 수 있는 기간(일). 관리자는 기간과 상관없이 환불한다 |
| `MEMBERSHIP_PLATFORM_FEE_PERCENT` | `0` | 채널 주인 수익 장부에서 떼는 서비스 수수료율(%). 장부일 뿐 송금은 하지 않는다 |
| `MEMBERSHIP_EXPIRY_NOTICE_DAYS` | `3` | 유료 구독이 끝나기 며칠 전부터 알릴지 |
| `TOSS_WEBHOOK_TOKEN` | 비어 있음 | 토스 웹훅 주소에 `?token=` 으로 붙이는 비밀 값. 비워 두면 검사하지 않는다 |
| `DONATION_AMOUNTS` | `1000,3000,5000,10000,30000,50000` | 방송 후원으로 고를 수 있는 금액(원, 쉼표 구분). 이 목록에 없는 금액은 주문을 만들어 주지 않는다 |
| `VOD_ENABLED` | `false` (compose 는 `true`) | 스트리밍 서버가 방송을 녹화하는지. 켜면 방송이 끝날 때 다시보기 주소를 내려 준다. **녹화하지 않는 서버에서 켜면 없는 주소를 가리킨다** |
| `VOD_BASE_URL` | 비어 있음 | 다시보기 재생 주소의 앞부분. 비우면 `HLS_BASE_URL` 옆의 `/vod` 를 쓴다 |
| `RTMP_CALLBACK_TOKEN` | 비어 있음 | nginx 가 송출 시작·종료 콜백 주소에 `?token=` 으로 붙이는 비밀 값. **스트리밍 서버와 API 에 같은 값**을 넣는다. 비워 두면 검사하지 않으므로 콜백이 바깥에 열린 서버에서는 반드시 채운다(영문·숫자만 쓸 것). 틀리면 403 |
| `LOG_LEVEL` | `INFO` | |

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

`streaming/nginx.conf` 가 방송을 두 갈래로 흘립니다. `hls` 는 실시간 재생용(3초 조각, 12초만 유지), `vod` 는 녹화용입니다.

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

#!/usr/bin/env bash
# OBS 송출이 제대로 동작하는지 끝까지 확인해 주는 스크립트.
#
#   ./scripts/verify-live.sh
#
# 필요한 것: 백엔드(8080)와 스트리밍 서버(1935/8081)가 떠 있을 것.
# curl 만 있으면 되고 jq 나 python 은 필요 없다.

set -u

API="${API_BASE_URL:-http://localhost:8080}"
WEB="${WEB_BASE_URL:-http://localhost:5173}"
EMAIL="${TEST_EMAIL:-obs-test@example.com}"
PASSWORD="password123"
NICKNAME="OBS테스터"
WAIT_SECONDS="${WAIT_SECONDS:-180}"

say()  { printf '%s\n' "$*"; }
ok()   { printf '  \033[32m✓\033[0m %s\n' "$*"; }
bad()  { printf '  \033[31m✗\033[0m %s\n' "$*"; }
warn() { printf '  \033[33m!\033[0m %s\n' "$*"; }

# JSON 에서 문자열 필드 하나를 뽑는다.
field() { sed -n "s/.*\"$1\":\"\([^\"]*\)\".*/\1/p" | head -1; }
number() { sed -n "s/.*\"$1\":\([0-9]*\).*/\1/p" | head -1; }

say ""
say "=== 1. 백엔드 확인 ==="
if ! curl -sf -o /dev/null "$API/api/streams"; then
  bad "백엔드에 연결할 수 없습니다: $API"
  say "    api 디렉터리에서 실행하세요:"
  say "    ./gradlew bootRun --args='--spring.profiles.active=local'"
  exit 1
fi
ok "백엔드 응답 정상 ($API)"

say ""
say "=== 2. 스트리밍 서버 확인 ==="
if curl -sf -o /dev/null "${HLS_BASE_URL:-http://localhost:8081}/health"; then
  ok "스트리밍 서버 응답 정상"
else
  bad "스트리밍 서버에 연결할 수 없습니다 (8081)"
  say "    streaming 디렉터리에서 실행하세요: docker compose up -d --build"
  exit 1
fi

say ""
say "=== 3. 테스트 계정 준비 ==="
curl -s -o /dev/null -X POST "$API/api/auth/signup" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\",\"nickname\":\"$NICKNAME\"}"

TOKEN=$(curl -s -X POST "$API/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}" | field accessToken)

if [ -z "$TOKEN" ]; then
  bad "로그인 실패. 이미 다른 비밀번호로 가입된 계정일 수 있습니다."
  say "    TEST_EMAIL=다른주소 ./scripts/verify-live.sh 로 다시 시도하세요."
  exit 1
fi
ok "로그인 완료 ($EMAIL)"

STREAM_KEY=$(curl -s "$API/api/users/stream-key" \
  -H "Authorization: Bearer $TOKEN" | field streamKey)

if [ -z "$STREAM_KEY" ]; then
  bad "스트림 키를 가져오지 못했습니다."
  exit 1
fi
ok "스트림 키 확보"

say ""
say "=== 4. OBS 설정 ==="
say "  OBS → 설정 → 방송 에 아래 값을 넣으세요."
say ""
say "    서비스   : 사용자 지정..."
say "    서버     : rtmp://localhost:1935/live"
say "    스트림 키: $STREAM_KEY"
say ""
say "  입력했으면 OBS 에서 [방송 시작] 을 누르세요."
say "  ${WAIT_SECONDS}초 동안 기다립니다..."
say ""

LIVE_JSON=""
ELAPSED=0
while [ "$ELAPSED" -lt "$WAIT_SECONDS" ]; do
  LIVE_JSON=$(curl -s "$API/api/lives")
  TOTAL=$(printf '%s' "$LIVE_JSON" | number totalElements)
  if [ "${TOTAL:-0}" -gt 0 ]; then
    break
  fi
  sleep 3
  ELAPSED=$((ELAPSED + 3))
  printf '.'
done
say ""

if [ "${TOTAL:-0}" -eq 0 ]; then
  bad "방송이 감지되지 않았습니다."
  say ""
  say "  확인해 볼 것:"
  say "   - OBS 가 '연결 중' 에서 멈춰 있나요? → 스트림 키를 다시 확인하세요."
  say "   - streaming 로그: docker compose -f streaming/docker-compose.yml logs -f"
  say "   - nginx 가 백엔드에 닿지 못하면 송출이 거부됩니다."
  say "     (nginx.conf 의 host.docker.internal 주소)"
  exit 1
fi

ok "방송 감지됨"

TITLE=$(printf '%s' "$LIVE_JSON" | field title)
HLS_URL=$(printf '%s' "$LIVE_JSON" | field hlsUrl)
PUBLIC_NAME=$(printf '%s' "$HLS_URL" | sed 's#.*/##; s#\.m3u8$##')

say ""
say "=== 5. 결과 ==="
say "  제목    : $TITLE"
say "  재생 URL: $HLS_URL"

say ""
say "  [스트림 키 노출 여부]"
case "$HLS_URL" in
  *"$STREAM_KEY"*)
    warn "재생 URL 에 스트림 키가 그대로 들어 있습니다."
    say "      → on_publish 리다이렉트가 동작하지 않았습니다."
    say "      → app.rtmp.rename-on-publish 설정과 nginx 로그를 확인하세요."
    ;;
  *)
    ok "재생 URL 에 스트림 키가 없습니다 (리다이렉트 정상 동작)"
    ;;
esac

say ""
say "  [HLS 재생 파일]"
# nginx 가 첫 조각을 만들기까지 몇 초 걸린다.
HLS_OK=""
for _ in 1 2 3 4 5 6 7 8 9 10; do
  if curl -sf -o /dev/null "$HLS_URL"; then
    HLS_OK="yes"
    break
  fi
  sleep 2
done

if [ -n "$HLS_OK" ]; then
  ok "재생 목록(.m3u8)이 생성되었습니다"
else
  bad "재생 목록을 가져오지 못했습니다: $HLS_URL"
  say "      → nginx 가 HLS 를 만들고 있는지 확인하세요."
  say "         docker compose -f streaming/docker-compose.yml exec streaming ls /tmp/hls"
fi

say ""
say "=== 6. 브라우저에서 보기 ==="
say "  $WEB/?stream=$PUBLIC_NAME"
say ""
say "  방송을 끄면 목록에서 사라지는지도 확인해 보세요:"
say "    curl -s $API/api/lives"
say ""

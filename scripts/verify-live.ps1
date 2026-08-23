# OBS 송출이 제대로 동작하는지 끝까지 확인해 주는 스크립트 (Windows PowerShell 용).
#
#   powershell -ExecutionPolicy Bypass -File scripts\verify-live.ps1
#
# 필요한 것: 백엔드(8080)와 스트리밍 서버(1935/8081)가 떠 있을 것.
#
# 이 파일은 UTF-8 BOM 으로 저장해야 한다.
# Windows PowerShell 5.1 은 BOM 이 없으면 .ps1 을 시스템 코드페이지로 읽어
# 한글이 깨지고 구문 오류가 난다.

$ErrorActionPreference = 'Stop'

$Api         = if ($env:API_BASE_URL) { $env:API_BASE_URL } else { 'http://localhost:8080' }
$Web         = if ($env:WEB_BASE_URL) { $env:WEB_BASE_URL } else { 'http://localhost:5173' }
$HlsBase     = if ($env:HLS_BASE_URL) { $env:HLS_BASE_URL } else { 'http://localhost:8081' }
$Email       = if ($env:TEST_EMAIL)   { $env:TEST_EMAIL }   else { 'obs-test@example.com' }
$Password    = 'password123'
$Nickname    = 'OBS테스터'
$WaitSeconds = if ($env:WAIT_SECONDS) { [int]$env:WAIT_SECONDS } else { 180 }

$Json = 'application/json; charset=utf-8'

function Ok   ($m) { Write-Host "  [OK] $m" -ForegroundColor Green }
function Bad  ($m) { Write-Host "  [!!] $m" -ForegroundColor Red }
function Warn ($m) { Write-Host "  [??] $m" -ForegroundColor Yellow }

Write-Host ''
Write-Host '=== 1. 백엔드 확인 ==='
try {
    Invoke-RestMethod -Uri "$Api/api/streams" -Method Get -TimeoutSec 5 | Out-Null
    Ok "백엔드 응답 정상 ($Api)"
} catch {
    Bad "백엔드에 연결할 수 없습니다: $Api"
    Write-Host '     api 폴더에서 실행하세요:'
    Write-Host '     gradlew.bat bootRun --args="--spring.profiles.active=local"'
    exit 1
}

Write-Host ''
Write-Host '=== 2. 스트리밍 서버 확인 ==='
try {
    Invoke-RestMethod -Uri "$HlsBase/health" -Method Get -TimeoutSec 5 | Out-Null
    Ok '스트리밍 서버 응답 정상'
} catch {
    Bad '스트리밍 서버에 연결할 수 없습니다 (8081)'
    Write-Host '     프로젝트 루트에서 실행하세요: docker compose up -d streaming'
    exit 1
}

Write-Host ''
Write-Host '=== 3. 테스트 계정 준비 ==='

$signupBody = @{ email = $Email; password = $Password; nickname = $Nickname } | ConvertTo-Json
$signupArgs = @{
    Uri         = "$Api/api/auth/signup"
    Method      = 'Post'
    ContentType = $Json
    Body        = $signupBody
}
try {
    Invoke-RestMethod @signupArgs | Out-Null
} catch {
    # 이미 가입돼 있으면 409. 그대로 진행한다.
}

$loginBody = @{ email = $Email; password = $Password } | ConvertTo-Json
$loginArgs = @{
    Uri         = "$Api/api/auth/login"
    Method      = 'Post'
    ContentType = $Json
    Body        = $loginBody
}
try {
    $token = (Invoke-RestMethod @loginArgs).data.accessToken
} catch {
    Bad '로그인 실패. 이미 다른 비밀번호로 가입된 계정일 수 있습니다.'
    Write-Host '     아래처럼 다른 주소를 지정한 뒤 다시 시도하세요.'
    Write-Host '     $env:TEST_EMAIL = "other@example.com"'
    exit 1
}
Ok "로그인 완료 ($Email)"

$authHeader = @{ Authorization = "Bearer $token" }
$streamKey = (Invoke-RestMethod -Uri "$Api/api/users/stream-key" -Headers $authHeader).data.streamKey
Ok '스트림 키 확보'

Write-Host ''
Write-Host '=== 4. OBS 설정 ==='
Write-Host '  OBS -> 설정 -> 방송 에 아래 값을 넣으세요.'
Write-Host ''
Write-Host '    서비스   : 사용자 지정...'
Write-Host '    서버     : rtmp://localhost:1935/live'
Write-Host "    스트림 키: $streamKey" -ForegroundColor Cyan
Write-Host ''
Write-Host '  입력했으면 OBS 에서 [방송 시작] 을 누르세요.'
Write-Host "  $WaitSeconds 초 동안 기다립니다..."
Write-Host ''

$live = $null
$elapsed = 0
while ($elapsed -lt $WaitSeconds) {
    $lives = Invoke-RestMethod -Uri "$Api/api/lives"
    if ($lives.data.totalElements -gt 0) {
        $live = $lives.data.content[0]
        break
    }
    Start-Sleep -Seconds 3
    $elapsed = $elapsed + 3
    Write-Host -NoNewline '.'
}
Write-Host ''

if (-not $live) {
    Bad '방송이 감지되지 않았습니다.'
    Write-Host ''
    Write-Host '  확인해 볼 것:'
    Write-Host '   - OBS 가 [연결 중] 에서 멈춰 있나요? 스트림 키를 다시 확인하세요.'
    Write-Host '   - 스트리밍 로그: docker compose logs -f streaming'
    Write-Host '   - nginx 가 백엔드에 닿지 못하면 송출이 거부됩니다.'
    exit 1
}

Ok '방송 감지됨'

Write-Host ''
Write-Host '=== 5. 결과 ==='
Write-Host "  제목    : $($live.title)"
Write-Host "  재생 URL: $($live.hlsUrl)"

Write-Host ''
Write-Host '  [스트림 키 노출 여부]'
if ($live.hlsUrl -like "*$streamKey*") {
    Warn '재생 URL 에 스트림 키가 그대로 들어 있습니다.'
    Write-Host '       on_publish 리다이렉트가 동작하지 않았습니다.'
    Write-Host '       app.rtmp.rename-on-publish 설정과 nginx 로그를 확인하세요.'
} else {
    Ok '재생 URL 에 스트림 키가 없습니다 (리다이렉트 정상 동작)'
}

Write-Host ''
Write-Host '  [HLS 재생 파일]'
$hlsOk = $false
for ($i = 0; $i -lt 10; $i++) {
    try {
        # -UseBasicParsing 이 없으면 PowerShell 5.1 에서 IE 파싱 보안 경고가 뜬다.
        Invoke-WebRequest -Uri $live.hlsUrl -Method Get -TimeoutSec 5 -UseBasicParsing | Out-Null
        $hlsOk = $true
        break
    } catch {
        Start-Sleep -Seconds 2
    }
}

if ($hlsOk) {
    Ok '재생 목록(.m3u8)이 생성되었습니다'
} else {
    Bad "재생 목록을 가져오지 못했습니다: $($live.hlsUrl)"
    Write-Host '       docker compose exec streaming ls /tmp/hls'
}

$publicName = [System.IO.Path]::GetFileNameWithoutExtension($live.hlsUrl)

Write-Host ''
Write-Host '=== 6. 브라우저에서 보기 ==='
Write-Host "  $Web/?stream=$publicName" -ForegroundColor Cyan
Write-Host ''

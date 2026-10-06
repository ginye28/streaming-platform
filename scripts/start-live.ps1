<#
  운영 사이트(Vercel 웹 + Render API)에서 라이브 방송이 되게 하는 스크립트.

  내 PC 에서 스트리밍 서버(nginx-rtmp)를 켜고, 시청자가 영상을 받을 HLS 포트(8081)만
  Cloudflare 임시 터널로 공개한다. 송출(RTMP 1935)은 이 PC 안에서만 받는다. OBS 도 이 PC 에서 켠다.

  미리 필요한 것
    1. Docker Desktop 실행 중
    2. cloudflared 설치 (winget install Cloudflare.cloudflared)
    3. Render API 에 환경변수 RTMP_CALLBACK_TOKEN 설정 (영문·숫자 16자 이상). 이 스크립트에도 같은 값을 준다.

  사용
    .\scripts\start-live.ps1 -Token 내가정한비밀값
    (또는 $env:RTMP_CALLBACK_TOKEN 에 넣어 두고 인자 없이 실행)

  끄기: .\scripts\stop-live.ps1
#>
param(
    [string]$Token = $env:RTMP_CALLBACK_TOKEN,
    [string]$ApiHost = 'sp-api-5mal.onrender.com'
)

$ErrorActionPreference = 'Stop'

$repo = Split-Path -Parent $PSScriptRoot
$template = Join-Path $repo 'streaming\nginx.tunnel.conf'
$work = Join-Path $env:TEMP 'sp-live'
New-Item -ItemType Directory -Force $work | Out-Null

if (-not $Token -or $Token -notmatch '^[A-Za-z0-9]{12,}$') {
    throw 'RTMP_CALLBACK_TOKEN 은 영문·숫자만 12자 이상이어야 합니다. -Token 으로 넘기거나 환경변수에 넣으세요.'
}

# cloudflared 위치
$cf = (Get-Command cloudflared -ErrorAction SilentlyContinue).Source
if (-not $cf) { $cf = 'C:\Program Files (x86)\cloudflared\cloudflared.exe' }
if (-not (Test-Path $cf)) { throw 'cloudflared 가 없습니다. winget install Cloudflare.cloudflared 로 설치하세요.' }

docker info *> $null
if ($LASTEXITCODE -ne 0) { throw 'Docker Desktop 이 꺼져 있습니다. 켜고 다시 실행하세요.' }

# 이전 실행 정리
& "$PSScriptRoot\stop-live.ps1" -Quiet

# 1) 임시 터널로 HLS 포트 공개 → 주소를 로그에서 읽는다
$log = Join-Path $work 'cloudflared.log'
Remove-Item $log -ErrorAction SilentlyContinue
$proc = Start-Process -FilePath $cf `
    -ArgumentList @('tunnel', '--no-autoupdate', '--url', 'http://localhost:8081') `
    -RedirectStandardError $log -RedirectStandardOutput (Join-Path $work 'cloudflared.out') `
    -WindowStyle Hidden -PassThru
$proc.Id | Set-Content (Join-Path $work 'cloudflared.pid')

$publicBase = $null
for ($i = 0; $i -lt 60 -and -not $publicBase; $i++) {
    Start-Sleep -Seconds 1
    if (Test-Path $log) {
        $m = Select-String -Path $log -Pattern 'https://[a-z0-9-]+\.trycloudflare\.com' | Select-Object -First 1
        if ($m) { $publicBase = $m.Matches[0].Value }
    }
}
if (-not $publicBase) { throw "터널 주소를 못 얻었습니다. 로그: $log" }

# 2) 설정 틀의 자리표시자를 채운 사본을 만든다 (LF 줄바꿈)
$conf = (Get-Content $template -Raw -Encoding UTF8).Replace("`r`n", "`n")
$conf = $conf.Replace('__API_HOST__', $ApiHost)
$conf = $conf.Replace('__RTMP_CALLBACK_TOKEN__', $Token)
$conf = $conf.Replace('__HLS_PUBLIC_BASE__', "$publicBase/hls")
$confPath = Join-Path $work 'nginx.conf'
[IO.File]::WriteAllText($confPath, $conf, (New-Object Text.UTF8Encoding($false)))

# 3) 스트리밍 서버
docker run -d --name sp-streaming-live `
    -p 1935:1935 -p 8081:8081 `
    -v "${confPath}:/etc/nginx/nginx.conf:ro" `
    -v sp-streaming-vod:/tmp/vod `
    tiangolo/nginx-rtmp | Out-Null
if ($LASTEXITCODE -ne 0) { throw '스트리밍 서버 컨테이너를 띄우지 못했습니다.' }

Start-Sleep -Seconds 2
try {
    $health = (Invoke-WebRequest -UseBasicParsing "$publicBase/health" -TimeoutSec 20).Content
} catch { $health = $null }

Write-Host ''
Write-Host '라이브 준비 완료' -ForegroundColor Green
Write-Host "  공개 주소(시청용)  : $publicBase/hls   (확인: $(if ($health -eq 'ok') { '정상' } else { '아직 응답 없음 - 몇 초 뒤 다시' }))"
Write-Host '  OBS 서버           : rtmp://localhost:1935/live'
Write-Host '  OBS 스트림 키      : 사이트 로그인 → 내 계정 → 스트림 키'
Write-Host ''
Write-Host '주의: 이 PC 와 이 창의 터널이 켜져 있는 동안만 시청할 수 있습니다. 끌 때는 scripts\stop-live.ps1'
Write-Host '      터널 주소는 켤 때마다 바뀌지만, 방송을 시작하면 API 가 새 주소를 받아 갑니다.'

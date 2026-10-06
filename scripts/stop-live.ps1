<#
  start-live.ps1 로 켠 것을 끈다. (스트리밍 서버 컨테이너 + 임시 터널)
  다시보기 녹화(docker 볼륨 sp-streaming-vod)는 남긴다.
#>
param([switch]$Quiet)

$ErrorActionPreference = 'Continue'

docker rm -f sp-streaming-live *> $null

$pidFile = Join-Path $env:TEMP 'sp-live\cloudflared.pid'
if (Test-Path $pidFile) {
    $old = [int](Get-Content $pidFile)
    Stop-Process -Id $old -Force -ErrorAction SilentlyContinue
    Remove-Item $pidFile -ErrorAction SilentlyContinue
}

if (-not $Quiet) { Write-Host '라이브 서버와 터널을 껐습니다.' }

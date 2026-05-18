# MinimalPayAndroid — Git 초기화 · 커밋 · Push
# 사용: PowerShell에서  C:\MinimalPayAndroid  폴더로 이동 후
#   .\push-to-git.ps1
# GitHub 원격이 없으면 gh CLI로 저장소 생성 후 push (gh 로그인 필요)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    Write-Error "git이 설치되어 있지 않습니다."
}

if (-not (Test-Path .git)) {
    git init
    git branch -M main
    Write-Host "Git 저장소 초기화 완료"
}

git add -A
$status = git status --porcelain
if (-not $status) {
    Write-Host "커밋할 변경 사항이 없습니다."
} else {
    git commit -m @"
feat: MinimalPay Android 정산 앱

- 4단계 위저드 UI (그룹/지출/정산/송금)
- 그룹 카드 내 참여 멤버 추가·목록
- Strategy/GRASP 도메인·Controller 레이어
- UC-4 Extend 송금 연동
"@
    Write-Host "커밋 완료"
}

$remote = git remote get-url origin 2>$null
if (-not $remote) {
    Write-Host "원격 저장소가 없습니다."
    if (Get-Command gh -ErrorAction SilentlyContinue) {
        $repoName = "MinimalPayAndroid"
        Write-Host "gh로 GitHub 저장소 생성 및 push 시도: $repoName"
        gh repo create $repoName --public --source=. --remote=origin --push
    } else {
        Write-Host @"

다음 중 하나를 실행하세요:

1) GitHub에 빈 저장소 만든 뒤:
   git remote add origin https://github.com/사용자명/MinimalPayAndroid.git
   git push -u origin main

2) GitHub CLI 설치 후 이 스크립트를 다시 실행 (자동 생성)
"@
        exit 1
    }
} else {
    Write-Host "Push: origin ($remote)"
    git push -u origin HEAD
}

Write-Host "완료."

$vhdxPath = "D:\softdata\docker-data\DockerDesktopWSL\disk\docker_data.vhdx"
$tmpScript = "$env:TEMP\diskpart_commands.txt"

$before = (Get-Item $vhdxPath).Length / 1GB
Write-Host "=== VHDX 压缩 ===" -ForegroundColor Cyan
Write-Host "文件: $vhdxPath"
Write-Host "压缩前: $([math]::Round($before, 2)) GB"
Write-Host ""

@"
select vdisk file="$vhdxPath"
attach vdisk readonly
compact vdisk
detach vdisk
exit
"@ | Out-File -FilePath $tmpScript -Encoding ASCII

Write-Host "正在压缩..." -ForegroundColor Yellow
diskpart /s $tmpScript

Remove-Item $tmpScript -Force

$after = (Get-Item $vhdxPath).Length / 1GB
Write-Host ""
Write-Host "=== 完成 ===" -ForegroundColor Green
Write-Host "压缩后: $([math]::Round($after, 2)) GB"
Write-Host "释放空间: $([math]::Round($before - $after, 2)) GB"
Write-Host ""
Write-Host "按任意键退出..." -NoNewline
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

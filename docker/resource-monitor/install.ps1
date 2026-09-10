$ErrorActionPreference = 'Stop'
$scriptPath = Join-Path $PSScriptRoot 'collect.ps1'
$executable = Join-Path $env:SystemRoot 'System32/WindowsPowerShell/v1.0/powershell.exe'
$arguments = '-NoProfile -NonInteractive -WindowStyle Hidden -ExecutionPolicy Bypass -File "' + $scriptPath + '"'
$currentAccount = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name
$action = New-ScheduledTaskAction -Execute $executable -Argument $arguments -WorkingDirectory $PSScriptRoot
$trigger = New-ScheduledTaskTrigger -AtLogOn -User $currentAccount
$principal = New-ScheduledTaskPrincipal -UserId $currentAccount -LogonType Interactive -RunLevel Limited
$settings = New-ScheduledTaskSettingsSet -ExecutionTimeLimit ([TimeSpan]::Zero) -MultipleInstances IgnoreNew -RestartCount 3 -RestartInterval (New-TimeSpan -Minutes 1) -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries
Register-ScheduledTask -TaskName 'DrinkingWater-ResourceMonitor' -Action $action -Trigger $trigger -Principal $principal -Settings $settings -Description 'Read-only host and Docker resource snapshots for the drinking water admin console' -Force | Out-Null
Start-ScheduledTask -TaskName 'DrinkingWater-ResourceMonitor'
Get-ScheduledTask -TaskName 'DrinkingWater-ResourceMonitor' | Select-Object TaskName,State

param([switch]$Once)
$ErrorActionPreference = 'Stop'
$dataDirectory = Join-Path $PSScriptRoot 'data'
[System.IO.Directory]::CreateDirectory($dataDirectory) | Out-Null
$mutex = [System.Threading.Mutex]::new($false, 'Local\DrinkingWaterResourceCollector')
if (-not $mutex.WaitOne(0)) { exit 0 }

function Invoke-DockerRead([string[]]$Arguments) {
    $start = [System.Diagnostics.ProcessStartInfo]::new()
    $start.FileName = 'docker.exe'
    $start.Arguments = $Arguments -join ' '
    $start.UseShellExecute = $false
    $start.CreateNoWindow = $true
    $start.RedirectStandardOutput = $true
    $start.RedirectStandardError = $true
    $process = [System.Diagnostics.Process]::Start($start)
    try {
        $output = $process.StandardOutput.ReadToEndAsync()
        $errorOutput = $process.StandardError.ReadToEndAsync()
        if (-not $process.WaitForExit(12000)) { $process.Kill(); throw 'Docker read timed out' }
        if ($process.ExitCode -ne 0) { throw 'Docker read failed' }
        return $output.GetAwaiter().GetResult()
    } finally { $process.Dispose() }
}

function PercentValue($value) {
    $parsed = 0.0
    if ([double]::TryParse(([string]$value).TrimEnd('%'), [Globalization.NumberStyles]::Float,
            [Globalization.CultureInfo]::InvariantCulture, [ref]$parsed)) { return $parsed }
    return $null
}

try {
    do {
        try {
            $os = Get-CimInstance Win32_OperatingSystem -OperationTimeoutSec 5
            $cpus = @(Get-CimInstance Win32_Processor -OperationTimeoutSec 5)
            $disks = @(Get-CimInstance Win32_LogicalDisk -Filter 'DriveType=3' -OperationTimeoutSec 5 | ForEach-Object {
                @{ name=$_.DeviceID; totalBytes=[long]$_.Size; freeBytes=[long]$_.FreeSpace }
            })
            $dockerStatus = 'OK'
            $containers = @()
            try {
                $ids = @((Invoke-DockerRead @('ps','-aq','--filter','name=dw-')).Trim() -split '\s+' | Where-Object { $_ })
                if ($ids.Count -gt 0) {
                    # Persist operational fields only, never environment variables or mount details.
                    $format = '"{{.Name}}|{{.State.Status}}|{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}|{{.RestartCount}}|{{.State.ExitCode}}"'
                    $states = Invoke-DockerRead (@('inspect','--format',$format) + $ids)
                    $statsByName = @{}
                    $statsText = Invoke-DockerRead @('stats','--no-stream','--format','json')
                    foreach ($line in ($statsText -split '\r?\n' | Where-Object { $_ })) {
                        $stat = $line | ConvertFrom-Json
                        $statsByName[$stat.Name] = $stat
                    }
                    $containers = @($states -split '\r?\n' | Where-Object { $_ } | ForEach-Object {
                        $parts = $_.Split('|')
                        $name = $parts[0].TrimStart('/')
                        if ($name.EndsWith('-init') -and $parts[1] -eq 'exited' -and $parts[4] -eq '0') { $parts[1] = 'completed' }
                        $stat = $statsByName[$name]
                        @{ name=$name; state=$parts[1]; health=$parts[2]; restarts=[int]$parts[3];
                           cpuPercent=$(if($stat){PercentValue $stat.CPUPerc}else{$null});
                           memoryUsage=$(if($stat){$stat.MemUsage}else{$null});
                           memoryPercent=$(if($stat){PercentValue $stat.MemPerc}else{$null});
                           networkIO=$(if($stat){$stat.NetIO}else{$null});
                           blockIO=$(if($stat){$stat.BlockIO}else{$null});
                           pids=$(if($stat -and $stat.PIDs -match '^\d+$'){[int]$stat.PIDs}else{$null}) }
                    })
                }
            } catch { $dockerStatus = 'UNAVAILABLE' }
            $sample = @{
                sampledAt=[DateTime]::UtcNow.ToString('o');
                host=@{ name=$env:COMPUTERNAME; os=$os.Caption;
                    cores=[int](($cpus | Measure-Object NumberOfLogicalProcessors -Sum).Sum);
                    cpuPercent=($cpus | Measure-Object LoadPercentage -Average).Average;
                    totalMemoryBytes=[long]$os.TotalVisibleMemorySize * 1024;
                    usedMemoryBytes=([long]$os.TotalVisibleMemorySize - [long]$os.FreePhysicalMemory) * 1024;
                    disks=$disks };
                docker=@{ status=$dockerStatus; containers=$containers }
            }
            $temporary = Join-Path $dataDirectory 'snapshot.tmp'
            $destination = Join-Path $dataDirectory 'snapshot.json'
            [System.IO.File]::WriteAllText($temporary, ($sample | ConvertTo-Json -Depth 8), [Text.UTF8Encoding]::new($false))
            Move-Item -LiteralPath $temporary -Destination $destination -Force
        } catch { Write-Warning ('Resource collection failed: ' + $_.Exception.GetType().Name) }
        if (-not $Once) { Start-Sleep -Seconds 10 }
    } while (-not $Once)
} finally { $mutex.ReleaseMutex(); $mutex.Dispose() }

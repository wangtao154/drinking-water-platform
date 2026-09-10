# 系统资源采集器

`collect.ps1` 在 Windows 主机上采集主机 CPU、内存、磁盘和 Docker 容器指标，并写入 `data/snapshot.json`。`system-service` 以只读方式读取该快照，同时查询 Redis 指标，供后台“系统资源”页面展示。

## 运行方式

- `install.ps1` 创建名为 `DrinkingWater-ResourceMonitor` 的当前用户计划任务，并立即启动。
- 计划任务每轮采集完成后等待 10 秒；Docker Desktop 和当前用户会话需要保持可用。
- 页面每 10 秒刷新一次；系统服务在内存中保留最近 30 分钟趋势，重启系统服务后趋势会重新开始累计。

## 指标说明

- 主机内存使用率基于 Windows 主机总内存；容器内存使用率基于 Docker 报告的容器可用上限，两者不能直接相加比较。
- 容器网络与磁盘 I/O 是 Docker 自容器启动以来的累计值；重启次数为 Docker 的重启计数。
- Redis 未设置 `maxmemory` 时会显示“未设置”，不会将其误显示为零容量；命中率为 Redis 启动以来的累计值。
- 超过 45 秒未更新的主机快照会在页面标记为“已过期”。

## 维护

使用管理员 PowerShell 可查询任务状态：

```powershell
Get-ScheduledTask -TaskName DrinkingWater-ResourceMonitor
```

若需要停止采集，可停止该计划任务；若需要移除，可使用 Windows 任务计划程序删除同名任务。快照中不记录 Docker 挂载、环境变量或任何账号密码。

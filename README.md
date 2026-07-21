# 直饮水平台开发

> 基于 Spring Boot 3.2 + Spring Cloud Alibaba 的直饮水物联网微服务平台

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3.2 + Spring Cloud Alibaba 2023 + MyBatis-Plus 3.5 |
| 前端 | Vue 3 + Element Plus + Pinia + Vite |
| 移动端 | 微信小程序原生 |
| 数据库 | MySQL 8.0 + InfluxDB 2.7 + Redis 7 |
| 消息 | RabbitMQ 3.12 + EMQX 5.3 (MQTT) |
| 基础设施 | Docker Compose + Nacos + MinIO + Sentinel |

## 快速启动

### 1. 启动基础设施

```bash
# 复制环境变量配置
cp .env.example .env

# 修改密码（可选，默认密码已可用）
# vi .env

# 启动全部中间件
docker compose up -d
```

启动后可访问以下管理界面：

| 服务 | 地址 | 默认账号 |
|------|------|---------|
| MySQL | localhost:3306 | root / .env 中的密码 |
| Redis | localhost:6379 | .env 中的密码 |
| InfluxDB | http://localhost:8086 | admin / .env 中的密码 |
| RabbitMQ | http://localhost:15672 | admin / .env 中的密码 |
| EMQX | http://localhost:18083 | admin / EMQX@2026 |
| MinIO | http://localhost:9001 | admin / .env 中的密码 |
| Nacos | http://localhost:8848/nacos | nacos / .env 中的密码 |
| Sentinel | http://localhost:8858 | sentinel / sentinel |

### 2. 数据库初始化

MySQL 容器首次启动时自动执行 `sql/init.sql`，包含全部 33 张表和种子数据。

### 3. 构建公共模块

```bash
# 需要 JDK 17+ 和 Maven 3.8+
mvn clean install -pl platform-common
```

## 项目结构

```
直饮水平台开发/
├── docker-compose.yml          # 基础设施编排
├── .env.example                # 环境变量模板
├── pom.xml                     # Maven 父 POM
├── sql/
│   └── init.sql                # 数据库初始化脚本（33 张表 + 种子数据）
├── docker/
│   ├── mysql/my.cnf            # MySQL 配置
│   ├── influxdb/config.yml     # InfluxDB 配置
│   ├── rabbitmq/definitions.json # RabbitMQ Exchange/Queue 定义
│   └── emqx/emqx.conf          # EMQX MQTT Broker 配置
├── platform-common/            # 公共基础设施模块（P0 已完成）
│   └── src/main/java/com/platform/common/
│       ├── base/               # BaseEntity, BaseController, BaseMapperPlus
│       ├── result/             # R, PageResult, ResultCode
│       ├── exception/          # BusinessException, GlobalExceptionHandler
│       ├── auth/               # JwtUtil, UserContext, CurrentUser
│       ├── config/             # MybatisPlus, Redis, RabbitMQ, Jackson 配置
│       ├── enums/              # 13 个业务枚举 + PointMapping
│       ├── dto/                # PageQueryDTO, DeviceActivatedEvent
│       └── util/               # SnowflakeIdUtil, BigDecimalUtil, QRCodeUtil
├── 直饮水平台详细设计文档.md     # 设计文档 V3.0
└── 直饮水平台详细设计文档.html   # 设计文档 HTML 版
```

## 后台账号

| 账号 | 密码 | 角色 |
|------|------|------|
| admin | admin123 | 超级管理员 |

## 开发路线图

| 阶段 | 内容 | 状态 |
|------|------|------|
| P0 | 基础设施 + 数据库 + 公共模块 | ✅ 完成 |
| P1 | auth + device + user + iot（核心设备链路） | 待开始 |
| P2 | filter + worker-order + dealer + package | 待开始 |
| P3 | order + payment + finance | 待开始 |
| P4 | monitor + report + inventory + system + push | 待开始 |
| P5 | Vue 3 管理后台 | 待开始 |
| P6 | 3 个微信小程序 | 待开始 |

---
id: rbac-and-audit
title: 角色权限、账户管理与审计日志
scope: 系统管理、角色管理、账户管理、审计日志
permissions: [SYSTEM_USER, SYSTEM_ROLE, SYSTEM_AUDIT]
sources: [admin-web/src/views/system/roles.vue, admin-web/src/views/system/users.vue, admin-web/src/views/system/auditLogs.vue, /api/v1/system/**, .workbuddy/memory/MEMORY.md]
updatedAt: 2026-08-20
---

# 角色权限、账户管理与审计日志

## 角色与权限

1. 角色决定用户能够看到的菜单和可以调用的后台接口。
2. 新增或编辑账户时必须选择一个存在且启用的角色；用户名必须唯一。
3. 权限树保存时以实际勾选的叶子节点为准。父菜单的半选状态只表示子项部分选中，不能作为额外权限保存。
4. 权限变更后，相关用户需要重新登录或刷新用户信息后才会生效。

## 审计日志

1. 审计日志用于追踪后台敏感操作和重要配置变更。
2. 查询审计日志需要对应的审计权限；日志应包含操作人、操作时间、模块、动作与必要摘要。
3. AI 助手的调用也应记录用户、使用工具、权限判定、数据范围、模型和耗时，但不得记录密钥或敏感原文。

## AI 助手边界

1. 可解释权限不足原因、角色与权限的关系、审计日志用途。
2. 不可创建、删除或修改账户、角色、权限和系统配置。
3. 没有 `SYSTEM_AUDIT` 权限时，不允许通过 AI 查询审计日志内容。

## 变更记录

- 2026-08-20：建立第一期知识条目。

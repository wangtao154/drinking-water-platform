package com.platform.ai.assistant.tool;

/**
 * Whitelisted read-only data tools. Each one owns the business permission it
 * needs before an internal service request can be made.
 */
public enum AssistantReadTool {
    DEVICE("设备概况", "DEVICE_VIEW"),
    DEVICE_PRODUCTION("设备制水统计", "DEVICE_VIEW"),
    ALERTS("告警统计", "MONITOR_VIEW"),
    WORK_ORDERS("工单统计", "WORK_ORDER_VIEW"),
    SCAN_ORDERS("扫码订单统计", "ORDER_SCAN"),
    DASHBOARD("平台仪表盘", "REPORT_VIEW"),
    ORDER_REPORT("订单报表", "REPORT_VIEW");

    private final String title;
    private final String permission;

    AssistantReadTool(String title, String permission) {
        this.title = title;
        this.permission = permission;
    }

    public String title() {
        return title;
    }

    public String permission() {
        return permission;
    }
}

package com.platform.ai.assistant.tool;

/**
 * Fixed function-calling schemas handed to the model. The model may only pick
 * one of these names and fill the declared parameters; request URLs, service
 * tokens and credentials are never exposed to it.
 *
 * <p>Semantic extraction (which tool, which device, which time range) is the
 * model's job. Regular expressions are used downstream only to validate the
 * shape of the returned parameters, never to understand the question.</p>
 */
public final class AssistantToolSchemas {

    private AssistantToolSchemas() {
    }

    /**
     * One validated tool invocation parsed from the model's tool_calls.
     *
     * @param toolName   whitelisted tool name
     * @param deviceId   optional device id / control-board SN (format-validated)
     * @param startTime  optional inclusive start (model-provided ISO local time)
     * @param endTime    optional exclusive end (model-provided ISO local time)
     * @param detail     whether the user asked for secondary metrics
     */
    public record ToolInvocation(String toolName, String deviceId,
                                 java.time.LocalDateTime startTime,
                                 java.time.LocalDateTime endTime, boolean detail) {
    }

    public static final String TOOLS_JSON = """
            [
              {
                "type": "function",
                "function": {
                  "name": "device_snapshot",
                  "description": "查询设备概况。用户提到具体设备时查单台设备状态；未提具体设备时查平台设备统计",
                  "parameters": {
                    "type": "object",
                    "properties": {
                      "device_id": {"type": "string", "description": "设备ID或控制板SN，如 0002、j044331。用户未指定具体设备时不传"},
                      "start_time": {"type": "string", "description": "统计起始时间，格式 yyyy-MM-ddTHH:mm:ss"},
                      "end_time": {"type": "string", "description": "统计结束时间，格式 yyyy-MM-ddTHH:mm:ss"},
                      "detail": {"type": "boolean", "description": "用户要求详细/全部/明细指标时为 true"}
                    }
                  }
                }
              },
              {
                "type": "function",
                "function": {
                  "name": "device_production",
                  "description": "查询单台设备的制水统计（制水时段数、时长、纯水产量，以及废水、TDS、膜压力等详细指标）。必须提供 device_id",
                  "parameters": {
                    "type": "object",
                    "properties": {
                      "device_id": {"type": "string", "description": "设备ID或控制板SN，如 0002、j044331"},
                      "start_time": {"type": "string", "description": "起始时间，格式 yyyy-MM-ddTHH:mm:ss。用户未指定时间时不传，默认近24小时"},
                      "end_time": {"type": "string", "description": "结束时间，格式 yyyy-MM-ddTHH:mm:ss"},
                      "detail": {"type": "boolean", "description": "用户询问废水、TDS、水质、压力、流量等详细指标时为 true"}
                    },
                    "required": ["device_id"]
                  }
                }
              },
              {
                "type": "function",
                "function": {
                  "name": "alerts",
                  "description": "查询告警统计（总数、未处理数等）",
                  "parameters": {
                    "type": "object",
                    "properties": {
                      "start_time": {"type": "string", "description": "统计起始时间，格式 yyyy-MM-ddTHH:mm:ss。用户未指定时间时不传"},
                      "end_time": {"type": "string", "description": "统计结束时间，格式 yyyy-MM-ddTHH:mm:ss"},
                      "detail": {"type": "boolean", "description": "用户询问已处理、预警级、报警级、已推送等分项时为 true"}
                    }
                  }
                }
              },
              {
                "type": "function",
                "function": {
                  "name": "work_orders",
                  "description": "查询工单统计（总数、待处理、处理中、已完成等）",
                  "parameters": {
                    "type": "object",
                    "properties": {
                      "start_time": {"type": "string", "description": "统计起始时间，格式 yyyy-MM-ddTHH:mm:ss。用户未指定时间时不传"},
                      "end_time": {"type": "string", "description": "统计结束时间，格式 yyyy-MM-ddTHH:mm:ss"},
                      "detail": {"type": "boolean", "description": "用户询问派单、接单、取消、平均耗时等分项时为 true"}
                    }
                  }
                }
              },
              {
                "type": "function",
                "function": {
                  "name": "scan_orders",
                  "description": "查询扫码取水订单统计（已支付笔数、收入、取水量）",
                  "parameters": {
                    "type": "object",
                    "properties": {
                      "start_time": {"type": "string", "description": "按支付时间统计的起始时间，格式 yyyy-MM-ddTHH:mm:ss。用户未指定时间时不传"},
                      "end_time": {"type": "string", "description": "按支付时间统计的结束时间，格式 yyyy-MM-ddTHH:mm:ss"},
                      "detail": {"type": "boolean", "description": "用户询问退款、待支付、下发、命令状态等分项时为 true"}
                    }
                  }
                }
              },
              {
                "type": "function",
                "function": {
                  "name": "dashboard",
                  "description": "查询平台仪表盘经营概况（设备、客户、订单、告警等汇总）",
                  "parameters": {
                    "type": "object",
                    "properties": {
                      "start_time": {"type": "string", "description": "统计起始时间，格式 yyyy-MM-ddTHH:mm:ss。用户未指定时间时不传"},
                      "end_time": {"type": "string", "description": "统计结束时间，格式 yyyy-MM-ddTHH:mm:ss"},
                      "detail": {"type": "boolean", "description": "用户询问离线设备、客户数、滤芯、运维、经销商等分项时为 true"}
                    }
                  }
                }
              },
              {
                "type": "function",
                "function": {
                  "name": "order_report",
                  "description": "查询订单报表统计（订单总数、已支付、总收入）。注意：用户提到扫码取水时应改用 scan_orders",
                  "parameters": {
                    "type": "object",
                    "properties": {
                      "start_time": {"type": "string", "description": "统计起始时间，格式 yyyy-MM-ddTHH:mm:ss。用户未指定时间时不传"},
                      "end_time": {"type": "string", "description": "统计结束时间，格式 yyyy-MM-ddTHH:mm:ss"},
                      "detail": {"type": "boolean", "description": "用户询问待支付、取消、退款、今日订单等分项时为 true"}
                    }
                  }
                }
              }
            ]
            """;
}

package com.platform.ai.assistant.tool;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Fast local gate for the costly tool-routing model call. It only decides
 * whether live data may be needed; the model still selects the exact tool and
 * the read-tool layer still enforces permissions and argument validation.
 */
@Component
public class AdminAssistantQueryIntentClassifier {

    private static final Pattern EXPLICIT_DATA_QUERY = Pattern.compile(
            "(查询|查一下|帮我查|查找|看看|统计|汇总|分析|对比|多少|几笔|金额|收入|取水量|制水量|产水量|耗时|趋势|明细|详情|实时|当前|在线|离线|历史数据|各项指标)");
    private static final Pattern TIME_RANGE = Pattern.compile(
            "(今天|今日|昨天|昨日|本周|上周|本月|上月|(?:最)?近\\s*[一二三四五六七八九十百\\d]+\\s*(?:小时|天|日|周|月))");
    private static final Pattern DEVICE_METRIC = Pattern.compile(
            "((?:设备|SN)\\s*[A-Za-z0-9_-]+.*(?:TDS|压力|流量|制水|产水|状态)|(?:TDS|压力|流量|制水|产水|状态).*(?:设备|SN)\\s*[A-Za-z0-9_-]+)",
            Pattern.CASE_INSENSITIVE);

    public boolean requiresDataTools(String question) {
        if (question == null || question.isBlank()) {
            return false;
        }
        return EXPLICIT_DATA_QUERY.matcher(question).find()
                || TIME_RANGE.matcher(question).find()
                || DEVICE_METRIC.matcher(question).find();
    }
}

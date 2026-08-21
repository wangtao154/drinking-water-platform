package com.platform.common.enums;

import java.util.*;

/**
 * IoT 测点映射常量
 * P 系列（实时值）和 Q 系列（设定、校准及心跳参数）
 * 对应设计文档 3.15.2 节
 */
public final class PointMapping {

    private PointMapping() {
    }

    // ==================== P 系列测点 → 中文名称 ====================

    public static final Map<String, String> P_NAME = Map.ofEntries(
            Map.entry("P1", "原水TDS"),
            Map.entry("P2", "纯水TDS"),
            Map.entry("P3", "矿水TDS"),
            Map.entry("P4", "原水温度"),
            Map.entry("P5", "纯水温度"),
            Map.entry("P6", "矿水温度"),
            Map.entry("P7", "纯水瞬时流量"),
            Map.entry("P8", "净水瞬时流量"),
            Map.entry("P9", "矿水瞬时流量"),
            Map.entry("P10", "废水瞬时流量"),
            Map.entry("P11", "原水瞬时流量"),
            Map.entry("P12", "纯水累计流量"),
            Map.entry("P13", "净水累计流量"),
            Map.entry("P14", "矿水累计流量"),
            Map.entry("P15", "废水累计流量"),
            Map.entry("P16", "原水累计流量"),
            Map.entry("P17", "原水压力"),
            Map.entry("P18", "膜前压力"),
            Map.entry("P19", "膜后压力"),
            Map.entry("P20", "矿水压力"),
            Map.entry("P21", "比例阀开度1"),
            Map.entry("P22", "比例阀开度2"),
            Map.entry("P23", "制水状态"),
            Map.entry("P24", "TDS制水状态"),
            Map.entry("P25", "RO强冲状态"),
            Map.entry("P26", "纯水洗膜状态"),
            Map.entry("P27", "超滤冲洗状态"),
            Map.entry("P28", "故障报警"),
            Map.entry("P29", "比例阀状态"),
            Map.entry("P30", "预留3"),
            Map.entry("P31", "高压开关"),
            Map.entry("P32", "低压开关"),
            Map.entry("P33", "漏水状态"),
            Map.entry("P34", "电压低检测"),
            Map.entry("P35", "预留4"),
            Map.entry("P36", "预留5"),
            Map.entry("P37", "预留6"),
            Map.entry("P38", "DI1调试"),
            Map.entry("P39", "DI2调试")
    );

    // ==================== Q 系列测点 → 中文名称 ====================

    public static final Map<String, String> Q_NAME = Map.ofEntries(
            Map.entry("Q1", "从站地址"),
            Map.entry("Q2", "波特率"),
            Map.entry("Q3", "TDS1设定值"),
            Map.entry("Q4", "TDS1差值"),
            Map.entry("Q5", "RO强冲时间"),
            Map.entry("Q6", "RO强冲间隔"),
            Map.entry("Q7", "超滤排污时间"),
            Map.entry("Q8", "超滤排污间隔"),
            Map.entry("Q9", "纯水洗膜时间"),
            Map.entry("Q10", "恒TDS制水命令"),
            Map.entry("Q11", "纯水洗膜开启"),
            Map.entry("Q12", "RO膜强冲"),
            Map.entry("Q13", "超滤冲洗"),
            Map.entry("Q14", "TDS校准"),
            Map.entry("Q15", "调试命令"),
            Map.entry("Q26", "IP地址1"),
            Map.entry("Q27", "IP地址2"),
            Map.entry("Q28", "IP地址3"),
            Map.entry("Q29", "IP地址4"),
            Map.entry("Q30", "IMEI"),
            Map.entry("Q31", "年"),
            Map.entry("Q32", "月"),
            Map.entry("Q33", "日"),
            Map.entry("Q75", "原水压力零点"),
            Map.entry("Q76", "膜前压力零点"),
            Map.entry("Q77", "膜后压力零点"),
            Map.entry("Q78", "矿水压力零点"),
            Map.entry("Q79", "原水压力斜率点"),
            Map.entry("Q80", "膜前压力斜率点"),
            Map.entry("Q81", "膜后压力斜率点"),
            Map.entry("Q82", "矿水压力斜率点"),
            Map.entry("Q83", "压力标压"),
            Map.entry("Q84", "压力校准模式"),
            Map.entry("Q85", "纯水K系数"),
            Map.entry("Q86", "净水K系数"),
            Map.entry("Q87", "矿水K系数"),
            Map.entry("Q88", "废水K系数"),
            Map.entry("Q89", "流量校准模式"),
            Map.entry("Q90", "流量标准水量"),
            Map.entry("Q91", "TDS标液浓度"),
            Map.entry("Q92", "TDS校准模式"),
            Map.entry("Q93", "TDS1零点"),
            Map.entry("Q94", "TDS2零点"),
            Map.entry("Q95", "TDS3零点"),
            Map.entry("Q96", "TDS温度补偿系数"),
            Map.entry("Q97", "TDS修正系数K"),
            Map.entry("Q102", "心跳检测")
    );

    // ==================== 报警相关测点 ====================

    public static final List<String> ALERT_POINTS = List.of(
            "P28",  // 故障报警
            "P32",  // 低压开关（缺水）
            "P33",  // 漏水状态
            "P34"   // 电压低检测
    );

    // ==================== 需转换单位的测点 ====================

    public static final Set<String> PRESSURE_POINTS = Set.of(
            "P17", "P18", "P19", "P20"          // 压力 bar ×100
    );

    public static final Set<String> DIVIDE_BY_10 = Set.of(
            "P7", "P8", "P9", "P10", "P11"     // 瞬时流量 L/min ×10
    );

    public static final Set<String> DIVIDE_BY_100 = Set.of(
            "P17", "P18", "P19", "P20"          // 压力 bar ×100
    );

    // ==================== 累计流量测点（用于滤芯寿命计算）====================

    public static final Set<String> CUMULATIVE_FLOW_POINTS = Set.of(
            "P12",  // 纯水累计流量
            "P13",  // 净水累计流量
            "P14",  // 矿水累计流量
            "P15",  // 废水累计流量
            "P16"   // 原水累计流量
    );

    // ==================== 常用下发测点 ====================

    public static final Map<String, String> COMMON_SET_POINTS = Map.ofEntries(
            Map.entry("Q3", "TDS1设定值"),
            Map.entry("Q4", "TDS1差值"),
            Map.entry("Q5", "RO强冲时间"),
            Map.entry("Q6", "RO强冲间隔"),
            Map.entry("Q7", "超滤排污时间"),
            Map.entry("Q8", "超滤排污间隔"),
            Map.entry("Q9", "纯水洗膜时间"),
            Map.entry("Q10", "恒TDS制水命令"),
            Map.entry("Q11", "纯水洗膜开启"),
            Map.entry("Q12", "RO膜强冲"),
            Map.entry("Q13", "超滤冲洗"),
            Map.entry("Q14", "TDS校准")
    );

    /**
     * 获取测点中文名称
     */
    public static String getName(String pointId) {
        String name = P_NAME.get(pointId);
        if (name == null) {
            name = Q_NAME.get(pointId);
        }
        return name != null ? name : pointId;
    }

    /**
     * 判断是否为 P 系列测点
     */
    public static boolean isPSeries(String pointId) {
        return pointId != null && pointId.startsWith("P");
    }

    /**
     * 判断是否为 Q 系列测点
     */
    public static boolean isQSeries(String pointId) {
        return pointId != null && pointId.startsWith("Q");
    }

    /**
     * 判断是否需要 ÷10 转换单位
     */
    public static boolean needsDivideBy10(String pointId) {
        return DIVIDE_BY_10.contains(pointId);
    }

    /**
     * 判断是否需要 ÷100 转换单位
     */
    public static boolean needsDivideBy100(String pointId) {
        return DIVIDE_BY_100.contains(pointId);
    }

    /**
     * 判断是否为压力测点
     */
    public static boolean isPressurePoint(String pointId) {
        return PRESSURE_POINTS.contains(pointId);
    }

    /**
     * 将设备上报的原始数值转换为业务实际值
     */
    public static double normalizeValue(String pointId, double rawValue) {
        if (needsDivideBy100(pointId)) {
            return rawValue / 100.0;
        }
        if (needsDivideBy10(pointId)) {
            return rawValue / 10.0;
        }
        return rawValue;
    }
}

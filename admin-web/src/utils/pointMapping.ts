/**
 * IoT 测点映射 - P系列(实时值) 和 Q系列(配置参数)
 * 对应后端 platform-common PointMapping.java
 */

export interface PointInfo {
  name: string
  unit: string
  category: PointCategory
}

export type PointCategory = 'quality' | 'flow' | 'pressure' | 'valve' | 'status' | 'config'

export const CATEGORY_LABELS: Record<PointCategory, string> = {
  quality: '水质参数',
  flow: '流量数据',
  pressure: '压力数据',
  valve: '阀门状态',
  status: '运行状态',
  config: '配置参数',
}

const pointMap: Record<string, PointInfo> = {
  // 水质参数
  P1:  { name: '原水TDS',    unit: 'PPM', category: 'quality' },
  P2:  { name: '纯水TDS',    unit: 'PPM', category: 'quality' },
  P3:  { name: '矿水TDS',    unit: 'PPM', category: 'quality' },
  P4:  { name: '原水温度',   unit: '℃',   category: 'quality' },
  P5:  { name: '纯水温度',   unit: '℃',   category: 'quality' },
  P6:  { name: '矿水温度',   unit: '℃',   category: 'quality' },

  // 流量数据
  P7:  { name: '纯水瞬时流量', unit: 'L/min', category: 'flow' },
  P8:  { name: '净水瞬时流量', unit: 'L/min', category: 'flow' },
  P9:  { name: '矿水瞬时流量', unit: 'L/min', category: 'flow' },
  P10: { name: '废水瞬时流量', unit: 'L/min', category: 'flow' },
  P11: { name: '原水瞬时流量', unit: 'L/min', category: 'flow' },
  P12: { name: '纯水累计流量', unit: 'L',     category: 'flow' },
  P13: { name: '净水累计流量', unit: 'L',     category: 'flow' },
  P14: { name: '矿水累计流量', unit: 'L',     category: 'flow' },
  P15: { name: '废水累计流量', unit: 'L',     category: 'flow' },
  P16: { name: '原水累计流量', unit: 'L',     category: 'flow' },

  // 压力数据
  P17: { name: '原水压力', unit: 'bar', category: 'pressure' },
  P18: { name: '膜前压力', unit: 'bar', category: 'pressure' },
  P19: { name: '膜后压力', unit: 'bar', category: 'pressure' },
  P20: { name: '矿水压力', unit: 'bar', category: 'pressure' },

  // 阀门状态
  P21: { name: '比例阀开度1', unit: '%',  category: 'valve' },
  P22: { name: '比例阀开度2', unit: '%',  category: 'valve' },
  P29: { name: '比例阀状态',   unit: '',   category: 'valve' },

  // 运行状态
  P23: { name: '制水状态',       unit: '', category: 'status' },
  P24: { name: 'TDS制水状态',    unit: '', category: 'status' },
  P25: { name: 'RO强冲状态',     unit: '', category: 'status' },
  P26: { name: '纯水洗膜状态',   unit: '', category: 'status' },
  P27: { name: '超滤冲洗状态',   unit: '', category: 'status' },
  P28: { name: '故障报警',       unit: '', category: 'status' },
  P30: { name: '预留3',          unit: '', category: 'status' },
  P31: { name: '高压开关',       unit: '', category: 'status' },
  P32: { name: '低压开关',       unit: '', category: 'status' },
  P33: { name: '漏水状态',       unit: '', category: 'status' },
  P34: { name: '电压低检测',     unit: '', category: 'status' },
  P35: { name: '预留4',          unit: '', category: 'status' },
  P36: { name: '预留5',          unit: '', category: 'status' },
  P37: { name: '预留6',          unit: '', category: 'status' },
  P38: { name: 'DI1调试',        unit: '', category: 'status' },
  P39: { name: 'DI2调试',        unit: '', category: 'status' },

  // Q系列配置参数
  Q1:  { name: '从站地址',       unit: '',   category: 'config' },
  Q2:  { name: '波特率',         unit: '',   category: 'config' },
  Q3:  { name: 'TDS1设定值',     unit: 'PPM', category: 'config' },
  Q4:  { name: 'TDS1差值',       unit: 'PPM', category: 'config' },
  Q5:  { name: 'RO强冲时间',     unit: 's',   category: 'config' },
  Q6:  { name: 'RO强冲间隔',     unit: 'min', category: 'config' },
  Q7:  { name: '超滤排污时间',   unit: 's',   category: 'config' },
  Q8:  { name: '超滤排污间隔',   unit: 'min', category: 'config' },
  Q9:  { name: '纯水洗膜时间',   unit: 's',   category: 'config' },
  Q10: { name: '恒TDS制水命令',  unit: '',    category: 'config' },
  Q11: { name: '纯水洗膜开启',   unit: '',    category: 'config' },
  Q12: { name: 'RO膜强冲',       unit: '',    category: 'config' },
  Q13: { name: '超滤冲洗',       unit: '',    category: 'config' },
  Q14: { name: 'TDS校准',        unit: '',    category: 'config' },
  Q15: { name: '调试命令',       unit: '',    category: 'config' },
  Q26: { name: 'IP地址1',        unit: '',    category: 'config' },
  Q27: { name: 'IP地址2',        unit: '',    category: 'config' },
  Q28: { name: 'IP地址3',        unit: '',    category: 'config' },
  Q29: { name: 'IP地址4',        unit: '',    category: 'config' },
  Q30: { name: 'IMEI',           unit: '',    category: 'config' },
  Q31: { name: '年',             unit: '',    category: 'config' },
  Q32: { name: '月',             unit: '',    category: 'config' },
  Q33: { name: '日',             unit: '',    category: 'config' },
  // Q34~Q53: MQTT服务器地址（uint16[20]，最长40字符）
  Q34: { name: 'MQTT服务器地址',  unit: '',    category: 'config' },
  // Q54: MQTT端口
  Q54: { name: 'MQTT端口',       unit: '',    category: 'config' },
  Q74: { name: '扫码出水信息',       unit: '',    category: 'config' },
  Q75: { name: '原水压力零点',      unit: 'ADC',       category: 'config' },
  Q76: { name: '膜前压力零点',      unit: 'ADC',       category: 'config' },
  Q77: { name: '膜后压力零点',      unit: 'ADC',       category: 'config' },
  Q78: { name: '矿水压力零点',      unit: 'ADC',       category: 'config' },
  Q79: { name: '原水压力斜率点',    unit: 'ADC',       category: 'config' },
  Q80: { name: '膜前压力斜率点',    unit: 'ADC',       category: 'config' },
  Q81: { name: '膜后压力斜率点',    unit: 'ADC',       category: 'config' },
  Q82: { name: '矿水压力斜率点',    unit: 'ADC',       category: 'config' },
  Q83: { name: '压力标压',          unit: '0.01bar',   category: 'config' },
  Q84: { name: '压力校准模式',      unit: '',          category: 'config' },
  Q85: { name: '纯水K系数',         unit: 'x10脉冲/升', category: 'config' },
  Q86: { name: '净水K系数',         unit: 'x10脉冲/升', category: 'config' },
  Q87: { name: '矿水K系数',         unit: 'x10脉冲/升', category: 'config' },
  Q88: { name: '废水K系数',         unit: 'x10脉冲/升', category: 'config' },
  Q89: { name: '流量校准模式',      unit: '',          category: 'config' },
  Q90: { name: '流量标准水量',      unit: 'x10(0.1L)', category: 'config' },
  Q91: { name: 'TDS标液浓度',       unit: 'ppm',       category: 'config' },
  Q92: { name: 'TDS校准模式',       unit: '',          category: 'config' },
  Q93: { name: 'TDS1零点',          unit: 'ppm',       category: 'config' },
  Q94: { name: 'TDS2零点',          unit: 'ppm',       category: 'config' },
  Q95: { name: 'TDS3零点',          unit: 'ppm',       category: 'config' },
  Q96: { name: 'TDS温度补偿系数',   unit: 'x10000',    category: 'config' },
  Q97: { name: 'TDS修正系数K',      unit: 'x1000',     category: 'config' },
  Q102: { name: '心跳检测',         unit: '',          category: 'config' },
}

export function getPointInfo(pointId: string): PointInfo {
  return pointMap[pointId] || { name: pointId, unit: '', category: 'status' }
}

export function formatPointValue(pointId: string, value: number | string): string {
  if (typeof value === 'string') {
    return value
  }
  if (value === 0 || value === 1) {
    // Binary status points - show on/off
    const binaryPoints = ['P23', 'P24', 'P25', 'P26', 'P27', 'P28', 'P29', 'P31', 'P32', 'P33', 'P34']
    if (binaryPoints.includes(pointId)) {
      return value === 1 ? '开' : '关'
    }
  }
  // Format numbers with appropriate precision
  if (value % 1 === 0) {
    return value.toString()
  }
  return value.toFixed(2)
}

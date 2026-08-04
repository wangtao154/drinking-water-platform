import service from '@/utils/request'

const BASE = '/v1/ai'

export interface AiMetricVO {
  field: string
  name: string
  unit: string
  count: number
  first: number | null
  last: number | null
  min: number | null
  max: number | null
  avg: number | null
  delta: number | null
}

export interface AiAdviceVO {
  summary: string
  reasons?: string[]
  recommendedActions?: string[]
  confidence?: string
}

export interface QwenAdviceVO {
  status: string
  model?: string
  summary?: string
  riskLevel?: string
  maintenancePriority?: string
  recommendedActions?: string[]
  reasoning?: string
  errorMessage?: string | null
}

export interface RoMembranePredictionVO {
  sn: string
  deviceId?: string
  modelName?: string
  online?: boolean
  lifecycleStatus?: string
  activatedAt?: string
  rangeDays: number
  aggregateEvery: string
  ratedPureLiters: number
  dataPointCount: number
  dataStatus: string
  healthScore: number
  riskLevel: string
  estimatedRemainingLiters: number
  estimatedRemainingDays: number
  dailyPureLiters: number
  desalinationRate: number
  wastewaterRatio: number
  membranePressureDiff: number
  waterProducing?: boolean
  pressureAssessment?: string
  pressureAssessmentMessage?: string
  metrics: Record<string, AiMetricVO>
  ruleAdvice: AiAdviceVO
  qwenAdvice: QwenAdviceVO
}

export interface RoMembranePredictionParams {
  sn: string
  rangeDays?: number
  aggregateEvery?: string
  ratedPureLiters?: number
}

export function predictRoMembrane(params: RoMembranePredictionParams) {
  return service({
    method: 'GET',
    url: `${BASE}/ro-membrane/predict`,
    params,
    timeout: 180000
  }) as unknown as Promise<{ code: number; message: string; data: RoMembranePredictionVO; timestamp: number }>
}

import service from '@/utils/request'

const BASE = '/v1/ai'
const ADMIN_ASSISTANT_REQUEST_TIMEOUT_MS = 120000

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
  healthScore?: number
  summary?: string
  riskLevel?: string
  estimatedRemainingLiters?: number
  estimatedRemainingDays?: number
  maintenancePriority?: string
  confidence?: string
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
  productionSessionCount?: number
  stableProductionSessionCount?: number
  productionSampleCount?: number
  productionDataConfidence?: string
  dataCoverageDays?: number
  accumulatedPureLiters?: number
  observedPureLiters?: number
  observedWasteLiters?: number
  predictionSource?: string
  predictionFallbackReason?: string
  healthScore: number
  riskLevel: string
  estimatedRemainingLiters: number
  estimatedRemainingDays: number
  dailyPureLiters: number
  productionRawTds?: number
  productionPureTds?: number
  desalinationRate: number
  wastewaterRatio: number
  membranePressureDiff: number
  productionMembraneBefore?: number
  productionMembraneAfter?: number
  productionPureFlow?: number
  membraneBeforeChangePercent?: number
  membraneAfterChangePercent?: number
  pureFlowChangePercent?: number
  pressureFoulingSuspected?: boolean
  backpressureDropIgnored?: boolean
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

export interface AdminAssistantCitationVO {
  id: string
  title: string
  updatedAt?: string
  sources?: string[]
}

export interface AdminAssistantDataSourceVO {
  tool: string
  title: string
  status: 'OK' | 'DENIED' | 'UNAVAILABLE' | 'NOT_FOUND'
  summary: string
  dataRange: string
  queriedAt: string
  facts: Record<string, string | number>
}

export interface AdminAssistantChatResponse {
  requestId?: string
  conversationId?: string
  answer: string
  model?: string | null
  fallback: boolean
  notice: string
  citations: AdminAssistantCitationVO[]
  dataSources: AdminAssistantDataSourceVO[]
}

export function predictRoMembrane(params: RoMembranePredictionParams) {
  return service({
    method: 'GET',
    url: `${BASE}/ro-membrane/predict`,
    params,
    timeout: 180000
  }) as unknown as Promise<{ code: number; message: string; data: RoMembranePredictionVO; timestamp: number }>
}

export function chatWithAdminAssistant(question: string, conversationId?: string) {
  const data: Record<string, string> = { question }
  if (conversationId) {
    data.conversationId = conversationId
  }
  return service({
    method: 'POST',
    url: `${BASE}/assistant/chat`,
    data,
    timeout: ADMIN_ASSISTANT_REQUEST_TIMEOUT_MS
  }) as unknown as Promise<{ code: number; message: string; data: AdminAssistantChatResponse; timestamp: number }>
}

export function clearAdminAssistantConversation(conversationId: string) {
  return service({
    method: 'DELETE',
    url: `${BASE}/assistant/conversations/${encodeURIComponent(conversationId)}`,
    timeout: 10000
  }) as unknown as Promise<{ code: number; message: string; data: null; timestamp: number }>
}

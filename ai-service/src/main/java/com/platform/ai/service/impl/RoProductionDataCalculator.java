package com.platform.ai.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Extracts RO membrane indicators from stable water-production periods only.
 */
final class RoProductionDataCalculator {

    private static final double FLOW_ACTIVE_THRESHOLD = 0.001D;
    private static final double COUNTER_INCREASE_THRESHOLD = 0.01D;
    private static final long SESSION_GAP_SECONDS = 300L;
    private static final long WARM_UP_SECONDS = 60L;
    private static final long MIN_STABLE_DURATION_SECONDS = 20L;
    private static final int MIN_STABLE_SAMPLES = 3;
    private static final int MIN_TREND_SESSIONS = 3;
    private static final double STABLE_RATIO = 0.85D;
    private static final double PRESSURE_RISE_PERCENT = 10D;
    private static final double FLOW_DECLINE_PERCENT = -10D;
    private static final double BACKPRESSURE_DROP_PERCENT = -15D;

    private RoProductionDataCalculator() {
    }

    static Analysis analyze(List<TelemetrySample> source) {
        if (source == null || source.isEmpty()) {
            return Analysis.empty();
        }

        List<TelemetrySample> samples = source.stream()
                .filter(sample -> sample != null && sample.time() != null)
                .sorted(Comparator.comparing(TelemetrySample::time))
                .toList();
        if (samples.isEmpty()) {
            return Analysis.empty();
        }

        boolean hasProductionStatus = samples.stream().anyMatch(sample -> finite(sample.productionStatus()));
        CounterProgress pureCounter = counterProgress(samples, true);
        CounterProgress wasteCounter = counterProgress(samples, false);
        List<List<TelemetrySample>> sessions = splitSessions(samples, hasProductionStatus);

        List<StableSession> stableSessions = sessions.stream()
                .map(RoProductionDataCalculator::stableSession)
                .filter(session -> session != null)
                .toList();

        int stableSampleCount = stableSessions.stream().mapToInt(StableSession::sampleCount).sum();
        Double before = median(stableSessions.stream().map(StableSession::before).filter(RoProductionDataCalculator::finite).toList());
        Double after = median(stableSessions.stream().map(StableSession::after).filter(RoProductionDataCalculator::finite).toList());
        Double pressureDiff = median(stableSessions.stream().map(StableSession::pressureDiff).filter(RoProductionDataCalculator::finite).toList());
        Double pureFlow = median(stableSessions.stream().map(StableSession::pureFlow).filter(RoProductionDataCalculator::positive).toList());
        Double rawTds = median(stableSessions.stream().map(StableSession::rawTds).filter(RoProductionDataCalculator::positive).toList());
        Double pureTds = median(stableSessions.stream().map(StableSession::pureTds).filter(RoProductionDataCalculator::positive).toList());
        SessionTrend trend = sessionTrend(stableSessions);

        double coverageDays = coverageDays(pureCounter, wasteCounter);
        String confidence = confidence(stableSessions.size(), stableSampleCount);
        String evidence = hasProductionStatus ? "P23_STATUS" : "FLOW_OR_COUNTER_DELTA";

        return new Analysis(
                !sessions.isEmpty(),
                sessions.size(),
                stableSessions.size(),
                stableSampleCount,
                hasProductionStatus,
                before,
                after,
                pressureDiff,
                pureFlow,
                trend.beforeChangePercent(),
                trend.afterChangePercent(),
                trend.flowChangePercent(),
                rawTds,
                pureTds,
                pureCounter.latestValue(),
                pureCounter.delta(),
                wasteCounter.delta(),
                coverageDays,
                confidence,
                evidence
        );
    }

    private static List<List<TelemetrySample>> splitSessions(List<TelemetrySample> samples,
                                                              boolean hasProductionStatus) {
        List<List<TelemetrySample>> sessions = new ArrayList<>();
        List<TelemetrySample> current = null;
        TelemetrySample previous = null;
        Instant lastActiveAt = null;

        for (TelemetrySample sample : samples) {
            boolean active;
            if (hasProductionStatus) {
                active = finite(sample.productionStatus()) && sample.productionStatus() >= 0.5D;
            } else {
                boolean flowActive = positive(sample.pureInstantFlow())
                        && sample.pureInstantFlow() > FLOW_ACTIVE_THRESHOLD;
                boolean counterIncreased = previous != null
                        && finite(previous.pureTotal())
                        && finite(sample.pureTotal())
                        && sample.pureTotal() - previous.pureTotal() > COUNTER_INCREASE_THRESHOLD;
                active = flowActive || counterIncreased;
            }

            boolean gapExceeded = lastActiveAt != null
                    && sample.time().isAfter(lastActiveAt.plusSeconds(SESSION_GAP_SECONDS));
            if (active) {
                if (current == null || gapExceeded) {
                    current = new ArrayList<>();
                    sessions.add(current);
                }
                current.add(sample);
                lastActiveAt = sample.time();
            } else if (hasProductionStatus && finite(sample.productionStatus()) && current != null) {
                current = null;
                lastActiveAt = null;
            }
            previous = sample;
        }
        return sessions;
    }

    private static StableSession stableSession(List<TelemetrySample> session) {
        if (session == null || session.isEmpty()) {
            return null;
        }
        Instant stableFrom = session.get(0).time().plusSeconds(WARM_UP_SECONDS);
        List<TelemetrySample> candidates = session.stream()
                .filter(sample -> !sample.time().isBefore(stableFrom))
                .filter(sample -> positive(sample.pureInstantFlow()))
                .filter(sample -> positive(sample.membraneBefore()))
                .filter(sample -> finite(sample.membraneAfter()))
                .filter(sample -> sample.membraneBefore() > sample.membraneAfter())
                .toList();
        if (candidates.size() < MIN_STABLE_SAMPLES) {
            return null;
        }

        Double medianFlow = median(candidates.stream().map(TelemetrySample::pureInstantFlow).toList());
        Double medianBefore = median(candidates.stream().map(TelemetrySample::membraneBefore).toList());
        if (!positive(medianFlow) || !positive(medianBefore)) {
            return null;
        }

        List<TelemetrySample> stable = candidates.stream()
                .filter(sample -> sample.pureInstantFlow() >= medianFlow * STABLE_RATIO)
                .filter(sample -> sample.membraneBefore() >= medianBefore * STABLE_RATIO)
                .toList();
        if (stable.size() < MIN_STABLE_SAMPLES) {
            return null;
        }
        long stableDuration = Duration.between(stable.get(0).time(), stable.get(stable.size() - 1).time()).getSeconds();
        if (stableDuration < MIN_STABLE_DURATION_SECONDS) {
            return null;
        }

        return new StableSession(
                stable.get(stable.size() - 1).time(),
                median(stable.stream().map(TelemetrySample::membraneBefore).filter(RoProductionDataCalculator::finite).toList()),
                median(stable.stream().map(TelemetrySample::membraneAfter).filter(RoProductionDataCalculator::finite).toList()),
                median(stable.stream()
                        .map(sample -> sample.membraneBefore() - sample.membraneAfter())
                        .filter(RoProductionDataCalculator::finite)
                        .filter(value -> value >= 0D)
                        .toList()),
                median(stable.stream().map(TelemetrySample::pureInstantFlow).filter(RoProductionDataCalculator::positive).toList()),
                median(stable.stream().map(TelemetrySample::rawTds).filter(RoProductionDataCalculator::positive).toList()),
                median(stable.stream().map(TelemetrySample::pureTds).filter(RoProductionDataCalculator::positive).toList()),
                stable.size()
        );
    }

    private static SessionTrend sessionTrend(List<StableSession> sessions) {
        if (sessions == null || sessions.size() < MIN_TREND_SESSIONS) {
            return SessionTrend.empty();
        }
        int groupSize = Math.max(1, Math.min(3, sessions.size() / 2));
        List<StableSession> baseline = sessions.subList(0, groupSize);
        List<StableSession> recent = sessions.subList(sessions.size() - groupSize, sessions.size());
        return new SessionTrend(
                changePercent(sessionMedian(baseline, StableSession::before), sessionMedian(recent, StableSession::before)),
                changePercent(sessionMedian(baseline, StableSession::after), sessionMedian(recent, StableSession::after)),
                changePercent(sessionMedian(baseline, StableSession::pureFlow), sessionMedian(recent, StableSession::pureFlow))
        );
    }

    private static Double sessionMedian(List<StableSession> sessions,
                                        java.util.function.Function<StableSession, Double> extractor) {
        return median(sessions.stream().map(extractor).filter(RoProductionDataCalculator::finite).toList());
    }

    private static Double changePercent(Double baseline, Double recent) {
        if (!positive(baseline) || !finite(recent)) {
            return null;
        }
        return (recent - baseline) / baseline * 100D;
    }

    static PressureEvaluation evaluatePressure(Analysis analysis) {
        if (analysis == null || !analysis.productionDetected()) {
            return new PressureEvaluation("IDLE_IGNORED",
                    "分析周期内未检测到制水记录，待机压力不参与RO膜判断", false, false);
        }
        if (!analysis.hasProductionPressure()) {
            return new PressureEvaluation("PRODUCING_PRESSURE_MISSING",
                    "检测到制水记录，但稳定制水段压力样本不足，待机压力不参与判断", false, false);
        }
        if (!analysis.hasPressureTrend()) {
            return new PressureEvaluation("PRODUCING_TREND_INSUFFICIENT",
                    "稳定制水会话不足，压差仅作展示，不据此判断RO膜堵塞", false, false);
        }

        boolean feedPressureRising = analysis.membraneBeforeChangePercent() >= PRESSURE_RISE_PERCENT;
        boolean pureFlowDeclining = analysis.pureFlowChangePercent() <= FLOW_DECLINE_PERCENT;
        boolean backpressureDropping = analysis.membraneAfterChangePercent() <= BACKPRESSURE_DROP_PERCENT;

        if (feedPressureRising && pureFlowDeclining) {
            return new PressureEvaluation("POSSIBLE_FOULING",
                    "稳定制水时膜前压力持续上升且纯水流量下降，存在RO膜堵塞或水路阻力升高迹象", true, false);
        }
        if (backpressureDropping && !feedPressureRising) {
            return new PressureEvaluation("BACKPRESSURE_DROP_IGNORED",
                    "压差增大主要来自膜后背压下降，膜前压力未升高，按储水桶放空工况处理，不判定RO膜堵塞", false, true);
        }
        if (feedPressureRising) {
            return new PressureEvaluation("FEED_PRESSURE_RISE_OBSERVED",
                    "膜前压力有所上升，但纯水流量未同步下降，暂不判定RO膜堵塞，建议继续观察", false, false);
        }
        if (pureFlowDeclining) {
            return new PressureEvaluation("FLOW_DECLINE_OBSERVED",
                    "纯水流量有所下降，但膜前压力未升高，暂不判定RO膜堵塞，建议检查用水和储水桶工况", false, false);
        }
        return new PressureEvaluation("PRODUCING_NORMAL",
                "稳定制水时膜前压力和纯水流量趋势正常，压差不单独作为RO膜堵塞依据", false, false);
    }

    private static CounterProgress counterProgress(List<TelemetrySample> samples, boolean pure) {
        Double previous = null;
        Double latest = null;
        double delta = 0D;
        Instant firstAt = null;
        Instant lastAt = null;

        for (TelemetrySample sample : samples) {
            Double value = pure ? sample.pureTotal() : sample.wasteTotal();
            if (!finite(value) || value < 0D) {
                continue;
            }
            if (firstAt == null) {
                firstAt = sample.time();
            }
            lastAt = sample.time();
            latest = value;
            if (previous == null) {
                previous = value;
                continue;
            }
            if (value >= previous) {
                delta += value - previous;
                previous = value;
                continue;
            }

            // A large drop to a near-zero value is treated as a counter reset.
            if (value <= Math.max(1D, previous * 0.2D)) {
                delta += value;
                previous = value;
            }
            // Small negative jumps are sensor jitter and do not move the baseline.
        }
        return new CounterProgress(latest, delta > 0D ? delta : null, firstAt, lastAt);
    }

    private static double coverageDays(CounterProgress pure, CounterProgress waste) {
        Instant first = earlier(pure.firstAt(), waste.firstAt());
        Instant last = later(pure.lastAt(), waste.lastAt());
        if (first == null || last == null || !last.isAfter(first)) {
            return 0D;
        }
        return Duration.between(first, last).toSeconds() / 86400D;
    }

    private static Instant earlier(Instant left, Instant right) {
        if (left == null) return right;
        if (right == null) return left;
        return left.isBefore(right) ? left : right;
    }

    private static Instant later(Instant left, Instant right) {
        if (left == null) return right;
        if (right == null) return left;
        return left.isAfter(right) ? left : right;
    }

    private static String confidence(int sessionCount, int sampleCount) {
        if (sessionCount >= 3 && sampleCount >= 30) {
            return "HIGH";
        }
        if (sessionCount >= 1 && sampleCount >= 5) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private static boolean finite(Double value) {
        return value != null && !value.isNaN() && !value.isInfinite();
    }

    private static boolean positive(Double value) {
        return finite(value) && value > 0D;
    }

    private static Double median(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<Double> sorted = values.stream().sorted().toList();
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return sorted.get(middle);
        }
        return (sorted.get(middle - 1) + sorted.get(middle)) / 2D;
    }

    record TelemetrySample(
            Instant time,
            Double rawTds,
            Double pureTds,
            Double pureInstantFlow,
            Double pureTotal,
            Double wasteTotal,
            Double membraneBefore,
            Double membraneAfter,
            Double productionStatus
    ) {
    }

    record Analysis(
            boolean productionDetected,
            int productionSessionCount,
            int stableSessionCount,
            int stableSampleCount,
            boolean hasProductionStatusData,
            Double productionMembraneBefore,
            Double productionMembraneAfter,
            Double productionPressureDiff,
            Double productionPureFlow,
            Double membraneBeforeChangePercent,
            Double membraneAfterChangePercent,
            Double pureFlowChangePercent,
            Double productionRawTds,
            Double productionPureTds,
            Double latestPureTotal,
            Double pureDelta,
            Double wasteDelta,
            double dataCoverageDays,
            String confidence,
            String evidence
    ) {
        static Analysis empty() {
            return new Analysis(false, 0, 0, 0, false, null, null, null, null,
                    null, null, null, null, null,
                    null, null, null, 0D, "LOW", null);
        }

        boolean hasProductionPressure() {
            return productionMembraneBefore != null
                    && productionMembraneAfter != null
                    && productionPressureDiff != null;
        }

        boolean hasPressureTrend() {
            return stableSessionCount >= MIN_TREND_SESSIONS
                    && membraneBeforeChangePercent != null
                    && membraneAfterChangePercent != null
                    && pureFlowChangePercent != null;
        }
    }

    private record StableSession(
            Instant endedAt,
            Double before,
            Double after,
            Double pressureDiff,
            Double pureFlow,
            Double rawTds,
            Double pureTds,
            int sampleCount
    ) {
    }

    private record SessionTrend(
            Double beforeChangePercent,
            Double afterChangePercent,
            Double flowChangePercent
    ) {
        static SessionTrend empty() {
            return new SessionTrend(null, null, null);
        }
    }

    record PressureEvaluation(
            String code,
            String message,
            boolean foulingSuspected,
            boolean backpressureDropIgnored
    ) {
    }

    private record CounterProgress(
            Double latestValue,
            Double delta,
            Instant firstAt,
            Instant lastAt
    ) {
    }
}

package com.platform.ai.service.impl;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoProductionDataCalculatorTest {

    @Test
    void shouldUseAllStableProductionSessionsAndIgnoreIdleValues() {
        Instant start = Instant.parse("2026-08-20T00:00:00Z");
        List<RoProductionDataCalculator.TelemetrySample> samples = new ArrayList<>();
        samples.add(sample(start.minusSeconds(60), 500D, 80D, 0D, 10D, 15D, 0.2D, 2.5D, 0D));

        addSession(samples, start, 140D, 9D, 9.1D, 2.0D, 10D, 15D);
        samples.add(sample(start.plusSeconds(120), 600D, 90D, 0D, 11D, 16D, 0.1D, 2.6D, 0D));
        addSession(samples, start.plusSeconds(600), 150D, 12D, 9.0D, 2.2D, 12D, 18D);
        samples.add(sample(start.plusSeconds(720), 700D, 100D, 0D, 13D, 19D, 0.2D, 2.7D, 0D));

        RoProductionDataCalculator.Analysis result = RoProductionDataCalculator.analyze(samples);

        assertThat(result.productionDetected()).isTrue();
        assertThat(result.productionSessionCount()).isEqualTo(2);
        assertThat(result.stableSessionCount()).isEqualTo(2);
        assertThat(result.stableSampleCount()).isEqualTo(6);
        assertThat(result.productionMembraneBefore()).isCloseTo(9.05D, within(0.001D));
        assertThat(result.productionMembraneAfter()).isCloseTo(2.1D, within(0.001D));
        assertThat(result.productionPressureDiff()).isCloseTo(6.95D, within(0.001D));
        assertThat(result.productionPureFlow()).isEqualTo(80D);
        assertThat(result.productionRawTds()).isCloseTo(145D, within(0.001D));
        assertThat(result.productionPureTds()).isCloseTo(10.5D, within(0.001D));
        assertThat(result.confidence()).isEqualTo("MEDIUM");
    }

    @Test
    void shouldExcludeZeroPureTdsFromStableProductionMedian() {
        Instant start = Instant.parse("2026-08-20T00:00:00Z");
        List<RoProductionDataCalculator.TelemetrySample> samples = new ArrayList<>();
        for (int second = 0; second <= 80; second += 10) {
            Double pureTds = second < 70 ? 0D : 10D;
            samples.add(sample(start.plusSeconds(second), 140D, pureTds, 80D,
                    10D, 15D, 9D, 2D, 1D));
        }

        RoProductionDataCalculator.Analysis result = RoProductionDataCalculator.analyze(samples);

        assertThat(result.productionPureTds()).isEqualTo(10D);
        assertThat(result.productionRawTds()).isEqualTo(140D);
    }

    @Test
    void shouldAccumulateNonNegativeCounterProgressAcrossResetAndIgnoreJitter() {
        Instant start = Instant.parse("2026-08-20T00:00:00Z");
        List<RoProductionDataCalculator.TelemetrySample> samples = List.of(
                sample(start, null, null, 0D, 100D, 200D, null, null, 0D),
                sample(start.plusSeconds(3600), null, null, 0D, 102D, 203D, null, null, 0D),
                sample(start.plusSeconds(7200), null, null, 0D, 101.8D, 202.8D, null, null, 0D),
                sample(start.plusSeconds(10800), null, null, 0D, 0.5D, 0.8D, null, null, 0D),
                sample(start.plusSeconds(14400), null, null, 0D, 2D, 2.8D, null, null, 0D)
        );

        RoProductionDataCalculator.Analysis result = RoProductionDataCalculator.analyze(samples);

        assertThat(result.pureDelta()).isCloseTo(4D, within(0.001D));
        assertThat(result.wasteDelta()).isCloseTo(5.8D, within(0.001D));
        assertThat(result.latestPureTotal()).isEqualTo(2D);
    }

    @Test
    void shouldIgnoreLargePressureDifferenceCausedByStorageTankBackpressureDrop() {
        Instant start = Instant.parse("2026-08-20T00:00:00Z");
        List<RoProductionDataCalculator.TelemetrySample> samples = new ArrayList<>();
        addSession(samples, start, 140D, 8D, 9D, 4D, 10D, 15D, 80D);
        addSession(samples, start.plusSeconds(600), 140D, 8D, 9D, 4D, 11D, 16D, 80D);
        addSession(samples, start.plusSeconds(1200), 140D, 8D, 9.1D, 2D, 12D, 17D, 81D);
        addSession(samples, start.plusSeconds(1800), 140D, 8D, 9.1D, 2D, 13D, 18D, 81D);

        RoProductionDataCalculator.Analysis result = RoProductionDataCalculator.analyze(samples);
        RoProductionDataCalculator.PressureEvaluation evaluation =
                RoProductionDataCalculator.evaluatePressure(result);

        assertThat(result.membraneBeforeChangePercent()).isBetween(1D, 2D);
        assertThat(result.membraneAfterChangePercent()).isEqualTo(-50D);
        assertThat(result.pureFlowChangePercent()).isBetween(1D, 2D);
        assertThat(evaluation.code()).isEqualTo("BACKPRESSURE_DROP_IGNORED");
        assertThat(evaluation.foulingSuspected()).isFalse();
        assertThat(evaluation.backpressureDropIgnored()).isTrue();
    }

    @Test
    void shouldSuspectFoulingWhenFeedPressureRisesAndPureFlowDeclinesTogether() {
        Instant start = Instant.parse("2026-08-20T00:00:00Z");
        List<RoProductionDataCalculator.TelemetrySample> samples = new ArrayList<>();
        addSession(samples, start, 140D, 8D, 8D, 2D, 10D, 15D, 90D);
        addSession(samples, start.plusSeconds(600), 140D, 8D, 8D, 2D, 11D, 16D, 90D);
        addSession(samples, start.plusSeconds(1200), 140D, 8D, 10D, 2D, 12D, 17D, 70D);
        addSession(samples, start.plusSeconds(1800), 140D, 8D, 10D, 2D, 13D, 18D, 70D);

        RoProductionDataCalculator.Analysis result = RoProductionDataCalculator.analyze(samples);
        RoProductionDataCalculator.PressureEvaluation evaluation =
                RoProductionDataCalculator.evaluatePressure(result);

        assertThat(result.membraneBeforeChangePercent()).isEqualTo(25D);
        assertThat(result.pureFlowChangePercent()).isCloseTo(-22.222D, within(0.001D));
        assertThat(evaluation.code()).isEqualTo("POSSIBLE_FOULING");
        assertThat(evaluation.foulingSuspected()).isTrue();
        assertThat(evaluation.backpressureDropIgnored()).isFalse();
    }

    private static void addSession(List<RoProductionDataCalculator.TelemetrySample> samples,
                                   Instant start,
                                   double rawTds,
                                   double pureTds,
                                   double before,
                                   double after,
                                   double pureTotal,
                                   double wasteTotal) {
        addSession(samples, start, rawTds, pureTds, before, after, pureTotal, wasteTotal, 80D);
    }

    private static void addSession(List<RoProductionDataCalculator.TelemetrySample> samples,
                                   Instant start,
                                   double rawTds,
                                   double pureTds,
                                   double before,
                                   double after,
                                   double pureTotal,
                                   double wasteTotal,
                                   double flow) {
        for (int second = 0; second <= 80; second += 10) {
            double currentPureTds = second == 60 ? 0D : pureTds;
            samples.add(sample(start.plusSeconds(second), rawTds, currentPureTds, flow,
                    pureTotal + second / 100D, wasteTotal + second / 80D, before, after, 1D));
        }
    }

    private static RoProductionDataCalculator.TelemetrySample sample(
            Instant time,
            Double rawTds,
            Double pureTds,
            Double flow,
            Double pureTotal,
            Double wasteTotal,
            Double before,
            Double after,
            Double status) {
        return new RoProductionDataCalculator.TelemetrySample(
                time, rawTds, pureTds, flow, pureTotal, wasteTotal, before, after, status
        );
    }

    private static org.assertj.core.data.Offset<Double> within(double value) {
        return org.assertj.core.data.Offset.offset(value);
    }
}

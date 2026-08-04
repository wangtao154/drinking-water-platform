package com.platform.ai.controller;

import com.platform.ai.dto.RoMembranePredictionRequest;
import com.platform.ai.dto.RoMembranePredictionVO;
import com.platform.ai.service.RoMembranePredictionService;
import com.platform.common.result.R;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/ai/ro-membrane")
public class RoMembranePredictionController {

    private final RoMembranePredictionService predictionService;

    public RoMembranePredictionController(RoMembranePredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @GetMapping("/predict")
    public R<RoMembranePredictionVO> predict(
            @RequestParam @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{2,64}$") String sn,
            @RequestParam(required = false) @Min(1) @Max(180) Integer rangeDays,
            @RequestParam(required = false) @Min(100) Double ratedPureLiters,
            @RequestParam(required = false) @Pattern(regexp = "^[1-9][0-9]*(s|m|h)$") String aggregateEvery) {
        RoMembranePredictionRequest request = new RoMembranePredictionRequest();
        request.setSn(sn);
        request.setRangeDays(rangeDays);
        request.setRatedPureLiters(ratedPureLiters);
        request.setAggregateEvery(aggregateEvery);
        return R.ok(predictionService.predict(request));
    }

    @PostMapping("/predict")
    public R<RoMembranePredictionVO> predict(@Valid @RequestBody RoMembranePredictionRequest request) {
        return R.ok(predictionService.predict(request));
    }
}

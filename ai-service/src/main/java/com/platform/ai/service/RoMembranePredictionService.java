package com.platform.ai.service;

import com.platform.ai.dto.RoMembranePredictionRequest;
import com.platform.ai.dto.RoMembranePredictionVO;

public interface RoMembranePredictionService {

    RoMembranePredictionVO predict(RoMembranePredictionRequest request);
}

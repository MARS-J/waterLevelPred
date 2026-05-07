package com.waterlevel.server.service;

import com.waterlevel.server.dto.PredictRequest;

import java.util.Map;

public interface AlgorithmService {

    Map<String, Object> getAlgorithmHealth();

    Map<String, Object> predict(PredictRequest request);

    Map<String, Object> getBackendOverview();

    Map<String, Object> getVisualScreenSummary();

    Map<String, Object> getSceneStatus();
}

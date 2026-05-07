package com.waterlevel.server.controller;

import com.waterlevel.server.common.ApiResponse;
import com.waterlevel.server.dto.PredictRequest;
import com.waterlevel.server.service.AlgorithmService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/algorithm")
public class AlgorithmController {

    private final AlgorithmService algorithmService;

    public AlgorithmController(AlgorithmService algorithmService) {
        this.algorithmService = algorithmService;
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> algorithmHealth() {
        return ApiResponse.success(algorithmService.getAlgorithmHealth());
    }

    @PostMapping("/predict")
    public ApiResponse<Map<String, Object>> predict(@Valid @RequestBody PredictRequest request) {
        return ApiResponse.success("prediction success", algorithmService.predict(request));
    }
}

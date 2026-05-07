package com.waterlevel.server.controller;

import com.waterlevel.server.common.ApiResponse;
import com.waterlevel.server.service.AlgorithmService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/visual")
public class VisualController {

    private final AlgorithmService algorithmService;

    public VisualController(AlgorithmService algorithmService) {
        this.algorithmService = algorithmService;
    }

    @GetMapping("/screen/summary")
    public ApiResponse<Map<String, Object>> screenSummary() {
        return ApiResponse.success(algorithmService.getVisualScreenSummary());
    }

    @GetMapping("/scene/status")
    public ApiResponse<Map<String, Object>> sceneStatus() {
        return ApiResponse.success(algorithmService.getSceneStatus());
    }
}

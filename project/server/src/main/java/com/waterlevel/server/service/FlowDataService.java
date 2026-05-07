package com.waterlevel.server.service;

import com.waterlevel.server.entity.FlowRawData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface FlowDataService {

    Map<String, Object> importCsv(MultipartFile file, String dataSource, String stationCode);

    Page<FlowRawData> listData(LocalDateTime startTime, LocalDateTime endTime,
                                String dataSource, String stationCode, String qualityStatus,
                                Pageable pageable);

    FlowRawData getDetail(Long id);

    void deleteData(Long id);

    void batchDelete(List<Long> ids);

    FlowRawData updateData(Long id, FlowRawData data);

    Map<String, Object> checkQuality();
}

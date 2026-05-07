-- 原始流量数据表（支持多特征）
CREATE TABLE IF NOT EXISTS flow_raw_data (
    id BIGSERIAL PRIMARY KEY,
    monitor_time TIMESTAMP NOT NULL,
    observed_wl DOUBLE PRECISION,
    predicted_wl DOUBLE PRECISION,
    wind_speed DOUBLE PRECISION,
    air_pressure DOUBLE PRECISION,
    air_temperature DOUBLE PRECISION,
    water_temperature DOUBLE PRECISION,
    station_code VARCHAR(100),
    data_source VARCHAR(50),
    quality_status VARCHAR(20) DEFAULT '待校验',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 训练数据集表
CREATE TABLE IF NOT EXISTS flow_dataset (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    time_range_start TIMESTAMP,
    time_range_end TIMESTAMP,
    record_count INT DEFAULT 0,
    quality_score DOUBLE PRECISION,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 数据集与原始数据关联表
CREATE TABLE IF NOT EXISTS flow_dataset_record (
    id BIGSERIAL PRIMARY KEY,
    dataset_id BIGINT NOT NULL REFERENCES flow_dataset(id) ON DELETE CASCADE,
    raw_data_id BIGINT NOT NULL REFERENCES flow_raw_data(id) ON DELETE CASCADE,
    UNIQUE(dataset_id, raw_data_id)
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_flow_raw_data_monitor_time ON flow_raw_data(monitor_time);
CREATE INDEX IF NOT EXISTS idx_flow_raw_data_station_code ON flow_raw_data(station_code);
CREATE INDEX IF NOT EXISTS idx_flow_raw_data_data_source ON flow_raw_data(data_source);
CREATE INDEX IF NOT EXISTS idx_flow_raw_data_quality_status ON flow_raw_data(quality_status);
CREATE INDEX IF NOT EXISTS idx_flow_dataset_time_range ON flow_dataset(time_range_start, time_range_end);

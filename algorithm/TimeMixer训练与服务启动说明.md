# TimeMixer 训练与服务启动说明

## 1. 目录说明

- 算法根目录：`/home/xingyu/projects/my/parttime/waterLevelPred/algorithm/TimeMixer`
- 原始数据集：`/home/xingyu/projects/my/parttime/waterLevelPred/algorithm/TimeMixer/data/noaa_full_features.csv`
- 训练脚本：`/home/xingyu/projects/my/parttime/waterLevelPred/algorithm/TimeMixer/train_waterlevel.py`
- 服务脚本：`/home/xingyu/projects/my/parttime/waterLevelPred/algorithm/TimeMixer/timemixer_service.py`
- 模型权重输出目录：`/home/xingyu/projects/my/parttime/waterLevelPred/algorithm/TimeMixer/artifacts/noaa_observed_wl`

## 2. 运行前提

本项目算法运行统一使用 conda 环境 `tm`。

先进入算法目录：

```bash
cd /home/xingyu/projects/my/parttime/waterLevelPred/algorithm/TimeMixer
```

如果只是临时执行命令，推荐直接使用：

```bash
conda run -n tm python xxx.py
```

如果希望先进入环境再执行，也可以：

```bash
conda activate tm
cd /home/xingyu/projects/my/parttime/waterLevelPred/algorithm/TimeMixer
```

## 3. 如何跑训练

### 3.1 直接使用当前封装好的训练脚本

当前项目已经有一个面向 `noaa_full_features.csv` 的训练脚本，可以直接执行：

```bash
conda run -n tm python train_waterlevel.py
```

这个脚本会自动完成以下事情：

- 读取 `data/noaa_full_features.csv`
- 统一时间列名为 `date`
- 生成预处理后的训练文件 `data/noaa_full_features_timemixer.csv`
- 对缺失值做插值和前后填充
- 使用 TimeMixer 进行训练和测试
- 将最佳权重复制到 `artifacts/noaa_observed_wl/checkpoint.pth`
- 生成模型元数据 `artifacts/noaa_observed_wl/metadata.json`

### 3.2 当前训练默认参数

当前训练脚本默认使用以下关键参数：

- 目标列：`observed_wl`
- 输入序列长度：`96`
- 预测步长：`24`
- 训练轮数：`1`
- 批大小：`256`
- 学习率：`0.001`
- 模型：`TimeMixer`
- 数据模式：`M`

### 3.3 自定义训练参数

如果需要自定义参数，可以这样运行：

```bash
conda run -n tm python train_waterlevel.py \
  --source-csv data/noaa_full_features.csv \
  --prepared-csv data/noaa_full_features_timemixer.csv \
  --artifact-dir artifacts/noaa_observed_wl \
  --target observed_wl \
  --seq-len 96 \
  --pred-len 24 \
  --train-epochs 5 \
  --batch-size 256 \
  --learning-rate 0.001 \
  --model-id waterlevel_noaa
```

### 3.4 训练完成后重点查看的文件

- 训练导出的权重：

```bash
/home/xingyu/projects/my/parttime/waterLevelPred/algorithm/TimeMixer/artifacts/noaa_observed_wl/checkpoint.pth
```

- 训练元数据：

```bash
/home/xingyu/projects/my/parttime/waterLevelPred/algorithm/TimeMixer/artifacts/noaa_observed_wl/metadata.json
```

- 原始 checkpoint：

```bash
/home/xingyu/projects/my/parttime/waterLevelPred/algorithm/TimeMixer/checkpoints/long_term_forecast_waterlevel_noaa_noaa_TimeMixer_custom_sl96_pl24_dm16_nh8_el3_dl1_df32_fc3_ebtimeF_dtTrue_WaterLevel_0/checkpoint.pth
```

## 4. 如何启动可被调用的服务

### 4.1 启动服务

训练完成后，可以直接启动当前封装好的 HTTP 服务：

```bash
conda run -n tm python timemixer_service.py
```

默认启动地址：

```text
http://0.0.0.0:18081
```

### 4.2 自定义服务端口

如果需要修改端口，可以这样启动：

```bash
conda run -n tm python timemixer_service.py --port 18082
```

如果需要指定模型目录：

```bash
conda run -n tm python timemixer_service.py \
  --artifact-dir /home/xingyu/projects/my/parttime/waterLevelPred/algorithm/TimeMixer/artifacts/noaa_observed_wl \
  --host 0.0.0.0 \
  --port 18081
```

### 4.3 服务启动依赖的文件

服务启动时会自动读取：

- `artifacts/noaa_observed_wl/metadata.json`
- `artifacts/noaa_observed_wl/checkpoint.pth`

如果这两个文件不存在，说明需要先执行训练。

## 5. 服务接口说明

### 5.1 健康检查接口

请求：

```bash
curl http://127.0.0.1:18081/health
```

作用：

- 检查服务是否启动成功
- 返回目标列、输入长度、预测长度、权重路径

### 5.2 预测接口

请求方式：

```bash
POST /predict
```

### 5.3 使用已有 CSV 文件预测

```bash
curl -X POST http://127.0.0.1:18081/predict \
  -H "Content-Type: application/json" \
  -d '{
    "csv_path": "/home/xingyu/projects/my/parttime/waterLevelPred/algorithm/TimeMixer/data/noaa_full_features.csv"
  }'
```

说明：

- 服务会读取该 CSV
- 自动取最后 `96` 条作为输入窗口
- 输出未来 `24` 个时间点的预测结果

### 5.4 使用前端或后端传入 records 预测

```bash
curl -X POST http://127.0.0.1:18081/predict \
  -H "Content-Type: application/json" \
  -d '{
    "records": [
      {
        "date": "2024-01-01 00:00:00",
        "predicted_wl": 0.419,
        "wind_speed": 1.9,
        "air_press": 1015.2,
        "air_temp": 3.0,
        "water_temp": 6.5,
        "observed_wl": 0.683
      }
    ]
  }'
```

注意：

- `records` 模式下必须传入完整字段
- 实际调用时至少需要 `96` 条记录
- 字段需要包含：
  - `date`
  - `predicted_wl`
  - `wind_speed`
  - `air_press`
  - `air_temp`
  - `water_temp`
  - `observed_wl`

## 6. 返回结果说明

`/predict` 返回的核心字段包括：

- `target`：当前预测目标列
- `target_predictions`：目标列未来预测值
- `feature_predictions`：全部特征未来预测值
- `model_setting`：训练任务 setting
- `checkpoint_path`：当前加载权重路径

其中最常用的是：

- `target_predictions`

格式示例：

```json
[
  {
    "date": "2025-11-01 00:00:00",
    "value": 0.5863592028617859
  }
]
```

## 7. 推荐调用流程

推荐按以下顺序使用：

1. 先训练模型
2. 确认 `artifacts/noaa_observed_wl` 下已有 `checkpoint.pth` 和 `metadata.json`
3. 启动 `timemixer_service.py`
4. 先调用 `/health`
5. 再调用 `/predict`
6. 由 Java Spring Boot 或前端去请求这个 HTTP 服务

## 8. 常用命令汇总

### 训练

```bash
cd /home/xingyu/projects/my/parttime/waterLevelPred/algorithm/TimeMixer
conda run -n tm python train_waterlevel.py
```

### 启动服务

```bash
cd /home/xingyu/projects/my/parttime/waterLevelPred/algorithm/TimeMixer
conda run -n tm python timemixer_service.py
```

### 健康检查

```bash
curl http://127.0.0.1:18081/health
```

### 预测

```bash
curl -X POST http://127.0.0.1:18081/predict \
  -H "Content-Type: application/json" \
  -d '{"csv_path": "/home/xingyu/projects/my/parttime/waterLevelPred/algorithm/TimeMixer/data/noaa_full_features.csv"}'
```

## 9. 补充说明

- 训练和服务都必须在 conda 环境 `tm` 下执行
- 当前服务默认使用 CPU
- 当前服务的默认预测步长固定为训练时的 `24`
- 如果更换了数据集、目标列、输入维度或预测步长，建议重新训练并重新启动服务

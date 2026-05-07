from __future__ import annotations

import json
import sys
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any

import pandas as pd
import torch
from sklearn.preprocessing import StandardScaler

ROOT = Path(__file__).resolve().parents[2]
ALGORITHM_DIR = ROOT / "algorithm" / "TimeMixer"
sys.path.insert(0, str(ALGORITHM_DIR))

from models import TimeMixer
from run import parser
from utils.timefeatures import time_features


ARTIFACT_DIR = ALGORITHM_DIR / "artifacts" / "noaa_observed_wl"
METADATA_PATH = ARTIFACT_DIR / "metadata.json"


def load_csv(csv_path: str | Path, target: str) -> pd.DataFrame:
    frame = pd.read_csv(csv_path)
    if "date" not in frame.columns and "Date Time" in frame.columns:
        frame = frame.rename(columns={"Date Time": "date"})
    if "date" not in frame.columns:
        raise ValueError("缺少 date 或 Date Time 列")
    if target not in frame.columns:
        raise ValueError(f"缺少目标列 {target}")
    frame["date"] = pd.to_datetime(frame["date"])
    feature_columns = [column for column in frame.columns if column not in {"date", target}]
    frame = frame[["date", *feature_columns, target]].sort_values("date").reset_index(drop=True)
    numeric_columns = [column for column in frame.columns if column != "date"]
    frame[numeric_columns] = frame[numeric_columns].interpolate(method="linear", limit_direction="both")
    frame[numeric_columns] = frame[numeric_columns].ffill().bfill()
    return frame


def fit_scaler(frame: pd.DataFrame) -> StandardScaler:
    scaler = StandardScaler()
    train_border = int(len(frame) * 0.7)
    scaler.fit(frame.iloc[:train_border, 1:].values)
    return scaler


def build_args(training_args: dict[str, Any]):
    args = parser.parse_args(
        [
            "--task_name",
            str(training_args["task_name"]),
            "--is_training",
            str(training_args["is_training"]),
            "--model_id",
            str(training_args["model_id"]),
            "--model",
            str(training_args["model"]),
            "--data",
            str(training_args["data"]),
        ]
    )
    for key, value in training_args.items():
        setattr(args, key, value)
    args.use_gpu = False
    args.use_multi_gpu = False
    return args


def build_time_marks(dates: pd.Series, freq: str):
    return time_features(pd.to_datetime(dates.values), freq=freq).transpose(1, 0)


def infer_frequency(frame: pd.DataFrame) -> pd.Timedelta:
    diffs = frame["date"].sort_values().diff().dropna()
    if diffs.empty:
        raise ValueError("无法推断时间间隔")
    return diffs.mode().iloc[0]


class TimeMixerBridge:
    def __init__(self) -> None:
        if not METADATA_PATH.exists():
            raise FileNotFoundError(f"未找到模型元数据: {METADATA_PATH}")
        self.metadata = json.loads(METADATA_PATH.read_text())
        self.args = build_args(self.metadata["training_args"])
        self.device = torch.device("cpu")
        self.model = TimeMixer.Model(self.args).float().to(self.device)
        state_dict = torch.load(self.metadata["checkpoint_path"], map_location=self.device)
        self.model.load_state_dict(state_dict)
        self.model.eval()
        self.target = self.metadata["target"]
        self.feature_columns = self.metadata["feature_columns"]
        self.training_frame = load_csv(self.metadata["prepared_csv"], self.target)
        self.scaler = fit_scaler(self.training_frame)

    def prepare_input_frame(self, payload: dict[str, Any]) -> pd.DataFrame:
        csv_path = payload.get("csv_path")
        records = payload.get("records")
        if csv_path:
            frame = load_csv(csv_path, self.target)
        elif records:
            frame = pd.DataFrame(records)
            if "date" not in frame.columns and "Date Time" in frame.columns:
                frame = frame.rename(columns={"Date Time": "date"})
            missing_columns = [column for column in ["date", *self.feature_columns] if column not in frame.columns]
            if missing_columns:
                raise ValueError(f"缺少字段: {missing_columns}")
            frame["date"] = pd.to_datetime(frame["date"])
            frame = frame[["date", *self.feature_columns]].sort_values("date").reset_index(drop=True)
            numeric_columns = [column for column in frame.columns if column != "date"]
            frame[numeric_columns] = frame[numeric_columns].interpolate(method="linear", limit_direction="both")
            frame[numeric_columns] = frame[numeric_columns].ffill().bfill()
        else:
            frame = self.training_frame.copy()
        if len(frame) < self.args.seq_len:
            raise ValueError(f"输入数据不足，至少需要 {self.args.seq_len} 条")
        return frame

    def predict(self, payload: dict[str, Any]) -> dict[str, Any]:
        frame = self.prepare_input_frame(payload)
        pred_len = int(payload.get("pred_len", self.args.pred_len))
        if pred_len != self.args.pred_len:
            raise ValueError(f"当前模型固定预测步长为 {self.args.pred_len}")

        history = frame.tail(self.args.seq_len).copy()
        freq_delta = infer_frequency(frame)
        future_dates = pd.date_range(
            start=history["date"].iloc[-1] + freq_delta,
            periods=pred_len,
            freq=freq_delta,
        )

        scaled_x = self.scaler.transform(history[self.feature_columns].values)
        batch_x = torch.tensor(scaled_x, dtype=torch.float32, device=self.device).unsqueeze(0)
        batch_x_mark = torch.tensor(
            build_time_marks(history["date"], self.args.freq),
            dtype=torch.float32,
            device=self.device,
        ).unsqueeze(0)
        batch_y_mark = torch.tensor(
            build_time_marks(pd.Series(future_dates), self.args.freq),
            dtype=torch.float32,
            device=self.device,
        ).unsqueeze(0)

        dec_inp = None
        if self.args.down_sampling_layers == 0:
            dec_inp = torch.zeros(
                (1, pred_len, len(self.feature_columns)),
                dtype=torch.float32,
                device=self.device,
            )

        with torch.no_grad():
            outputs = self.model(batch_x, batch_x_mark, dec_inp, batch_y_mark)
        prediction = outputs.detach().cpu().numpy()[0]
        restored = self.scaler.inverse_transform(prediction)
        result_frame = pd.DataFrame(restored, columns=self.feature_columns)
        result_frame.insert(0, "date", future_dates.astype(str))

        return {
            "model": self.metadata["model_name"],
            "target": self.target,
            "seq_len": self.args.seq_len,
            "pred_len": self.args.pred_len,
            "checkpoint_path": self.metadata["checkpoint_path"],
            "target_predictions": result_frame[["date", self.target]].rename(columns={self.target: "value"}).to_dict(orient="records"),
            "feature_predictions": result_frame.to_dict(orient="records"),
        }


BRIDGE = TimeMixerBridge()


class Handler(BaseHTTPRequestHandler):
    def send_json(self, status: int, payload: dict[str, Any]) -> None:
        data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self) -> None:
        if self.path == "/health":
            self.send_json(
                200,
                {
                    "status": "ok",
                    "model": BRIDGE.metadata["model_name"],
                    "target": BRIDGE.target,
                    "checkpoint_path": BRIDGE.metadata["checkpoint_path"],
                },
            )
            return
        if self.path == "/metadata":
            self.send_json(200, BRIDGE.metadata)
            return
        self.send_json(404, {"message": "not found"})

    def do_POST(self) -> None:
        if self.path != "/predict":
            self.send_json(404, {"message": "not found"})
            return
        length = int(self.headers.get("Content-Length", "0"))
        raw_body = self.rfile.read(length).decode("utf-8") if length else "{}"
        try:
            payload = json.loads(raw_body or "{}")
            self.send_json(200, BRIDGE.predict(payload))
        except Exception as exc:
            self.send_json(400, {"message": str(exc)})


def main() -> None:
    host = "0.0.0.0"
    port = 18081
    server = ThreadingHTTPServer((host, port), Handler)
    print(f"TimeMixer bridge service running at http://{host}:{port}")
    server.serve_forever()


if __name__ == "__main__":
    main()

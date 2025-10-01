# Exchange Format Specification

港内 Repast モデルと外回り C++ モデルの間でやり取りする JSON メッセージ仕様。

## 共通メッセージ構造
- すべてのメッセージは UTF-8 エンコードの JSON オブジェクト。
- `event` フィールドでイベント種別を指定する。
- タイムスタンプは C++ 側で保持しているシミュレーション時間（UTC 相当）を `timestamp` として ISO-8601 文字列で送信する。
- `simulationStep` は Repast 側のステップ番号。双方で同期させる際の参考値。

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "SharedMessageEnvelope",
  "type": "object",
  "required": ["event", "messageId"],
  "properties": {
    "event": {
      "type": "string",
      "enum": [
        "arrival",
        "supply_request",
        "schedule_update",
        "loaded",
        "unloaded",
        "placement_completed",
        "installation_progress"
      ]
    },
    "messageId": { "type": "string", "description": "UUID などのユニーク ID" },
    "timestamp": { "type": "string", "format": "date-time" },
    "simulationStep": { "type": "integer", "minimum": 0 },
    "payload": { "type": "object" }
  },
  "additionalProperties": false
}
```

以下では `payload` のスキーマをイベント種別ごとに定義する。

---

## C++ → Repast
### `arrival`
C++ 側で計算された船団の到着情報。

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "ArrivalPayload",
  "type": "object",
  "required": ["vesselId", "eta", "cargo"],
  "properties": {
    "vesselId": { "type": "integer", "minimum": 0 },
    "eta": { "type": "string", "format": "date-time", "description": "港内到着予定時刻" },
    "position": {
      "type": "object",
      "required": ["x", "y"],
      "properties": {
        "x": { "type": "number" },
        "y": { "type": "number" }
      }
    },
    "capacity": { "type": "number", "minimum": 0 },
    "cargo": {
      "type": "array",
      "items": { "$ref": "#/definitions/material" }
    }
  },
  "definitions": {
    "material": {
      "type": "object",
      "required": ["type", "quantity"],
      "properties": {
        "type": {
          "type": "string",
          "enum": ["Blade", "Nacelle", "Tower", "Foundation"]
        },
        "quantity": { "type": "integer", "minimum": 1 }
      }
    }
  },
  "additionalProperties": false
}
```

### `supply_request`
Repast 側で不足している資材を C++ 側に要求する。

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "SupplyRequestPayload",
  "type": "object",
  "required": ["yardId", "materials"],
  "properties": {
    "yardId": { "type": "integer", "minimum": 0 },
    "materials": {
      "type": "array",
      "items": { "$ref": "#/definitions/material" }
    },
    "priority": { "type": "string", "enum": ["low", "normal", "high"], "default": "normal" }
  },
  "definitions": {
    "material": {
      "type": "object",
      "required": ["type", "quantity"],
      "properties": {
        "type": {
          "type": "string",
          "enum": ["Blade", "Nacelle", "Tower", "Foundation"]
        },
        "quantity": { "type": "integer", "minimum": 1 }
      }
    }
  },
  "additionalProperties": false
}
```

### `schedule_update`
C++ 側の工程計画変更を港内モデルに伝える。

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "ScheduleUpdatePayload",
  "type": "object",
  "required": ["tasks"],
  "properties": {
    "tasks": {
      "type": "array",
      "items": {
        "type": "object",
        "required": ["taskId", "vesselId", "start", "end"],
        "properties": {
          "taskId": { "type": "string" },
          "vesselId": { "type": "integer", "minimum": 0 },
          "start": { "type": "string", "format": "date-time" },
          "end": { "type": "string", "format": "date-time" },
          "description": { "type": "string" }
        },
        "additionalProperties": false
      }
    }
  },
  "additionalProperties": false
}
```

---

## Repast → C++
### `loaded`
港内での資材積込完了通知。

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "LoadedPayload",
  "type": "object",
  "required": ["vesselId", "materials"],
  "properties": {
    "vesselId": { "type": "integer", "minimum": 0 },
    "materials": {
      "type": "array",
      "items": { "$ref": "#/definitions/material" }
    }
  },
  "definitions": {
    "material": {
      "type": "object",
      "required": ["type", "quantity"],
      "properties": {
        "type": {
          "type": "string",
          "enum": ["Blade", "Nacelle", "Tower", "Foundation"]
        },
        "quantity": { "type": "integer", "minimum": 1 }
      }
    }
  },
  "additionalProperties": false
}
```

### `unloaded`
港内での荷下ろし完了通知。

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "UnloadedPayload",
  "type": "object",
  "required": ["vesselId", "materials", "yardId"],
  "properties": {
    "vesselId": { "type": "integer", "minimum": 0 },
    "yardId": { "type": "integer", "minimum": 0 },
    "materials": {
      "type": "array",
      "items": { "$ref": "#/definitions/material" }
    }
  },
  "definitions": {
    "material": {
      "type": "object",
      "required": ["type", "quantity"],
      "properties": {
        "type": {
          "type": "string",
          "enum": ["Blade", "Nacelle", "Tower", "Foundation"]
        },
        "quantity": { "type": "integer", "minimum": 1 }
      }
    }
  },
  "additionalProperties": false
}
```

### `placement_completed`
資材の港内配置が完了した際の通知。

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "PlacementCompletedPayload",
  "type": "object",
  "required": ["materialId", "location"],
  "properties": {
    "materialId": { "type": "string" },
    "location": {
      "type": "object",
      "required": ["x", "y"],
      "properties": {
        "x": { "type": "number" },
        "y": { "type": "number" }
      }
    },
    "vesselId": { "type": ["integer", "null"], "description": "関連する船舶 ID。無ければ null" }
  },
  "additionalProperties": false
}
```

### `installation_progress`
外回り工程に影響する進捗情報を送る。

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "InstallationProgressPayload",
  "type": "object",
  "required": ["materialType", "completed"],
  "properties": {
    "materialType": {
      "type": "string",
      "enum": ["Blade", "Nacelle", "Tower", "Foundation"]
    },
    "completed": { "type": "integer", "minimum": 0 },
    "total": { "type": "integer", "minimum": 0 },
    "notes": { "type": "string" }
  },
  "additionalProperties": false
}
```

---

## 例: 到着通知

```json
{
  "event": "arrival",
  "messageId": "8d3f224c-8a43-4e17-8b7a-64fd877ad1f3",
  "timestamp": "2025-05-12T09:00:00Z",
  "simulationStep": 120,
  "payload": {
    "vesselId": 3,
    "eta": "2025-05-12T09:30:00Z",
    "position": { "x": 125.4, "y": 87.2 },
    "capacity": 4,
    "cargo": [
      { "type": "Blade", "quantity": 2 },
      { "type": "Tower", "quantity": 1 }
    ]
  }
}
```

---

この仕様は初期案であり、通信項目が増える場合には追記する。

# Agents Specification

## Overview
- **C++ (外回り施工フロー)**: 既存の施工シミュレータで外洋での施工スケジュールや船団の到着計算を実行する。計算結果は `docs/cppdoc` にまとめられている外回りモデルの変数・ロジックに基づく。
- **Repast (港内モデル)**: 港内でのエージェント挙動、資材ハンドリング、動的なリソース配分を担当する。C++ 側から受け取った結果を、港内のエージェントとして具象化し、港内イベントの進行を管理する。
- **棲み分け**: C++ で算出済みの値（船団到着、工程進捗など）を Repast のエージェント属性・スケジュールに反映し、港内での配置・積込・ヤード管理を Repast が細かく制御する。

---

## Agent: Vessel（作業船・輸送船）
- **属性**
  - `id: int`
  - `position: (x: double, y: double)` — 港内平面での現在位置
  - `capacity: double` — 最大積載可能量
  - `cargo: List<Material>` — 積載中の資材
  - `status: enum {待機, 移動中, 積込中, 荷下ろし中}`
  - `assignedTask: Optional<Task>` — 現在指示されている作業内容
- **行動**
  - `moveTo(targetPosition)` — 指定座標に移動
  - `load(material)` — クレーンから資材を受け取り積込
  - `unload(material)` — ヤードや設置地点へ資材を降ろす
  - `requestAssistance()` — クレーン等への作業要求を送信
- **スケジュール**
  - `@ScheduledMethod(start = 1, interval = 1)` — 各ステップで移動・積込・降ろし処理を進行
- **通信**
  - **C++ → Repast**: `arrival` イベントなどを通じて生成・更新される
  - **Repast → C++**: `loaded` / `unloaded` などの完了通知を送信

---

## Agent: Crane（クレーン）
- **属性**
  - `id: int`
  - `position: (x: double, y: double)`
  - `radius: double` — 最大作業半径
  - `rate: double` — 単位時間あたりの処理能力
  - `status: enum {待機, 作業中}`
  - `queue: List<CraneTask>` — 作業待ち行列
- **行動**
  - `assignTask(craneTask)` — 船・ヤードからの要求を受け付け
  - `pick(material)` — 資材を掴み上げる
  - `place(material, targetAgent)` — Vessel または Yard へ配置
- **スケジュール**
  - `@ScheduledMethod(start = 1, interval = 1)` — 待ち行列処理と作業状態更新
- **通信**
  - Repast 内で Vessel・Yard と同期し、必要に応じて完了報告を発火

---

## Agent: Yard（ヤード）
- **属性**
  - `id: int`
  - `position: (x: double, y: double)`
  - `capacity: double` — 保管容量
  - `stock: List<Material>` — 現在保管中の資材
  - `layout: YardLayout` — ヤード内配置情報
- **行動**
  - `store(material)` — 資材を受け入れ保管
  - `retrieve(material)` — 指定資材を取り出しクレーンに引き渡す
  - `inventoryReport()` — 現在在庫を集計し C++ 側と同期
- **通信**
  - 主に Repast 内で Vessel / Crane と連携し、必要に応じて在庫状況を C++ 側へ送信

---

## Agent: Material（資材）
- **属性**
  - `type: enum {Blade, Nacelle, Tower, Foundation}`
  - `size: double`
  - `weight: double`
  - `state: enum {ヤード保管, 移動中, 船上, 設置完了}`
  - `owner: AgentReference` — 所属先（Yard or Vessel）
- **行動**
  - `assignTo(agent)` — 所属先を更新
  - `markInstalled()` — 設置完了を記録
- **通信**
  - 状態変化は Vessel / Yard を通じて共有され、必要に応じて C++ へレポート

---

## Agent: Worker（作業員／チーム）
- **属性**
  - `id: int`
  - `skill: enum {溶接, 荷役, 組立}`
  - `status: enum {待機, 作業中}`
  - `assignedTask: Optional<Task>`
- **行動**
  - `workOn(target)` — 船内・ヤードでの作業に従事
  - `reportProgress()` — 作業進捗を Repast 内に共有
- **通信**
  - 内部エージェント間の調整が中心。大きな工程進捗は C++ 側に送出

---

## C++ ↔ Repast 対応
- **C++ 内で計算済みの値**
  - 船団の到着時刻 → Repast の Vessel エージェント生成タイミングに利用
  - 資材在庫量・需要 → Repast の Yard `stock` と連携
  - 工程進捗（設置完了率など） → Material の `state` 更新と Worker の報告に反映
- **Repast 側で扱うイベント**
  - Vessel 到着・移動・積込
  - Crane による積込／荷下ろし
  - Yard の在庫変動
  - Material / Worker 状態遷移
- **通信イベント例**
  - C++ → Repast: `arrival`, `supply_request`, `schedule_update`
  - Repast → C++: `loaded`, `unloaded`, `installation_progress`

---

## 今後の拡張
- `HelloAgent` を拡張した `VesselAgent` 実装へ本仕様をマッピングする。
- Repast シミュレーション内でのスケジューリングと C++ 側イベント駆動ロジックを整合させる。
- 本ファイルは追加のエージェント種別や属性が定義された場合に随時更新する。

# Repast 実行環境の組み立てとバッチ実行手順

このドキュメントでは、リポジトリに含まれる港内モデルを Repast Simphony 上で
再構成して実行するまでの流れをまとめる。C++ 外洋モデルの成果物を踏まえつつ、
Repast で港内挙動を再現するための手順やコマンドの意味も詳しく解説する。

## 1. リポジトリ構成の概観

- `docs/cppdoc/` — C++ 側で定義された外回り（洋上）モデルのドキュメント。到着スケジュール
  や資材フローの前提条件が整理されている。Repast 側ではこれらをインポートした値として
  エージェント初期化に利用する。
- `src/test250930/` — Repast シナリオの Java 実装。港内の Vessel / Crane / Yard / Worker など
  のエージェントがここで定義され、C++ 計算結果を属性やイベントに写像する。
- `test250930.rs/` — Repast のシナリオ設定（`scenario.xml`, `launch.props` など）。
  実行時にはここで指定されたディレクトリ構成が参照される。
- `scripts/` — Repast ランタイムを取得し、クラスパスを自動生成するユーティリティ。

## 2. Repast ランタイムの取得

リポジトリには Repast Simphony の本体は含まれないため、まずは依存プラグインを
取得する。以下のスクリプトが Update Site の展開とクラスパスの平坦化を行う。

```bash
./scripts/setup_repast_runtime.sh
```

実行すると `lib/repast-2.11.0/` 以下にプラグインが展開され、`classpath.txt` に
必要なパスが 1 行ずつ出力される。環境変数として利用する場合は次のヘルパーが便利。

```bash
CLASSPATH=$(./scripts/repast_classpath.sh)
```

Windows PowerShell や `cmd.exe` で利用する際は、`CLASSPATH_WIN` などの変数に
セミコロン区切りで格納すると後続の `java` コマンドに渡しやすい。

## 3. モデルコードの再コンパイル

Repast の Java クラスは `src/test250930/` 配下にまとまっている。クラスパスが
用意できたら `find` と `xargs` を組み合わせて全ファイルを再コンパイルする。

```bash
find src/test250930 -name "*.java" -print0 \
  | xargs -0 javac -cp "$CLASSPATH" -d bin/test250930
```

- `find` は `src/test250930` 以下の `.java` ファイルをすべて列挙する。`-print0` を付けることで
  ファイル名にスペースが含まれても安全に扱える。
- `xargs -0` は `find` の出力（ヌル区切り）を `javac` の引数に渡す。これにより 1 回のコマンドで
  全クラスをコンパイルし、結果を `bin/test250930` ディレクトリに出力する。

`javac` 実行時にアノテーションプロセッサに関する警告が表示されることがあるが、
本ワークフローでは無視して問題ない。

## 4. バッチ実行コマンド

### Linux / macOS (POSIX シェル)

```bash
java -cp "bin/test250930:$CLASSPATH" \
  repast.simphony.batch.BatchMain "$(pwd)/test250930.rs"
```

- `-cp` にはコンパイル済みクラス (`bin/test250930`) と Repast ランタイムのクラスパスを
  コロン (`:`) で連結して指定する。
- 第 2 引数にはシナリオディレクトリの絶対パスを渡す。`launch.props` が相対パスを
  参照しているため、`$(pwd)` を用いて完全修飾パスにするのがポイント。

### Windows (PowerShell / CMD)

ユーザー環境では次のように実行する。クラスパス区切りがセミコロンになる点と、
バッチモードエントリポイントが `RepastBatchMain` である点が Linux 版との違い。

```powershell
java -cp "bin;${CLASSPATH_WIN}" \
  repast.simphony.runtime.RepastBatchMain
```

- `CLASSPATH_WIN` には `scripts\repast_classpath.bat` などで生成したセミコロン区切りの
  文字列を格納しておくと良い。
- `RepastBatchMain` は GUI を起動せずにシナリオを進行させるため、コンテナ環境や
  サーバー上でも `HeadlessException` を避けられる。

## 5. ログと確認ポイント

`launch.props` に設定された `logging.outputs` に従い、コンソール・ファイル・CSV・JSON
の各形式でイベントログが出力される。複数回の試行を行う場合は、実行前に `logs/`
ディレクトリを確認し不要なファイルを整理しておくと比較結果が取りやすい。

## 6. トラブルシュート

- `ClassNotFoundException` が発生する場合は、`CLASSPATH` または `CLASSPATH_WIN` の内容に
  `lib/repast-2.11.0/` 配下の JAR が含まれているか確認する。
- `HeadlessException` が出た場合は `RepastMain` を呼び出していないか、`RepastBatchMain`
  に切り替えているかを再確認する。
- `find` コマンドが見つからない場合は Git Bash や WSL など POSIX 対応シェルを利用する。

この手順で C++ 側の計算結果と Repast 港内モデルを結びつけたシナリオを再構成し、
コンテナ内でも Windows 環境でも同一の挙動で実行できる。

# Repast ランタイム構築とバッチ実行手順（日本語まとめ）

このドキュメントは、コンテナ環境上で Repast Simphony 2.11.0 を用いた港内モデルをビルドし、
バッチモードで実行するまでの流れを日本語で整理したものです。C++ 側で算出した港外フローと
連携させることを前提に、Java プロジェクトの構成・ビルド・実行方法、ならびに周辺スクリプトの
役割を詳しく解説します。

## 1. リポジトリ構成の再確認

```
repast-test/
├── bin/                 # 既存の Java バイトコード出力先（test250930 パッケージ）
├── docs/                # 実行手順やサンプルログなどのドキュメント
├── scripts/             # Repast ランタイム取得とクラスパス生成スクリプト
├── src/test250930/      # 港内エージェントやスケジューラの Java ソース
├── test250930.rs/       # Repast シナリオ（scenario.xml, launch.props など）
└── Repast.settings      # Repast Launcher 用設定
```

`src/test250930` 配下が本モデルのエージェント実装本体です。`docs/container-repast-runtime.md` は
英語版手順書、`README.md` はバッチ実行の概要とログ出力設定を記載しています。

## 2. Repast ランタイムのダウンロードと展開

リポジトリには Repast の本体が含まれないため、まずは公式更新サイトから 2.11.0 のプラグインを
取得します。`scripts/setup_repast_runtime.sh` がダウンロードから classpath 生成までを自動化して
いるので、このスクリプトを実行します。

```bash
./scripts/setup_repast_runtime.sh
```

- `curl` でアーカイブをダウンロードし、一時ディレクトリに展開
- `lib/repast-2.11.0/` 以下へ各プラグイン JAR をコピー
- JAR 内の `bin/` `classes/` ディレクトリを抽出し、クラスパス情報 (`classpath.txt`) を生成

クラスパスは複数行のテキストとして `lib/repast-2.11.0/classpath.txt` に保存されます。

## 3. クラスパス文字列の生成

複数行のクラスパスを Java 実行時に利用できるよう、`scripts/repast_classpath.sh` を利用します。

```bash
CLASSPATH=$(./scripts/repast_classpath.sh)
```

このスクリプトは `lib/repast-*/classpath.txt` のうち最新版を選び、コメント行 (`# ...`) を除外
したうえで `:` 区切りに連結します。Windows PowerShell や `cmd.exe` で利用する場合は、区切り文字を
`;` に置き換えた `CLASSPATH_WIN` 環境変数を用意します。

```powershell
$env:CLASSPATH_WIN = (Get-Content .\lib\repast-2.11.0\classpath.txt | Where-Object { $_ -notmatch '^#' }) -join ';'
```

## 4. Java ソースの再コンパイル

ソースコードを編集した場合は `javac` で再コンパイルします。Unix シェルでは `find` と `xargs` を
組み合わせることで、すべての Java ファイルを一括でコンパイルできます。

```bash
find src/test250930 -name "*.java" -print0 \
  | xargs -0 javac -cp "${CLASSPATH}" -d bin
```

- `find ... -print0` はファイル名にスペースが含まれても安全に処理できるよう終端を NUL 文字に
  しています。
- `xargs -0` が `find` の出力を受け取り、`javac` にまとめて渡します。
- `-cp "${CLASSPATH}"` で先ほど生成した Repast 依存のクラスパスを指定し、`-d bin` で
  バイトコードの出力先を既存の `bin` ディレクトリに揃えます。

Windows 環境であれば `Get-ChildItem` などを組み合わせても構いませんが、WSL や Git Bash を使用
する場合は上記コマンドがそのまま適用できます。

## 5. バッチモードでの実行

ヘッドレス環境では GUI を持つ `RepastMain` ではなく、バッチ用エントリポイントを利用します。
プラットフォームごとのクラスパス区切りに注意してください。

### 5.1 Linux / macOS

`test250930.rs/batch_params.xml` には単一実行を指示するパラメータスイープが定義されています。
バッチランナーを呼ぶ際は `-params` オプションでこのファイルを渡し、JDK17 以降で必須となる
`--add-opens` オプションも併せて指定します。

```bash
java --add-opens java.base/java.lang=ALL-UNNAMED \
  -cp "bin:${CLASSPATH}" \
  repast.simphony.runtime.RepastBatchMain \
  -params "$(pwd)/test250930.rs/batch_params.xml" \
  "$(pwd)/test250930.rs"
```

### 5.2 Windows（ユーザー指定コマンド）

```powershell
java --add-opens java.base/java.lang=ALL-UNNAMED `
  -cp "bin;${env:CLASSPATH_WIN}" `
  repast.simphony.runtime.RepastBatchMain `
  -params "$(Get-Location)\test250930.rs\batch_params.xml" `
  "$(Get-Location)\test250930.rs"
```

`RepastBatchMain` は `BatchMain` と同様に GUI を起動せず、`launch.props` の設定を参照して
シナリオを実行します。`$(pwd)/test250930.rs` や `$(Get-Location)\test250930.rs` のように絶対パス
を渡すことで、シナリオ内の `../bin` 参照が正しく解決されます。

## 6. トラブルシューティング（よくあるエラー）

### 6.1 `scenario.xml\user_path.xml` が見つからない

`RepastBatchMain` / `BatchMain` に **シナリオファイル (`scenario.xml`) のパス** を渡した場合、
ランタイムは `scenario.xml` をディレクトリとして扱おうとするため
`scenario.xml\user_path.xml` を参照し、`指定されたパスが見つかりません` というエラーになります。
必ず **シナリオディレクトリ** (`.../test250930.rs`) の絶対パスを引数に指定してください。

```bash
# ✅ 正しい例（Git Bash / MINGW64）
java --add-opens java.base/java.lang=ALL-UNNAMED \
  -cp "bin;${CLASSPATH_WIN}" \
  repast.simphony.runtime.RepastBatchMain \
  -params "$(pwd -W)\\test250930.rs\\batch_params.xml" \
  "$(pwd -W)\\test250930.rs"

# ❌ 誤った例（scenario.xml を直接指定してしまうケース）
java --add-opens java.base/java.lang=ALL-UNNAMED \
  -cp "bin;${CLASSPATH_WIN}" \
  repast.simphony.runtime.RepastBatchMain \
  -params "$(pwd -W)\\test250930.rs\\batch_params.xml" \
  ./test250930.rs/scenario.xml
```

### 6.2 `unknown plug-in ID - saf.core.runtime`

GUI ランチャーである `repast.simphony.runtime.RepastMain` をヘッドレス環境（X サーバが無い環境）で
起動すると、OSGi プラグインの初期化に失敗して上記のエラーが発生します。バッチ実行のみが
目的の場合は、常に `repast.simphony.batch.BatchMain` または `repast.simphony.runtime.RepastBatchMain`
を使用してください。これらは GUI を必要とせず、`launch.props` と `parameters.xml` を読み込んで
シミュレーションを自動で進行します。

## 7. ログ出力と確認ポイント

`test250930.rs/launch.props` にはログ出力の設定があり、デフォルトで `console,file` が有効です。
実行すると標準出力に各エージェントのイベントが記録され、`logs/` 配下に `.log` `.csv` `.json`
が生成されます。これらは C++ 側の外洋シミュレーションとの同期や可視化に利用できます。

## 8. 反復実行のためのヒント

- 同じコンテナセッション内であれば、セットアップ済みの `lib/repast-2.11.0/` を再利用できます。
- スクリプトや Makefile を追加し、`setup → compile → run` を一括で呼び出すと反復検証が容易です。
- C++ 側からの入力データを CSV 等で受け取り、`src/test250930` 内のエージェントに読み込ませる
  際は、再コンパイル前にファイルの配置や形式を確認してください。

以上の手順で、Repast ベースの港内モデルを確実に組み立て・実行できます。`find` をはじめとする
ユーティリティの挙動を理解しておくと、変更点が増えた際にも安全かつ効率的にビルドできます。

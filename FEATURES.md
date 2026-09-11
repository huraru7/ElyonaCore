# ElyonaCore — 実装機能一覧

> Paper API 1.21.1 製 Minecraft サーバープラグイン  
> Elyona World の全プラグインが依存する中核基盤

---

## 目次

1. [経済システム (Cred)](#1-経済システム-cred)
2. [MIMICメッセージシステム](#2-mimicメッセージシステム)
3. [ランクシステム](#3-ランクシステム)
4. [称号システム](#4-称号システム)
5. [プレイヤーデータ管理](#5-プレイヤーデータ管理)
6. [データベース層](#6-データベース層)
7. [カスタムイベント（他プラグイン連携用）](#7-カスタムイベント他プラグイン連携用)
8. [コマンド一覧](#8-コマンド一覧)
9. [パーミッション一覧](#9-パーミッション一覧)
10. [設定ファイル](#10-設定ファイル)

---

## 1. 経済システム (Cred)

### 概要
サーバー独自の通貨 **Cred (Cr)** を管理する。**Vault Economy** APIを実装しているため、
ShopGUI+・Essentials・その他 Vault 対応プラグインとそのまま連携できる。

### 特徴
- 通貨単位は **整数のみ**（小数点なし）
- オンラインプレイヤーの残高はメモリキャッシュで管理し、ログアウト時にDBへ書き戻す
- **サーバー口座** (UUID: `00000000-0000-0000-0000-000000000000`) が存在し、税収・ペナルティの受け皿になる

### 初回参加ボーナス
新規プレイヤーが初めてサーバーに参加した際、自動で **500 Cr** を付与する。
`initial_grant_done` フラグで重複付与を防止。

### 送金 (`/pay`)
- 5% の取引税が送金者から徴収される（`ceil(amount × 0.05)`で切り上げ）
- 税は**サーバー口座**に蓄積される
- 自分自身への送金不可・残高不足チェック済み
- 送金完了時に `ElyonaPayEvent` が発火（他プラグインがフック可能）

**例：** `/pay Alice 1000` の場合
| 操作 | 金額 |
|------|------|
| 送金者から引かれる | -1050 Cr（1000 + 税50） |
| 受取人が受け取る | +1000 Cr |
| サーバー口座に入る | +50 Cr |

---

## 2. MIMICメッセージシステム

### 概要
プラグインがプレイヤーに送るシステムメッセージをすべて統一フォーマット `[MIMIC] 「…」` で表示する。

### カラー
| 部位 | カラーコード |
|------|-------------|
| `[MIMIC]` プレフィックス | `#7F77DD`（紫がかった青） |
| メッセージ本文 | `#D3D1C7`（明るいグレー） |

### API
他プラグインから `ElyonaCorePlugin.getInstance().getMimicMessenger()` で取得して使用可能。

| メソッド | 説明 |
|---------|------|
| `sendTo(Player, String)` | 特定プレイヤーに送信 |
| `broadcast(String)` | 全プレイヤーにブロードキャスト |
| `broadcastWorld(World, String)` | 特定ワールドの全員に送信 |
| `build(String)` | `Component` だけ返す（GUIタイトル等に利用） |

---

## 3. ランクシステム

### 概要
シーズン制の経験値（EXP）ベースランクシステム。EXPを蓄積するとランクが上がり、
報酬（Cr）と称号が自動付与される。

### ランク階層

| ランク | 必要累計EXP | ランクアップ報酬 | 付与称号 |
|--------|------------|----------------|----------|
| Bronze | 0 | — | — |
| Silver | 1,000 | 500 Cr | 銀の探索者 |
| Gold | 3,000 | 1,500 Cr | 黄金の戦士 |
| Platinum | 7,000 | 3,000 Cr | 白金の勇者 |
| Diamond | 15,000 | 6,000 Cr | 輝きの覇者 |
| Legend | 30,000 | 15,000 Cr | 伝説の勇者 |

### ランクアップ時の処理（自動）
1. `ElyonaRankUpEvent` 発火
2. MIMICメッセージで本人と全体に通知
3. Cr報酬を残高に加算
4. 対応称号を自動付与（未所持の場合）

### シーズン制
- `elyona_seasons` テーブルで管理。複数シーズンの履歴が残る
- シーズン終了時（`/season end`）：全プレイヤーのランクが **Bronze にリセット**される
- ランクデータは `(uuid, season_id)` の複合キーで保存されるため過去シーズンの記録は消えない

### ランクGUI (`/rank`)
27スロットのチェストGUIで現在状況を表示：

```
[ ][ ][ ][ ][タイトル][ ][ ][ ][ ]  ← スロット 0-8
[前ランク×3][  現ランク  ][次ランク×3]  ← スロット 9-17
[████████░░░░░░░░░░░░░░░░░]           ← スロット 18-26（EXP進捗バー）
```

- 現ランクアイコン: ランク色のステンドグラスパネル + 現EXP/次ランク必要EXP を lore に表示
- 進捗バー: スロット9マスを `LIME_STAINED_GLASS_PANE`（達成）/ `GRAY_STAINED_GLASS_PANE`（未達成）で塗り分け

---

## 4. 称号システム

### 概要
プレイヤーが獲得した称号を装備すると、LuckPerms の prefix が書き換わり
ネームタグ・チャット欄・Tab補完に表示される。

### 称号定義（`titles.yml`）

| 称号ID | 表示名 | 入手条件 |
|--------|--------|----------|
| `rank_silver` | 銀の探索者 | Silverランク到達 |
| `rank_gold` | 黄金の戦士 | Goldランク到達 |
| `rank_platinum` | 白金の勇者 | Platinumランク到達 |
| `rank_diamond` | 輝きの覇者 | Diamondランク到達 |
| `rank_legend` | 伝説の勇者 | Legendランク到達 |
| `season_legend` | Season {season} Legend | シーズンLegend達成 |
| `territory_owner` | テリトリーの覇者 | 特別付与（管理者） |

### 装備の仕組み
1. `/title set <id>` → LuckPerms の全 prefix をクリアし新しい prefix を登録
2. prefix は `[称号名] ` の形式（優先度 100）
3. `/title clear` → prefix を削除（空白称号）

### 称号選択GUI (`/title`)
- 所持称号の数に応じて **動的サイズ**（行数 = `ceil(称号数 / 9)`、最大6行54スロット）
- 装備中の称号は `ENCHANTED_BOOK` で強調表示、未装備は `BOOK`
- クリックで即座に装備切り替え

### API（他プラグイン向け）
```java
TitleManager tm = ElyonaCorePlugin.getInstance().getTitleManager();
tm.grant(player, "territory_owner");   // 称号付与 + ElyonaTitleGrantEvent 発火
tm.has(uuid, "rank_silver");           // 所持確認
tm.setActive(player, "rank_gold");     // 装備（LuckPerms prefix 更新）
tm.clearActive(player);                // 装備解除
tm.getActive(uuid);                    // 現在装備中の称号ID
```

---

## 5. プレイヤーデータ管理

### 保存データ（`elyona_players`）
| カラム | 内容 |
|--------|------|
| `uuid` | プレイヤー UUID（主キー） |
| `name` | プレイヤー名（最終ログイン時に更新） |
| `first_join` | 初回参加日時 |
| `last_seen` | 最終ログアウト日時 |
| `initial_grant_done` | 初回ボーナス付与済みフラグ |

### ライフサイクル
```
参加 → DB upsert（名前・uuid）→ 残高ロード → 称号ロード → ランクロード → 初回ボーナス判定
退出 → 残高DB書き込み → last_seen 更新
停止 → 全オンラインプレイヤーをフラッシュ
```

---

## 6. データベース層

### 対応DB
| モード | 備考 |
|--------|------|
| **SQLite**（デフォルト） | ファイルDB。シングルスレッド制約あり（pool size = 1） |
| **MySQL** | 本番推奨。`config.yml` の `database.type: mysql` で切り替え |

### テーブル構成

```
elyona_players       プレイヤー基本情報
elyona_economy       残高（players に FK）
elyona_titles        称号所持履歴（players に FK）
elyona_active_title  装備中称号（players に FK）
elyona_seasons       シーズン一覧
elyona_ranks         ランク/EXP（players + seasons に FK）
```

### 非同期クエリAPI
```java
// SELECT: T型で結果を返す
databaseManager.queryAsync(sql, ps -> ps.setString(1, uuid), rs -> rs.getLong("balance"))
    .thenAccept(balance -> ...);

// INSERT/UPDATE/DELETE: 戻り値なし
databaseManager.executeAsync(sql, ps -> { ps.setLong(1, amount); ps.setString(2, uuid); });
```

---

## 7. カスタムイベント（他プラグイン連携用）

ElyonaCore は以下のカスタムイベントを発火する。他プラグインはこれらを `@EventHandler` で受け取れる。

### `ElyonaPayEvent`
送金完了時に発火。

| フィールド | 型 | 内容 |
|-----------|-----|------|
| `sender` | `Player` | 送金者 |
| `receiver` | `Player` | 受取人 |
| `amount` | `long` | 送金額（税抜き） |
| `tax` | `long` | 徴収された税額 |

### `ElyonaRankUpEvent`
ランクアップ時に発火。

| フィールド | 型 | 内容 |
|-----------|-----|------|
| `player` | `Player` | 対象プレイヤー |
| `oldTier` | `RankTier` | ランクアップ前 |
| `newTier` | `RankTier` | ランクアップ後 |

### `ElyonaTitleGrantEvent`
称号付与時に発火（重複付与時は発火しない）。

| フィールド | 型 | 内容 |
|-----------|-----|------|
| `player` | `Player` | 対象プレイヤー |
| `titleId` | `String` | 付与された称号ID |

---

## 8. コマンド一覧

### 経済コマンド

| コマンド | 説明 | 権限 |
|---------|------|------|
| `/balance` `/bal` | 自分の残高を確認 | 全員 |
| `/pay <player> <amount>` | 送金（5%税あり） | 全員 |
| `/eco give <player> <amount>` | 残高を加算 | `elyona.admin` |
| `/eco take <player> <amount>` | 残高を減算 | `elyona.admin` |
| `/eco set <player> <amount>` | 残高を設定 | `elyona.admin` |

### ランクコマンド

| コマンド | 説明 | 権限 |
|---------|------|------|
| `/rank` | 自分のランクGUIを開く | 全員 |
| `/rank <player>` | 他プレイヤーのランクGUIを開く | 全員 |
| `/season start <name>` | 新シーズン開始 | `elyona.admin` |
| `/season end` | 現シーズン終了（全員Bronzeリセット） | `elyona.admin` |

### 称号コマンド

| コマンド | 説明 | 権限 |
|---------|------|------|
| `/title` | 称号選択GUIを開く | 全員 |
| `/title set <id>` | 称号を装備 | 全員 |
| `/title clear` | 称号を外す | 全員 |
| `/title grant <player> <id>` | 称号を付与 | `elyona.admin` |

---

## 9. パーミッション一覧

| パーミッション | 説明 | デフォルト |
|--------------|------|-----------|
| `elyona.admin` | 管理者コマンド（`/eco`, `/season`, `/title grant`） | OP のみ |

---

## 10. 設定ファイル

### `config.yml`

```yaml
database:
  type: sqlite          # sqlite / mysql
  sqlite:
    file: elyona_data.db
  mysql:
    host: localhost
    port: 3306
    database: elyona
    username: root
    password: ""        # ← 本番環境で変更すること

economy:
  currency_name: "Cred"
  currency_symbol: "Cr"
  initial_grant: 500    # 初回ボーナス
  transaction_tax: 0.05 # 送金税率（5%）

rank:
  exp_requirements:
    SILVER: 1000
    GOLD: 3000
    PLATINUM: 7000
    DIAMOND: 15000
    LEGEND: 30000
  rankup_rewards:
    SILVER: 500
    GOLD: 1500
    PLATINUM: 3000
    DIAMOND: 6000
    LEGEND: 15000

mimic:
  prefix_color: "#7F77DD"  # [MIMIC] の色
  text_color: "#D3D1C7"    # 本文の色
```

### `titles.yml`
称号を追加・編集できる。`source` フィールドは参考情報のみ（付与はコードで制御）。

```yaml
titles:
  <称号ID>:
    display: "表示名"
    description: "説明文"
    source: RANK / SEASON / SPECIAL
```

---

## 依存プラグイン

| プラグイン | バージョン | 用途 |
|-----------|-----------|------|
| **Vault** | 1.7.x+ | Economy API プロバイダとして登録 |
| **LuckPerms** | 5.4+ | 称号のprefix管理 |

> `plugin.yml` に `depend: [Vault, LuckPerms]` があるため、両プラグインが存在しないとElyonaCoreは起動しない。

---

## ビルド方法

```bash
# 日本語ユーザーディレクトリ対応の特殊ビルドコマンド
GRADLE_USER_HOME="C:/gradle-home" \
JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.5.11-hotspot" \
PATH="/c/Program Files/Eclipse Adoptium/jdk-21.0.5.11-hotspot/bin:$PATH" \
./gradlew build -g "C:/gradle-home"
```

**出力ファイル：**
- `build/libs/ElyonaCore-1.0.0.jar` — サーバー投入用（reobfuscated）

# ElyonaCore

Elyona World の各プラグインが共通で利用する基盤プラグイン。プレイヤーデータの永続化(SQLite/MySQL)、MIMICメッセージ表示、共通イベント定義などを提供する。

## 主な機能

- プレイヤーデータの永続化(`DatabaseManager`) — SQLite / MySQL 両対応
- MIMIC端末のメッセージ表示基盤(`MimicMessenger`)
- 参加・退出時の共通処理(`PlayerJoinListener` / `PlayerQuitListener`)
- 他プラグイン(Economy/Items/Menu/Rank等)から利用する共通イベント定義

## 依存関係

- Paper 1.21.1
- [LuckPerms](https://luckperms.net/)

## ビルド

```
./gradlew build
```

Java 21 / Paper 1.21.1 (paperweight userdev) を使用。

## 設定

`config.yml` でデータベース種別(sqlite/mysql)や接続情報を設定する。MySQL利用時は環境ごとにサーバー上の設定ファイルを直接編集し、認証情報をリポジトリにコミットしないこと。

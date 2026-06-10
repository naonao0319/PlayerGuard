# PlayerGuard (naonao0319 fork)

WorldGuard を用いた、プレイヤー向けの土地保護プラグインです。プレイヤーが自分で範囲を保護し、メンバーやフラグを GUI から管理できます。

> このリポジトリは [TeamNekozouneko/PlayerGuard](https://github.com/TeamNekozouneko/PlayerGuard) の **fork** です。
> - fork: https://github.com/naonao0319/PlayerGuard
> - fork元: https://github.com/TeamNekozouneko/PlayerGuard
>
> 元のライセンス・著作権を尊重し、GPL-3.0 のもとで公開しています。

**PlayerGuard は再読み込みできません！ 設定変更後は再起動をしてください。**

## このforkでの主な変更点
- `/pg` で **管理ハブGUI** を開けるように（フラグ・メンバー管理・領域情報への入口）
- **メンバー管理を GUI 化**（追加・削除・譲渡をクリックで操作）
- **フラグを拡張**（爆発・延焼・モブ湧き・アイテム・環境変化を追加。GUI からトグル）
- 領域の権限を **主オーナー / subowner / builder** に分割＋建築権の **期限付き貸出** を追加
- サブコマンドの **エイリアス不具合を修正**＋短縮形を追加
- GitHub Actions による **自動リリース** を追加

## 必要なもの
- **WorldGuard**（およびその前提である **WorldEdit**）
- Spigot / Paper 系サーバー（API 1.13 以上）

## 導入
サーバーを停止し、`plugins/` フォルダに jar を入れて起動するだけで使えます。WorldGuard・WorldEdit も同じく導入してください。

## 使い方

### 領域の保護
1. 範囲を選択して `/claim`（別名 `/hogo`）で保護を取得します。
2. 解除は `/disclaim`、選択のキャンセルは `/cancel` です。

### `/pg` — 管理メニュー
自分の保護領域の中で `/pg` を実行すると **管理ハブGUI** が開きます。

- **フラグ設定** … 領域内のルールを許可／拒否で切り替え（下記フラグ参照）
- **メンバー管理** … subowner/builder の一覧／追加（オンラインから選択）／操作（クリック）／建築権の貸出／領域の譲渡
- **領域情報** … ID・座標範囲・オーナー/メンバー数を表示

領域の外で `/pg` を実行すると、自分の保護一覧（座標つき）をテキストで表示します。

### ロール（主オーナー / subowner / builder）
このforkでは、領域の権限を次の3段階に分けています。

- **主オーナー** … 譲渡・領域削除・subownerの任免ができる
- **subowner** … 建築・フラグ設定・builderの追加/削除・貸出管理ができる（譲渡/削除/subowner任免は不可）
- **builder** … 建築のみ（期限付き貸出で一時的に付与される場合あり）

### 設定できるフラグ
GUI の「フラグ設定」から、各項目を **許可 → 拒否 → 設定解除** で切り替えられます。

**1行目（基本）**
| フラグ | 内容 |
|---|---|
| ブロックの破壊 | メンバー以外のブロック破壊 |
| ブロックの設置 | メンバー以外のブロック設置 |
| アイテムの使用、チェストを開く | ボタン・チェスト・金床などの操作 |
| PvP | プレイヤー同士のダメージ |
| エンティティへのダメージ | 動物などへの攻撃 |
| メンバー以外の侵入 | 領域への立ち入り |
| ピストンの使用 | ピストン・含水葉の動作 |

**2行目（ワールド/環境）**
| フラグ | 内容 |
|---|---|
| 爆発によるダメージ | クリーパー / TNT / その他の爆発 |
| 火の延焼 | 延焼・溶岩による着火 |
| モブのスポーン | 領域内でのモブの自然湧き |
| アイテムのドロップ・拾う | アイテムの投棄・取得 |
| 環境変化 | 葉の消滅・草/キノコ/ツタ/作物の成長 |

### コマンド一覧
| コマンド | 別名 | 説明 |
|---|---|---|
| `/claim` | `/hogo` | 選択範囲を保護 |
| `/disclaim` | `/remove-hogo`, `/hogo-remove` | 保護を解除 |
| `/cancel` | `/cancel-claim` | 範囲選択をキャンセル |
| `/flags` | | フラグ設定GUIを開く（領域内） |
| `/pg` | `/guard`, `/pg` | 管理ハブGUI（領域内）/ 情報表示（領域外） |
| `/pg add <player>` | | メンバーを追加 |
| `/pg remove <player>` | `rm`, `del` | メンバーを削除 |
| `/pg transfer <player>` | `give` | 領域を譲渡（相手が `/pg confirm` で受理） |
| `/pg confirm` | `yes` | 移管リクエストを受理 |
| `/pg info` | `i` | 自分の保護情報を表示 |
| `/pg-admin` | `/pguard-admin`, `/pg-admin` | 管理者用（deleteall/deletesection/expand/lookup） |

> 領域の譲渡は、譲渡先プレイヤーに届くリクエストを相手自身が `/pg confirm` で受理して初めて完了します。受理後、その領域は譲渡先の保護量に加算されます。

## 設定（config.yml）
- `protection.limit` … プレイ日数に応じた保護できる総体積の上限
- `protection.flags` … 各フラグの既定値（`false` にするとそのフラグを GUI から操作不可にできます）
- `protection.spacing` … 領域同士の最小間隔
- `visitor-log` … 訪問者ログ（有効/無効、保持件数、保存間隔、記録対象/イベント、閲覧権限）
- `permissions` … ロール関連の権限制御（例: subownerに譲渡/削除を許可するか）

## ビルド
Maven でビルドできます。Minecraft 1.21 系に対応するため **JDK 21** が必要です。
```
mvn clean package
```
`target/PlayerGuard-x.x.x.jar`（shade 済み）が生成されます。

## リリース
`v` で始まるタグ（例: `v1.4.2`）を push すると、GitHub Actions が自動でビルドして Release に jar を添付します。

## ライセンス
このプロジェクトは [GPL-3.0](LICENSE) でライセンスされています。元の著作権は [TeamNekozouneko](https://github.com/TeamNekozouneko/PlayerGuard) に帰属します。

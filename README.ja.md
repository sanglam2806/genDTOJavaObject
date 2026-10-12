# codegen

[English](README.md) | [日本語](README.ja.md)

[FreeMarker](https://freemarker.apache.org/) のテンプレートを使って、テーブル定義から DTO インターフェース、実装クラス、Hibernate マッピングファイルを生成する小さなコードジェネレーターです。

## 生成されるファイル

テーブル `COM_MITSURU_NA`、パッケージ `com.mitsuru.gen` の場合、`src/main/java/com/mitsuru/gen/dto/` に次のファイルが作成されます。

| ファイル | テンプレート | 内容 |
|---|---|---|
| `ComMitsuruNaDto.java` | `Dto.ftl` | 各カラムの getter / setter を持つインターフェース |
| `ComMitsuruNaDtoImpl.java` | `DtoImpl.ftl` | インターフェースを実装し、フィールドを持つクラス |
| `ComMitsuruNa.hbm.xml` | `Hbm.ftl` | Hibernate マッピング(主キーは `<id>`、それ以外は `<property>`) |

ファイルが既に存在する場合はスキップされます(`SKIP (exists)`)。再生成するには、先にファイルを削除してください。

## 構成

```
freemarker-2.3.34.jar                  FreeMarker ライブラリ
lib/                                   JDBC ドライバーの JAR(Oracle / PostgreSQL / MySQL)
codegen.properties                     DB 接続とデフォルト設定
gen.sh, gen.bat                        Gradle を使わずにビルド・実行するスクリプト
src/main/java/com/mitsuru/codegen/
  CodegenApplication.java              ジェネレーター(メインクラス、コマンドライン引数)
  DbHub.java                           選択した DB への接続を作成
  TableReader.java                     JDBC メタデータからカラムを読み込み、型を変換
src/main/resources/templates/
  Dto.ftl, DtoImpl.ftl, Hbm.ftl        FreeMarker テンプレート
```

## 設定

テーブル定義(カラム名、コメント、データ型、主キー、`NOT NULL`)はデータベースから読み込みます。コード内に固定値はありません。

接続はプロジェクトのルートにある `codegen.properties` で設定します。DB ごとに設定を持つため、`db` を変更するだけで切り替えられます。

```properties
db=postgres                 # oracle | postgres | mysql

package=com.mitsuru.gen     # 生成コードのパッケージ
out=src/main/java           # 出力先のルート

oracle.url=jdbc:oracle:thin:@localhost:1521/XEPDB1
oracle.user=system
oracle.password=
oracle.schema=              # 省略可。デフォルトはログインユーザーのスキーマ

postgres.url=jdbc:postgresql://localhost:5432/postgres
postgres.user=postgres
postgres.password=
postgres.schema=            # 省略可。デフォルトは現在のスキーマ(public)

mysql.url=jdbc:mysql://localhost:3306/test   # URL のデータベースを使用
mysql.user=root
mysql.password=
```

テーブル名の大文字・小文字は区別しません(`COM_MITSURU_NA` で PostgreSQL の `com_mitsuru_na` も見つかります)。クラス名はテーブル名から作られます:`com_mitsuru_na` → `ComMitsuruNa`。

### コマンドライン引数

```
gen [--db=oracle|postgres|mysql] [--package=...] [--out=...] [--config=...] TABLE [TABLE...]
```

オプションは `codegen.properties` の値より優先されます。テーブルは複数指定できます。

### 型の対応

JDBC の型から変換するため、3 つの DB で同じように動作します。

| DB 型 | Java 型 | Hibernate 型 |
|---|---|---|
| `CHAR`、`VARCHAR`、`VARCHAR2`、`NVARCHAR*`、`TEXT` | `String` | `string` |
| `CLOB`、`NCLOB`、`LONGTEXT` | `String` | `text` |
| 小数部なしの `NUMBER`/`NUMERIC`/`DECIMAL`(桁数 ≤ 9)、`INT`、`SMALLINT`、`TINYINT` | `Integer` | `integer` |
| `NUMBER`/`NUMERIC`(桁数 ≤ 18)、`BIGINT` | `Long` | `long` |
| `NUMBER`/`NUMERIC`(桁数 > 18、小数部あり、桁数指定なし) | `java.math.BigDecimal` | `big_decimal` |
| `BOOLEAN`、`BIT(1)`、MySQL の `TINYINT(1)` | `Boolean` | `boolean` |
| `REAL`、`FLOAT`、`DOUBLE` | `Double` | `double` |
| `DATE`、`TIME`、`TIMESTAMP*`、`DATETIME` | `java.util.Date` | `timestamp` |
| `BLOB`、`BYTEA`、`RAW`、`VARBINARY` | `byte[]` | `binary` |

上記以外の型の場合、`IllegalArgumentException: Unmapped type` で処理が停止します。

## 実行方法

コマンドはすべてプロジェクトのルートで実行してください。出力先はカレントディレクトリからの相対パスです。

### JDBC ドライバー

使用する DB のドライバー JAR を `lib/` に置いてください。Gradle があれば、次のコマンドで 3 つともコピーされます。

```bash
./gradlew copyDrivers
```

Gradle がない場合は Maven Central からダウンロードして `lib/` に置きます:`com.oracle.database.jdbc:ojdbc11`(Java 8 の場合は `ojdbc8`)、`org.postgresql:postgresql`、`com.mysql:mysql-connector-j`。

### Gradle / Maven を使わない場合(スクリプト)

Java 8 以上が必要です。スクリプトはジェネレーターを `out/` にコンパイルしてから実行します。

macOS / Linux:

```bash
./gen.sh COM_MITSURU_NA
./gen.sh --db=oracle COM_MITSURU_NA ORDER_ITEM
```

Windows:

```bat
gen.bat COM_MITSURU_NA
gen.bat --db=mysql --package=com.example order_item
```

IDE を使う場合は、`freemarker-2.3.34.jar` と `lib/` の JAR をライブラリとして追加し、`templates` フォルダーがクラスパスに含まれていることを確認して、テーブル名をプログラム引数に指定してください。

### Gradle を使う場合

```bash
./gradlew bootRun --args="COM_MITSURU_NA"
./gradlew bootRun --args="--db=oracle COM_MITSURU_NA ORDER_ITEM"
```

## テンプレートのカスタマイズ

テンプレートでは次の値を使用できます。

| 名前 | 内容 |
|---|---|
| `pkg` | パッケージ名 |
| `cls` | クラス名(例:`ComMitsuruNa`) |
| `table` | データベース上のテーブル名 |
| `cols` | カラムのリスト |

`cols` の各要素:

| 名前 | 内容 |
|---|---|
| `name` | カラム名 |
| `prop` | プロパティ名(`camelCase`) |
| `comment` | コメント |
| `pk` | 主キーの場合 `true` |
| `nullable` | null 許可の場合 `true` |
| `javaType` | Java 型 |
| `hibType` | Hibernate 型 |

## 制限事項

- 複合主キーのテーブルでは `.hbm.xml` に `<id>` が複数出力されますが、Hibernate では使用できません。手動で `<composite-id>` に変更してください。

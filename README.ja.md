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
freemarker-2.3.34.jar                  FreeMarker ライブラリ(唯一の依存ライブラリ)
src/main/java/com/mitsuru/codegen/
  CodegenApplication.java              ジェネレーター(メインクラス)
src/main/resources/templates/
  Dto.ftl, DtoImpl.ftl, Hbm.ftl        FreeMarker テンプレート
```

## 設定

`CodegenApplication.java` の先頭にある定数を編集します。

| 定数 | 意味 |
|---|---|
| `TABLE_NAME` | テーブル名。クラス名に変換されます:`com_mitsuru_na` → `ComMitsuruNa` |
| `PACKAGE` | 生成コードのパッケージ |
| `COLUMNS` | カラム名(`snake_case`。`camelCase` のプロパティ名に変換されます) |
| `TYPES` | 各カラムの DB 型 |
| `COMMENTS` | 各カラムのコメント |
| `SIZES` | 桁数(`NUMBER` の場合のみ使用) |

主キーのカラムは `getColumns()` 内で指定します(`key.add("id")`)。主キーは `not-null`、それ以外のカラムは null 許可になります。

`COLUMNS`、`TYPES`、`COMMENTS`、`SIZES` は同じ件数・同じ順番にしてください。

### 型の対応

| DB 型 | Java 型 | Hibernate 型 |
|---|---|---|
| `VARCHAR*`、`NVARCHAR*`、`CHAR` | `String` | `string` |
| `NUMBER`(桁数 ≤ 9) | `Integer` | `integer` |
| `NUMBER`(桁数 ≤ 18) | `Long` | `long` |
| `NUMBER`(桁数 > 18) | `java.math.BigDecimal` | `big_decimal` |
| `DATE`、`TIMESTAMP*` | `java.util.Date` | `timestamp` |

上記以外の型の場合、`IllegalArgumentException: Unmapped type` で処理が停止します。

## 実行方法

コマンドはすべてプロジェクトのルートで実行してください。出力先はカレントディレクトリからの相対パスです。

### Gradle / Maven を使わない場合(javac のみ)

Java 8 以上が必要です。

macOS / Linux:

```bash
javac -encoding UTF-8 -cp freemarker-2.3.34.jar -d out src/main/java/com/mitsuru/codegen/CodegenApplication.java
java -cp out:src/main/resources:freemarker-2.3.34.jar com.mitsuru.codegen.CodegenApplication
```

Windows(`:` の代わりに `;` を使います):

```bat
javac -encoding UTF-8 -cp freemarker-2.3.34.jar -d out src\main\java\com\mitsuru\codegen\CodegenApplication.java
java -cp out;src\main\resources;freemarker-2.3.34.jar com.mitsuru.codegen.CodegenApplication
```

テンプレートはクラスパス上の `/templates` から読み込まれるため、`src/main/resources` をクラスパスに含める必要があります。

IDE を使う場合は、`freemarker-2.3.34.jar` をライブラリとして追加し(Eclipse:ビルド・パス → 外部 JAR の追加、IntelliJ IDEA:Project Structure → Libraries)、`templates` フォルダーがクラスパスに含まれていることを確認してください。

### Gradle を使う場合

```bash
./gradlew bootRun
```

## テンプレートのカスタマイズ

テンプレートでは次の値を使用できます。

| 名前 | 内容 |
|---|---|
| `pkg` | パッケージ名 |
| `cls` | クラス名(例:`ComMitsuruNa`) |
| `table` | テーブル名(大文字) |
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

- カラム定義はコード内に固定で書かれています。データベースからの読み込みはまだ実装されていません。
- `nullable` はデータベースから取得していません。主キー以外のカラムはすべて null 許可として扱われます。

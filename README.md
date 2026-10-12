# codegen

[English](README.md) | [日本語](README.ja.md)

A small code generator that uses [FreeMarker](https://freemarker.apache.org/) templates to create a DTO interface, its implementation class and a Hibernate mapping file from a table definition.

## Generated files

For the table `COM_MITSURU_NA` and the package `com.mitsuru.gen`, the generator creates these files in `src/main/java/com/mitsuru/gen/dto/`:

| File | Template | Contents |
|---|---|---|
| `ComMitsuruNaDto.java` | `Dto.ftl` | Interface with a getter and setter for each column |
| `ComMitsuruNaDtoImpl.java` | `DtoImpl.ftl` | Class with fields that implements the interface |
| `ComMitsuruNa.hbm.xml` | `Hbm.ftl` | Hibernate mapping (`<id>` for primary key columns, `<property>` for the others) |

If a file already exists, it is skipped (`SKIP (exists)`). To generate it again, delete the file first.

## Project layout

```
freemarker-2.3.34.jar                  FreeMarker library (the only dependency)
src/main/java/com/mitsuru/codegen/
  CodegenApplication.java              Generator (main class)
src/main/resources/templates/
  Dto.ftl, DtoImpl.ftl, Hbm.ftl        FreeMarker templates
```

## Configuration

Edit the constants at the top of `CodegenApplication.java`:

| Constant | Meaning |
|---|---|
| `TABLE_NAME` | Table name. It is converted to the class name: `com_mitsuru_na` → `ComMitsuruNa` |
| `PACKAGE` | Package of the generated code |
| `COLUMNS` | Column names (`snake_case`, converted to `camelCase` properties) |
| `TYPES` | DB type of each column |
| `COMMENTS` | Comment for each column |
| `SIZES` | Number of digits (used only for `NUMBER`) |

The primary key columns are set in `getColumns()` (`key.add("id")`). Primary key columns are `not-null`; the other columns are nullable.

`COLUMNS`, `TYPES`, `COMMENTS` and `SIZES` must have the same number of items, in the same order.

### Type mapping

| DB type | Java type | Hibernate type |
|---|---|---|
| `VARCHAR*`, `NVARCHAR*`, `CHAR` | `String` | `string` |
| `NUMBER` (size ≤ 9) | `Integer` | `integer` |
| `NUMBER` (size ≤ 18) | `Long` | `long` |
| `NUMBER` (size > 18) | `java.math.BigDecimal` | `big_decimal` |
| `DATE`, `TIMESTAMP*` | `java.util.Date` | `timestamp` |

Any other type stops the generator with `IllegalArgumentException: Unmapped type`.

## How to run

Run all commands from the project root. The output path is relative to the current directory.

### Without Gradle or Maven (javac only)

Requires Java 8 or later.

macOS / Linux:

```bash
javac -encoding UTF-8 -cp freemarker-2.3.34.jar -d out src/main/java/com/mitsuru/codegen/CodegenApplication.java
java -cp out:src/main/resources:freemarker-2.3.34.jar com.mitsuru.codegen.CodegenApplication
```

Windows (use `;` instead of `:`):

```bat
javac -encoding UTF-8 -cp freemarker-2.3.34.jar -d out src\main\java\com\mitsuru\codegen\CodegenApplication.java
java -cp out;src\main\resources;freemarker-2.3.34.jar com.mitsuru.codegen.CodegenApplication
```

`src/main/resources` must be on the classpath because the templates are loaded from `/templates` on the classpath.

In an IDE, add `freemarker-2.3.34.jar` as a library (Eclipse: Build Path → Add External JARs; IntelliJ IDEA: Project Structure → Libraries) and make sure the `templates` folder is on the classpath.

### With Gradle

```bash
./gradlew bootRun
```

## Customizing templates

The templates can use these values:

| Name | Contents |
|---|---|
| `pkg` | Package name |
| `cls` | Class name (e.g. `ComMitsuruNa`) |
| `table` | Table name in upper case |
| `cols` | List of columns |

Each item in `cols` has:

| Name | Contents |
|---|---|
| `name` | Column name |
| `prop` | Property name (`camelCase`) |
| `comment` | Comment |
| `pk` | `true` if the column is a primary key |
| `nullable` | `true` if the column is nullable |
| `javaType` | Java type |
| `hibType` | Hibernate type |

## Limitations

- The column definitions are hard-coded. Reading them from the database is not implemented yet.
- `nullable` is not read from the database; every non-key column is treated as nullable.

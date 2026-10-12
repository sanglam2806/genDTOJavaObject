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
freemarker-2.3.34.jar                  FreeMarker library
lib/                                   JDBC driver jars (Oracle / PostgreSQL / MySQL)
codegen.properties                     Database connections and defaults
gen.sh, gen.bat                        Build and run without Gradle
src/main/java/com/mitsuru/codegen/
  CodegenApplication.java              Generator (main class, command-line options)
  DbHub.java                           Opens the connection for the selected database
  TableReader.java                     Reads columns from JDBC metadata, maps types
src/main/resources/templates/
  Dto.ftl, DtoImpl.ftl, Hbm.ftl        FreeMarker templates
```

## Configuration

Table definitions are read from the database: column names, comments, data types, primary key and `NOT NULL`. Nothing is hard-coded.

Connections are set in `codegen.properties` (in the project root). Each database has its own settings, so you switch databases by changing only `db`:

```properties
db=postgres                 # oracle | postgres | mysql

package=com.mitsuru.gen     # package of the generated code
out=src/main/java           # output root

oracle.url=jdbc:oracle:thin:@localhost:1521/XEPDB1
oracle.user=system
oracle.password=
oracle.schema=              # optional, default: login user's schema

postgres.url=jdbc:postgresql://localhost:5432/postgres
postgres.user=postgres
postgres.password=
postgres.schema=            # optional, default: current schema (public)

mysql.url=jdbc:mysql://localhost:3306/test   # database in the URL is used
mysql.user=root
mysql.password=
```

Table names are not case sensitive (`COM_MITSURU_NA` finds `com_mitsuru_na` in PostgreSQL). The class name comes from the table name: `com_mitsuru_na` → `ComMitsuruNa`.

### Command-line options

```
gen [--db=oracle|postgres|mysql] [--package=...] [--out=...] [--config=...] TABLE [TABLE...]
```

Options override `codegen.properties`. You can pass several tables at once.

### Type mapping

Types are mapped from the JDBC type, so they work the same way for all three databases.

| DB type | Java type | Hibernate type |
|---|---|---|
| `CHAR`, `VARCHAR`, `VARCHAR2`, `NVARCHAR*`, `TEXT` | `String` | `string` |
| `CLOB`, `NCLOB`, `LONGTEXT` | `String` | `text` |
| `NUMBER`/`NUMERIC`/`DECIMAL` without scale (precision ≤ 9), `INT`, `SMALLINT`, `TINYINT` | `Integer` | `integer` |
| `NUMBER`/`NUMERIC` (precision ≤ 18), `BIGINT` | `Long` | `long` |
| `NUMBER`/`NUMERIC` (precision > 18, with scale, or no precision) | `java.math.BigDecimal` | `big_decimal` |
| `BOOLEAN`, `BIT(1)`, MySQL `TINYINT(1)` | `Boolean` | `boolean` |
| `REAL`, `FLOAT`, `DOUBLE` | `Double` | `double` |
| `DATE`, `TIME`, `TIMESTAMP*`, `DATETIME` | `java.util.Date` | `timestamp` |
| `BLOB`, `BYTEA`, `RAW`, `VARBINARY` | `byte[]` | `binary` |

Any other type stops the generator with `IllegalArgumentException: Unmapped type`.

## How to run

Run all commands from the project root. The output path is relative to the current directory.

### JDBC drivers

The driver jar for your database must be in `lib/`. If you have Gradle, this copies all three:

```bash
./gradlew copyDrivers
```

Without Gradle, download them from Maven Central and put them in `lib/`: `com.oracle.database.jdbc:ojdbc11` (or `ojdbc8` for Java 8), `org.postgresql:postgresql`, `com.mysql:mysql-connector-j`.

### Without Gradle or Maven (script)

Requires Java 8 or later. The script compiles the generator into `out/` and runs it.

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

In an IDE, add `freemarker-2.3.34.jar` and the jars in `lib/` as libraries, make sure the `templates` folder is on the classpath, and set the table names as program arguments.

### With Gradle

```bash
./gradlew bootRun --args="COM_MITSURU_NA"
./gradlew bootRun --args="--db=oracle COM_MITSURU_NA ORDER_ITEM"
```

## Customizing templates

The templates can use these values:

| Name | Contents |
|---|---|
| `pkg` | Package name |
| `cls` | Class name (e.g. `ComMitsuruNa`) |
| `table` | Table name as stored in the database |
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

- A table with a composite primary key gets several `<id>` elements in the `.hbm.xml`, which Hibernate does not accept. Change it to `<composite-id>` by hand.

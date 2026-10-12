package com.mitsuru.codegen;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads a table definition (columns, comments, primary key, nullable) from JDBC metadata.
 * Uses only standard JDBC, so the same code works for Oracle, PostgreSQL and MySQL.
 */
public class TableReader {
	private final Connection conn;
	private final DatabaseMetaData md;
	private final String catalog;
	private final String schema;

	public TableReader(Connection conn, String schema) throws SQLException {
		this.conn = conn;
		this.md = conn.getMetaData();
		// MySQL uses the catalog for the database, Oracle/PostgreSQL use the schema
		this.catalog = conn.getCatalog();
		this.schema = schema != null ? schema : currentSchema();
	}

	/** The table name as stored in the database (Oracle: upper case, PostgreSQL: lower case). */
	public String resolveTableName(String table) throws SQLException {
		Set<String> candidates = new LinkedHashSet<>();
		candidates.add(table);
		candidates.add(table.toUpperCase());
		candidates.add(table.toLowerCase());
		for (String t : candidates) {
			try (ResultSet rs = md.getTables(catalog, schema, t, new String[] { "TABLE", "VIEW" })) {
				if (rs.next()) return rs.getString("TABLE_NAME");
			}
		}
		throw new IllegalArgumentException("Table not found: " + table + " (schema: " + schema + ")");
	}

	public List<Map<String, Object>> getColumns(String table) throws SQLException {
		Set<String> key = new HashSet<>();
		try (ResultSet rs = md.getPrimaryKeys(catalog, schema, table)) {
			while (rs.next()) key.add(rs.getString("COLUMN_NAME"));
		}

		List<Map<String, Object>> cols = new ArrayList<>();
		try (ResultSet rs = md.getColumns(catalog, schema, table, "%")) {
			while (rs.next()) {
				String colName = rs.getString("COLUMN_NAME");
				String remarks = rs.getString("REMARKS");

				Map<String, Object> col = new HashMap<>();
				col.put("name", colName);
				col.put("comment", remarks == null || remarks.isEmpty() ? colName : remarks);
				col.put("prop", CodegenApplication.camel(colName));
				boolean pk = key.contains(colName);
				col.put("pk", pk);
				col.put("nullable", !pk && rs.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls);
				String[] t = mapType(rs.getInt("DATA_TYPE"), rs.getString("TYPE_NAME"),
						rs.getInt("COLUMN_SIZE"), rs.getInt("DECIMAL_DIGITS"));
				col.put("javaType", t[0]);
				col.put("hibType", t[1]);
				cols.add(col);
			}
		}
		if (cols.isEmpty()) throw new IllegalArgumentException("No columns found for table: " + table);
		return cols;
	}

	private String currentSchema() throws SQLException {
		try {
			return conn.getSchema();
		} catch (AbstractMethodError | SQLException e) {
			return null;
		}
	}

	/** Maps a JDBC type to {Java type, Hibernate type}. */
	static String[] mapType(int sqlType, String typeName, int size, int scale) {
		String name = typeName == null ? "" : typeName.toUpperCase();
		switch (sqlType) {
		case Types.CHAR:
		case Types.VARCHAR:
		case Types.NCHAR:
		case Types.NVARCHAR:
			return new String[] { "String", "string" };
		case Types.LONGVARCHAR:
		case Types.LONGNVARCHAR:
		case Types.CLOB:
		case Types.NCLOB:
			return new String[] { "String", "text" };
		case Types.BIT:
		case Types.BOOLEAN:
			if (size > 1) return new String[] { "byte[]", "binary" };
			return new String[] { "Boolean", "boolean" };
		case Types.TINYINT:
		case Types.SMALLINT:
		case Types.INTEGER:
			return name.contains("UNSIGNED") && sqlType == Types.INTEGER
					? new String[] { "Long", "long" }
					: new String[] { "Integer", "integer" };
		case Types.BIGINT:
			return name.contains("UNSIGNED")
					? new String[] { "java.math.BigDecimal", "big_decimal" }
					: new String[] { "Long", "long" };
		case Types.NUMERIC:
		case Types.DECIMAL:
			// Oracle NUMBER without precision: size 0, scale -127
			if (scale != 0 || size <= 0) return new String[] { "java.math.BigDecimal", "big_decimal" };
			if (size <= 9) return new String[] { "Integer", "integer" };
			if (size <= 18) return new String[] { "Long", "long" };
			return new String[] { "java.math.BigDecimal", "big_decimal" };
		case Types.REAL:
		case Types.FLOAT:
		case Types.DOUBLE:
			return new String[] { "Double", "double" };
		case Types.DATE:
		case Types.TIME:
		case Types.TIMESTAMP:
		case Types.TIME_WITH_TIMEZONE:
		case Types.TIMESTAMP_WITH_TIMEZONE:
			return new String[] { "java.util.Date", "timestamp" };
		case Types.BINARY:
		case Types.VARBINARY:
		case Types.LONGVARBINARY:
		case Types.BLOB:
			return new String[] { "byte[]", "binary" };
		default:
			// vendor types, e.g. Oracle TIMESTAMP WITH (LOCAL) TIME ZONE
			if (name.startsWith("TIMESTAMP") || name.equals("DATE")) return new String[] { "java.util.Date", "timestamp" };
			throw new IllegalArgumentException("Unmapped type: " + typeName + " (java.sql.Types " + sqlType + ")");
		}
	}
}

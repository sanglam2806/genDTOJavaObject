package com.mitsuru.codegen;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Opens a JDBC connection to the database selected by {@code db} in codegen.properties.
 * Switching databases only needs a different {@code db} value (or {@code --db=...}).
 *
 * Settings for each database use the database name as the prefix:
 * {@code <db>.url}, {@code <db>.user}, {@code <db>.password}, {@code <db>.schema} (optional).
 */
public class DbHub {
	public enum DbType { ORACLE, POSTGRES, MYSQL }

	private final DbType type;
	private final String url;
	private final String user;
	private final String password;
	private final String schema;

	public DbHub(Properties conf, String db) {
		this.type = parseType(db);
		String prefix = type.name().toLowerCase();
		this.url = required(conf, prefix + ".url");
		this.user = conf.getProperty(prefix + ".user", "");
		this.password = conf.getProperty(prefix + ".password", "");
		String s = conf.getProperty(prefix + ".schema", "").trim();
		this.schema = s.isEmpty() ? null : s;
	}

	public DbType getType() {
		return type;
	}

	/** Schema from the config, or null to use the connection's current schema. */
	public String getSchema() {
		return schema;
	}

	public Connection connect() throws SQLException {
		Properties p = new Properties();
		p.setProperty("user", user);
		p.setProperty("password", password);
		switch (type) {
		case ORACLE:
			// Oracle returns column comments (REMARKS) only with this option
			p.setProperty("remarksReporting", "true");
			break;
		case MYSQL:
			p.setProperty("useInformationSchema", "true");
			break;
		default:
			break;
		}
		try {
			return DriverManager.getConnection(url, p);
		} catch (SQLException e) {
			if (String.valueOf(e.getMessage()).contains("No suitable driver")) {
				throw new SQLException("Cannot connect to " + url + ". Is the " + type.name().toLowerCase()
						+ " JDBC driver jar in lib/? (" + e.getMessage() + ")", e);
			}
			throw e;
		}
	}

	private static DbType parseType(String db) {
		String d = db == null ? "" : db.trim().toLowerCase();
		switch (d) {
		case "oracle":
			return DbType.ORACLE;
		case "postgres":
		case "postgresql":
			return DbType.POSTGRES;
		case "mysql":
			return DbType.MYSQL;
		default:
			throw new IllegalArgumentException("Unknown db: '" + db + "' (use oracle, postgres or mysql)");
		}
	}

	private static String required(Properties conf, String key) {
		String v = conf.getProperty(key, "").trim();
		if (v.isEmpty()) throw new IllegalArgumentException("Missing setting in codegen.properties: " + key);
		return v;
	}
}

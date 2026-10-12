package com.mitsuru.codegen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.util.Properties;

import org.junit.jupiter.api.Test;

/** Choosing the DB and reading its settings from codegen.properties. */
class DbHubTest {

	private static Properties conf() {
		Properties p = new Properties();
		p.setProperty("oracle.url", "jdbc:oracle:thin:@ora:1521/XE");
		p.setProperty("oracle.schema", "APP");
		p.setProperty("postgres.url", "jdbc:h2:mem:hubtest");
		p.setProperty("postgres.user", "sa");
		p.setProperty("mysql.url", "jdbc:mysql://my:3306/test");
		return p;
	}

	@Test
	void selectsTypeByName() {
		assertThat(new DbHub(conf(), "oracle").getType()).isEqualTo(DbHub.DbType.ORACLE);
		assertThat(new DbHub(conf(), "POSTGRES").getType()).isEqualTo(DbHub.DbType.POSTGRES);
		assertThat(new DbHub(conf(), "postgresql").getType()).isEqualTo(DbHub.DbType.POSTGRES);
		assertThat(new DbHub(conf(), " mysql ").getType()).isEqualTo(DbHub.DbType.MYSQL);
	}

	@Test
	void unknownTypeFails() {
		assertThatThrownBy(() -> new DbHub(conf(), "sqlite"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Unknown db: 'sqlite'");
	}

	@Test
	void missingUrlFails() {
		Properties p = conf();
		p.remove("mysql.url");
		assertThatThrownBy(() -> new DbHub(p, "mysql"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("mysql.url");
	}

	@Test
	void schemaIsReadPerDb() {
		assertThat(new DbHub(conf(), "oracle").getSchema()).isEqualTo("APP");
		// empty / missing -> null, so the connection's current schema is used
		assertThat(new DbHub(conf(), "postgres").getSchema()).isNull();
	}

	@Test
	void connectsWithTheSelectedDbSettings() throws Exception {
		// postgres.url points to an H2 in-memory DB, so connect() really opens a connection
		try (Connection c = new DbHub(conf(), "postgres").connect()) {
			assertThat(c.isValid(1)).isTrue();
			assertThat(c.getMetaData().getURL()).isEqualTo("jdbc:h2:mem:hubtest");
		}
	}

	@Test
	void missingDriverGivesHint() {
		Properties p = conf();
		p.setProperty("oracle.url", "jdbc:nodriver://x");
		assertThatThrownBy(() -> new DbHub(p, "oracle").connect())
				.hasMessageContaining("JDBC driver jar in lib/");
	}
}

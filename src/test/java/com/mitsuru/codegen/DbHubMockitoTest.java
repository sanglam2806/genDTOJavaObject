package com.mitsuru.codegen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * DbHub.connect() with DriverManager mocked by Mockito: no driver and no database is used.
 * Checks which URL and connection properties DbHub passes for each DB type.
 */
class DbHubMockitoTest {

	private MockedStatic<DriverManager> driverManager;
	private final Connection fakeConnection = mock(Connection.class);

	@BeforeEach
	void setUp() {
		// while open, every static call to DriverManager goes to this mock
		driverManager = mockStatic(DriverManager.class);
	}

	@AfterEach
	void tearDown() {
		driverManager.close();
	}

	private static Properties conf() {
		Properties p = new Properties();
		p.setProperty("oracle.url", "jdbc:oracle:thin:@ora:1521/XE");
		p.setProperty("oracle.user", "scott");
		p.setProperty("oracle.password", "tiger");
		p.setProperty("postgres.url", "jdbc:postgresql://pg:5432/app");
		p.setProperty("postgres.user", "pguser");
		p.setProperty("mysql.url", "jdbc:mysql://my:3306/test");
		p.setProperty("mysql.user", "root");
		return p;
	}

	/** Runs connect() and returns the Properties that DbHub passed to DriverManager. */
	private Properties connectAndCapture(String db, String expectedUrl) throws SQLException {
		ArgumentCaptor<Properties> props = ArgumentCaptor.forClass(Properties.class);
		driverManager.when(() -> DriverManager.getConnection(eq(expectedUrl), any(Properties.class)))
				.thenReturn(fakeConnection);

		Connection c = new DbHub(conf(), db).connect();

		assertThat(c).isSameAs(fakeConnection);
		driverManager.verify(() -> DriverManager.getConnection(eq(expectedUrl), props.capture()));
		return props.getValue();
	}

	@Test
	void oracleUsesOracleSettingsAndRemarksReporting() throws Exception {
		Properties p = connectAndCapture("oracle", "jdbc:oracle:thin:@ora:1521/XE");

		assertThat(p.getProperty("user")).isEqualTo("scott");
		assertThat(p.getProperty("password")).isEqualTo("tiger");
		assertThat(p.getProperty("remarksReporting")).isEqualTo("true");
		assertThat(p.getProperty("useInformationSchema")).isNull();
	}

	@Test
	void postgresUsesPostgresSettingsWithoutExtraOptions() throws Exception {
		Properties p = connectAndCapture("postgres", "jdbc:postgresql://pg:5432/app");

		assertThat(p.getProperty("user")).isEqualTo("pguser");
		assertThat(p.getProperty("password")).isEmpty(); // not set -> ""
		assertThat(p.stringPropertyNames()).containsExactlyInAnyOrder("user", "password");
	}

	@Test
	void mysqlUsesMysqlSettingsAndInformationSchema() throws Exception {
		Properties p = connectAndCapture("mysql", "jdbc:mysql://my:3306/test");

		assertThat(p.getProperty("user")).isEqualTo("root");
		assertThat(p.getProperty("useInformationSchema")).isEqualTo("true");
		assertThat(p.getProperty("remarksReporting")).isNull();
	}

	@Test
	void connectsOnlyOnce() throws Exception {
		connectAndCapture("postgres", "jdbc:postgresql://pg:5432/app");

		driverManager.verify(() -> DriverManager.getConnection(anyString(), any(Properties.class)));
		driverManager.verifyNoMoreInteractions();
	}

	@Test
	void noSuitableDriverGivesLibHint() {
		// create the exception before stubbing: SQLException's constructor itself calls DriverManager
		SQLException noDriver = new SQLException("No suitable driver found for jdbc:oracle:thin:@ora:1521/XE", "08001");
		driverManager.when(() -> DriverManager.getConnection(anyString(), any(Properties.class)))
				.thenThrow(noDriver);

		assertThatThrownBy(() -> new DbHub(conf(), "oracle").connect())
				.isInstanceOf(SQLException.class)
				.hasMessageContaining("oracle JDBC driver jar in lib/")
				.hasCauseInstanceOf(SQLException.class);
	}

	@Test
	void otherErrorsArePassedThroughUnchanged() {
		SQLException refused = new SQLException("Connection refused", "08001");
		driverManager.when(() -> DriverManager.getConnection(anyString(), any(Properties.class)))
				.thenThrow(refused);

		assertThatThrownBy(() -> new DbHub(conf(), "postgres").connect()).isSameAs(refused);
	}
}

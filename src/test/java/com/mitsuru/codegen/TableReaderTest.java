package com.mitsuru.codegen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Reads table definitions from an H2 in-memory DB running in Oracle / PostgreSQL / MySQL mode.
 * H2 imitates each DB's name case (Oracle: upper, PostgreSQL: lower), so the lookup logic is checked
 * for each one without installing the real databases.
 */
class TableReaderTest {

	private static Connection open(String mode) throws Exception {
		String url = "jdbc:h2:mem:" + mode + System.nanoTime() + ";MODE=" + mode;
		if (mode.equals("PostgreSQL")) url += ";DATABASE_TO_LOWER=TRUE";
		Connection c = DriverManager.getConnection(url, "sa", "");
		try (Statement s = c.createStatement()) {
			s.execute("create table com_mitsuru_na ("
					+ " id numeric(10) primary key,"
					+ " user_name varchar(50) not null,"
					+ " phone_number varchar(20),"
					+ " price numeric(10,2),"
					+ " active boolean,"
					+ " creation_time timestamp)");
			s.execute("comment on column com_mitsuru_na.id is 'ユーザID'");
			s.execute("comment on column com_mitsuru_na.user_name is 'ユーザ'");
			s.execute("create table order_item (order_id bigint, line_no int, qty smallint,"
					+ " primary key (order_id, line_no))");
		}
		return c;
	}

	private static Map<String, Object> col(List<Map<String, Object>> cols, String prop) {
		return cols.stream().filter(c -> prop.equals(c.get("prop"))).findFirst()
				.orElseThrow(() -> new AssertionError("no column " + prop));
	}

	@ParameterizedTest
	@ValueSource(strings = { "Oracle", "PostgreSQL", "MySQL" })
	void findsTableInAnyCase(String mode) throws Exception {
		try (Connection c = open(mode)) {
			TableReader r = new TableReader(c, null);
			String stored = r.resolveTableName("com_mitsuru_na");
			assertThat(r.resolveTableName("COM_MITSURU_NA")).isEqualTo(stored);
			assertThat(r.resolveTableName("Com_Mitsuru_Na")).isEqualTo(stored);
			if (mode.equals("PostgreSQL")) assertThat(stored).isEqualTo("com_mitsuru_na");
			if (mode.equals("Oracle")) assertThat(stored).isEqualTo("COM_MITSURU_NA");
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "Oracle", "PostgreSQL", "MySQL" })
	void readsColumns(String mode) throws Exception {
		try (Connection c = open(mode)) {
			TableReader r = new TableReader(c, null);
			List<Map<String, Object>> cols = r.getColumns(r.resolveTableName("com_mitsuru_na"));

			// same order as the table definition
			assertThat(cols).extracting(m -> m.get("prop"))
					.containsExactly("id", "userName", "phoneNumber", "price", "active", "creationTime");

			Map<String, Object> id = col(cols, "id");
			assertThat(id.get("pk")).isEqualTo(true);
			assertThat(id.get("nullable")).isEqualTo(false);
			assertThat(id.get("javaType")).isEqualTo("Long");
			assertThat(id.get("comment")).isEqualTo("ユーザID");

			Map<String, Object> user = col(cols, "userName");
			assertThat(user.get("pk")).isEqualTo(false);
			assertThat(user.get("nullable")).isEqualTo(false); // NOT NULL
			assertThat(user.get("comment")).isEqualTo("ユーザ");

			Map<String, Object> phone = col(cols, "phoneNumber");
			assertThat(phone.get("nullable")).isEqualTo(true);
			// no comment -> column name
			assertThat(((String) phone.get("comment"))).isEqualToIgnoringCase("phone_number");

			assertThat(col(cols, "price").get("javaType")).isEqualTo("java.math.BigDecimal");
			assertThat(col(cols, "active").get("javaType")).isEqualTo("Boolean");
			assertThat(col(cols, "creationTime").get("javaType")).isEqualTo("java.util.Date");
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "Oracle", "PostgreSQL", "MySQL" })
	void readsCompositePrimaryKey(String mode) throws Exception {
		try (Connection c = open(mode)) {
			TableReader r = new TableReader(c, null);
			List<Map<String, Object>> cols = r.getColumns(r.resolveTableName("order_item"));
			assertThat(col(cols, "orderId").get("pk")).isEqualTo(true);
			assertThat(col(cols, "lineNo").get("pk")).isEqualTo(true);
			assertThat(col(cols, "qty").get("pk")).isEqualTo(false);
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "Oracle", "PostgreSQL", "MySQL" })
	void unknownTableFails(String mode) throws Exception {
		try (Connection c = open(mode)) {
			TableReader r = new TableReader(c, null);
			assertThatThrownBy(() -> r.resolveTableName("no_such_table"))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("Table not found: no_such_table");
		}
	}
}

package com.mitsuru.codegen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Types;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Pure logic, no database: JDBC type -> Java/Hibernate type, and snake_case -> camelCase. */
class TypeMappingTest {

	@ParameterizedTest(name = "{1}({2},{3}) -> {4} / {5}")
	@CsvSource({
		// sqlType,                 typeName,     size, scale, javaType,               hibType
		Types.VARCHAR + ",          VARCHAR2,     50,   0,     String,                 string",
		Types.NVARCHAR + ",         NVARCHAR2,    50,   0,     String,                 string",
		Types.CHAR + ",             CHAR,         1,    0,     String,                 string",
		Types.CLOB + ",             CLOB,         0,    0,     String,                 text",
		Types.NUMERIC + ",          NUMBER,       9,    0,     Integer,                integer",
		Types.NUMERIC + ",          NUMBER,       10,   0,     Long,                   long",
		Types.NUMERIC + ",          NUMBER,       18,   0,     Long,                   long",
		Types.NUMERIC + ",          NUMBER,       19,   0,     java.math.BigDecimal,   big_decimal",
		Types.NUMERIC + ",          NUMBER,       10,   2,     java.math.BigDecimal,   big_decimal",
		// Oracle NUMBER without precision
		Types.NUMERIC + ",          NUMBER,       0,    -127,  java.math.BigDecimal,   big_decimal",
		Types.INTEGER + ",          INT,          10,   0,     Integer,                integer",
		Types.INTEGER + ",          INT UNSIGNED, 10,   0,     Long,                   long",
		Types.BIGINT + ",           BIGINT,       19,   0,     Long,                   long",
		Types.SMALLINT + ",         SMALLINT,     5,    0,     Integer,                integer",
		Types.BIT + ",              bool,         1,    0,     Boolean,                boolean",
		Types.BOOLEAN + ",          BOOLEAN,      1,    0,     Boolean,                boolean",
		Types.DOUBLE + ",           DOUBLE,       0,    0,     Double,                 double",
		Types.DATE + ",             DATE,         0,    0,     java.util.Date,         timestamp",
		Types.TIMESTAMP + ",        TIMESTAMP(6), 0,    0,     java.util.Date,         timestamp",
		Types.BLOB + ",             BLOB,         0,    0,     byte[],                 binary",
		// Oracle vendor type code for TIMESTAMP WITH TIME ZONE
		"-101,                      TIMESTAMP WITH TIME ZONE, 0, 0, java.util.Date,    timestamp",
	})
	void mapType(int sqlType, String typeName, int size, int scale, String javaType, String hibType) {
		assertThat(TableReader.mapType(sqlType, typeName, size, scale)).containsExactly(javaType, hibType);
	}

	@Test
	void unknownTypeFails() {
		assertThatThrownBy(() -> TableReader.mapType(Types.ARRAY, "_int4", 0, 0))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Unmapped type: _int4");
	}

	@ParameterizedTest
	@CsvSource({
		"id,              id",
		"phone_number,    phoneNumber",
		"CREATION_TIME,   creationTime",
		"com_mitsuru_na,  comMitsuruNa",
	})
	void camel(String in, String expected) {
		assertThat(CodegenApplication.camel(in)).isEqualTo(expected);
	}
}

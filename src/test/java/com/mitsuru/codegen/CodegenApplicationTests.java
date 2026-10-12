package com.mitsuru.codegen;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** End to end: codegen.properties + command-line args -> generated files. */
class CodegenApplicationTests {

	@TempDir
	Path tmp;

	private Connection keepAlive;
	private Path config;
	private Path out;

	@BeforeEach
	void setUp() throws Exception {
		String url = "jdbc:h2:mem:e2e" + System.nanoTime() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE";
		// keep one connection open so the in-memory DB lives while main() runs
		keepAlive = DriverManager.getConnection(url, "sa", "");
		try (Statement s = keepAlive.createStatement()) {
			s.execute("create table com_mitsuru_na (id numeric(10) primary key, user_name varchar(50) not null,"
					+ " creation_time timestamp)");
			s.execute("comment on column com_mitsuru_na.id is 'ユーザID'");
		}

		out = tmp.resolve("out");
		config = tmp.resolve("codegen.properties");
		// db=oracle on purpose: --db=postgres on the command line must win
		Files.write(config, ("db=oracle\n"
				+ "oracle.url=jdbc:oracle:thin:@nowhere:1521/XE\n"
				+ "postgres.url=" + url + "\n"
				+ "postgres.user=sa\n"
				+ "package=com.example\n"
				+ "out=" + out.toString().replace('\\', '/') + "\n").getBytes(StandardCharsets.UTF_8));
	}

	@AfterEach
	void tearDown() throws Exception {
		keepAlive.close();
	}

	private void run(String... tables) throws Exception {
		String[] args = new String[tables.length + 2];
		args[0] = "--config=" + config;
		args[1] = "--db=postgres";
		System.arraycopy(tables, 0, args, 2, tables.length);
		CodegenApplication.main(args);
	}

	@Test
	void generatesFilesForTable() throws Exception {
		run("COM_MITSURU_NA");

		Path dir = out.resolve("com/example/dto");
		String dto = read(dir.resolve("ComMitsuruNaDto.java"));
		String impl = read(dir.resolve("ComMitsuruNaDtoImpl.java"));
		String hbm = read(dir.resolve("ComMitsuruNa.hbm.xml"));

		assertThat(dto).contains("package com.example.dto;")
				.contains("public interface ComMitsuruNaDto")
				.contains("Long getId();")
				.contains("String getUserName();")
				.contains("java.util.Date getCreationTime();")
				.contains("ユーザID");
		assertThat(impl).contains("public class ComMitsuruNaDtoImpl implements ComMitsuruNaDto")
				.contains("private String userName;");
		assertThat(hbm).contains("table=\"com_mitsuru_na\"")
				.contains("<id name=\"id\" column=\"id\" type=\"long\"/>")
				.contains("<property name=\"userName\" column=\"user_name\" type=\"string\" not-null=\"true\"/>")
				.contains("<property name=\"creationTime\" column=\"creation_time\" type=\"timestamp\"/>");
	}

	@Test
	void existingFilesAreNotOverwritten() throws Exception {
		run("com_mitsuru_na");
		Path dto = out.resolve("com/example/dto/ComMitsuruNaDto.java");
		Files.write(dto, "// edited by hand".getBytes(StandardCharsets.UTF_8));

		run("com_mitsuru_na");

		assertThat(read(dto)).isEqualTo("// edited by hand");
	}

	private static String read(Path p) throws Exception {
		return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
	}
}

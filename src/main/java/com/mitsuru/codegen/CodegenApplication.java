package com.mitsuru.codegen;

import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import freemarker.template.Configuration;

/**
 * Usage: CodegenApplication [--db=oracle|postgres|mysql] [--package=...] [--out=...] [--config=...] TABLE...
 * Options override the values in codegen.properties.
 */
public class CodegenApplication {
	private static final Charset CS = Charset.forName("UTF-8");
	private static final String ROOT_PACKAGE = "com.mitsuru.gen";
	private static final String OUTPUT_DIR = "src/main/java";
	private static final String CONFIG_FILE = "codegen.properties";

	public static void main(String[] args) throws Exception {
		Map<String, String> opts = new HashMap<>();
		List<String> tables = new ArrayList<>();
		for (String a : args) {
			if (a.startsWith("--")) {
				int eq = a.indexOf('=');
				if (eq < 0) throw new IllegalArgumentException("Option must be --name=value: " + a);
				opts.put(a.substring(2, eq), a.substring(eq + 1));
			} else {
				tables.add(a);
			}
		}
		if (tables.isEmpty()) {
			System.err.println("Usage: gen [--db=oracle|postgres|mysql] [--package=com.example] [--out=src/main/java] [--config=codegen.properties] TABLE...");
			System.exit(1);
		}

		Properties conf = loadConfig(opts.containsKey("config") ? opts.get("config") : CONFIG_FILE);
		String db = opt(opts, conf, "db", "oracle");
		String pkg = opt(opts, conf, "package", ROOT_PACKAGE);
		String outDir = opt(opts, conf, "out", OUTPUT_DIR) + "/" + pkg.replace('.', '/');

		Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
		cfg.setClassForTemplateLoading(CodegenApplication.class, "/templates");
		cfg.setDefaultEncoding("UTF-8");

		DbHub hub = new DbHub(conf, db);
		System.out.println("DB: " + hub.getType().name().toLowerCase());
		try (Connection conn = hub.connect()) {
			TableReader reader = new TableReader(conn, hub.getSchema());
			for (String t : tables) {
				genCode(cfg, reader, t, pkg, outDir);
			}
		}
	}

	private static void genCode(Configuration cfg, TableReader reader, String tableArg, String pkg, String outDir) throws Exception {
		String table = reader.resolveTableName(tableArg);
		String className = capFirst(camel(table));
		List<Map<String, Object>> cols = reader.getColumns(table);

		Map<String, Object> m = new HashMap<>();
		m.put("pkg", pkg);
		m.put("cls", className);
		m.put("table", table);
		m.put("cols", cols);

		write(cfg, "Dto.ftl",     outDir + "/dto/" + className + "Dto.java", m);
		write(cfg, "DtoImpl.ftl", outDir + "/dto/" + className + "DtoImpl.java", m);
		write(cfg, "Hbm.ftl",     outDir + "/dto/" + className + ".hbm.xml", m);
	}

	private static Properties loadConfig(String path) throws Exception {
		Properties p = new Properties();
		Path f = Paths.get(path);
		if (!Files.exists(f)) throw new IllegalArgumentException("Config file not found: " + f.toAbsolutePath());
		try (InputStream in = Files.newInputStream(f)) {
			p.load(new java.io.InputStreamReader(in, CS));
		}
		return p;
	}

	private static String opt(Map<String, String> opts, Properties conf, String key, String def) {
		if (opts.containsKey(key)) return opts.get(key);
		String v = conf.getProperty(key, "").trim();
		return v.isEmpty() ? def : v;
	}

	static String render(Configuration cfg, String tpl, Map<String, Object> m) throws Exception {
		StringWriter w = new StringWriter();
		cfg.getTemplate(tpl).process(m, w);
		return w.toString();
	}

	static void write(Configuration cfg, String tpl, String path, Map<String, Object> m) throws Exception {
		Path p = Paths.get(path);
		if (Files.exists(p)) { System.out.println("SKIP (exists): " + p); return; }
		Files.createDirectories(p.getParent());
		Files.write(p, render(cfg, tpl, m).getBytes(CS));
		System.out.println("CREATED: " + p);
	}

	static String camel(String s) {
		StringBuilder sb = new StringBuilder();
		boolean up = false;
		for (char c : s.toLowerCase().toCharArray()) {
			if (c == '_') {
				up = true;
				continue;
			}
			sb.append(up ? Character.toUpperCase(c) : c);
			up = false;
		}
		return sb.toString();
	}

	private static String capFirst(String s) {
		return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}
}

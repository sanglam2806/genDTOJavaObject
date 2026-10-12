package com.mitsuru.codegen;

import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import freemarker.template.Configuration;

public class CodegenApplication {
	private static final Charset CS = Charset.forName("UTF-8");

	private static final String TABLE_NAME = "com_mitsuru_na"; 
	private static final String PACKAGE = "com.mitsuru.gen";
	private static final List<String> COLUMNS = Arrays.asList("id", "user", "password", "phone_number", "creation_time");
	private static final List<String> TYPES = Arrays.asList("number", "nvarchar", "nvarchar", "nvarchar", "timestamp");
	private static final List<String> COMMENTS = Arrays.asList("ユーザID", "ユーザ", "パスワード", "携帯番号", "作成日時");
	private static final List<Integer> SIZES = Arrays.asList(10, 50, 255, 20, 0);

	public static void main(String[] args) throws Exception{
		print();
		genCode();
	}

	private static void print() {
		System.out.println("Hello Na-chan");
	}

	private static void genCode() throws Exception{
		// setting output
		// create files
		// get objects 
		// mapping to template params
		// insert file
		
		String table = TABLE_NAME.toUpperCase();
		String className = capFirst(camel(table));
		String outDir = "src/main/java/" + PACKAGE.replace('.', '/');

		List<Map<String, Object>> cols = getColumns(table);

		Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
		cfg.setClassForTemplateLoading(CodegenApplication.class, "/templates");
		cfg.setDefaultEncoding("UTF-8");

		Map<String, Object> m = new HashMap<>();
		m.put("pkg", PACKAGE);
		m.put("cls", className);
		m.put("table", table);
		m.put("cols", cols);

		// create new file
		write(cfg, "Dto.ftl",     outDir + "/dto/" + className + "Dto.java", m);
        write(cfg, "DtoImpl.ftl", outDir + "/dto/" + className + "DtoImpl.java", m);
        write(cfg, "Hbm.ftl",     outDir + "/dto/" + className + ".hbm.xml", m);

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

	private static List<Map<String, Object>> getColumns(String table) {
		List<Map<String, Object>> cols = new ArrayList<>();
		// will get from DB
		Set<String> key = new HashSet<>();
		key.add("id");

		for(int i = 0; i < COLUMNS.size(); i++) {
			String colName = COLUMNS.get(i);
			String type = TYPES.get(i).toUpperCase();
			String comment = COMMENTS.get(i);

			Map<String, Object> col = new HashMap<>();
			col.put("name", colName);
			col.put("comment", comment);
			col.put("prop", camel(colName));
			boolean pk = key.contains(colName);
			col.put("pk", pk);
			col.put("nullable", !pk);
			String[] t = mapType(type, SIZES.get(i));
			col.put("javaType", t[0]);
			col.put("hibType", t[1]);
			cols.add(col);
		}

		return cols;
	}

	private static String camel(String s) {
		StringBuilder sb = new StringBuilder();
		boolean up = false;
		for (char c : s.toLowerCase().toCharArray()) {
			if(c == '_') {
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

	private static String[] mapType(String t, int size){
		if (t.startsWith("VARCHAR") || t.startsWith("NVARCHAR") || t.equals("CHAR")) return new String[]{"String", "string"};
        if (t.equals("NUMBER")) {
            if (size <= 9) return new String[]{"Integer", "integer"};
            if (size <= 18) return new String[]{"Long", "long"};
            return new String[]{"java.math.BigDecimal", "big_decimal"};
        }
        if (t.equals("DATE") || t.startsWith("TIMESTAMP"))
            return new String[]{"java.util.Date", "timestamp"};
        throw new IllegalArgumentException("Unmapped type: " + t);  // fail loudly
	}

}

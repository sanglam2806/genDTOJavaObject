package com.mitsuru.codegen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CodegenApplication {

	public static void main(String[] args) {
		SpringApplication.run(CodegenApplication.class, args);
		print();
	}

	private static void print() {
		System.out.println("Hello Na-chan");
	}

}

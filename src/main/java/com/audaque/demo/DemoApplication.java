package com.audaque.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		// 必须在 AWT 加载前关闭 headless 模式，否则剪贴板监听会抛 HeadlessException
		System.setProperty("java.awt.headless", "false");
		SpringApplication.run(DemoApplication.class, args);
	}

}

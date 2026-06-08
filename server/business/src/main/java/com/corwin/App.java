package com.corwin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

/**
 * @author Corwin 2026/5/7
 */
@Slf4j
@SpringBootApplication(scanBasePackages = "com.corwin")
public class App {

    public static void main(String[] args) {
        System.setProperty("user.timezone", "UTC");
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        log.info("System default timezone set to {}", TimeZone.getDefault().getID());
        SpringApplication.run(App.class, args);
    }

}

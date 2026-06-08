package com.corwin;

import com.corwin.bootstrap.application.service.BootstrapDatabasePreflightInitializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.*;

/**
 * @author Corwin 2026/1/6
 */
@Slf4j
@SpringBootApplication(scanBasePackages = "com.corwin.bootstrap")
public class Bootstrap {

    public static void main(String[] args) {
        System.setProperty("user.timezone", "UTC");
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        log.info("System default timezone set to {}", TimeZone.getDefault().getID());

        List<String> autoconfigureExclude = new ArrayList<>();
        autoconfigureExclude.add("org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration");
        autoconfigureExclude.add("org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration");

        Map<String, Object> properties = new HashMap<>();
        properties.put("server.port", "0");
        properties.put("spring.main.banner-mode", "off");
        properties.put("spring.main.web-application-type", "none");
        properties.put("spring.autoconfigure.exclude", String.join(",", autoconfigureExclude));
        properties.put("spring.datasource.hikari.auto-commit", "false");
        properties.put("bootstrap.initialization.process", "true");

        SpringApplication application = new SpringApplication(Bootstrap.class);
        application.addInitializers(new BootstrapDatabasePreflightInitializer());
        application.setDefaultProperties(properties);
        application.run(args);
    }

}

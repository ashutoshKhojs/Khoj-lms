package com.khoj.lms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@Slf4j
public class LmsApplication {

	public static void main(String[] args) {
		try {
			ConfigurableApplicationContext context =
					SpringApplication.run(LmsApplication.class, args);

			Environment env = context.getEnvironment();

			String profile = env.getActiveProfiles().length > 0
					? env.getActiveProfiles()[0]
					: "default";

			String port = env.getProperty("server.port", "8080");

			log.info("═══════════════════════════════════════════════════");
			log.info("  Khoj LMS — Started Successfully");
			log.info("  Profile  : {}", profile);
			log.info("  Port     : {}", port);
			log.info("  Swagger  : http://localhost:{}/swagger-ui.html", port);
			log.info("  Health   : http://localhost:{}/actuator/health", port);
			log.info("═══════════════════════════════════════════════════");

		} catch (Exception e) {
			// Logs to main + error file both
			log.error("═══════════════════════════════════════════════════");
			log.error("  Khoj LMS — FAILED TO START");
			log.error("  Reason : {}", e.getMessage(), e);
			log.error("═══════════════════════════════════════════════════");
			System.exit(1);
		}
	}
}
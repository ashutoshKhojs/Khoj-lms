package com.khoj.lms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@Configuration
public class EmailConfig {

    @Bean("emailTemplateEngine")
    public SpringTemplateEngine emailTemplateEngine() {

        ClassLoaderTemplateResolver resolver =
                new ClassLoaderTemplateResolver();

        resolver.setPrefix("templates/email/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        resolver.setOrder(1);

        SpringTemplateEngine engine =
                new SpringTemplateEngine();

        engine.addTemplateResolver(resolver);

        System.out.println("=== EMAIL TEMPLATE ENGINE LOADED ===");

        return engine;
    }
}
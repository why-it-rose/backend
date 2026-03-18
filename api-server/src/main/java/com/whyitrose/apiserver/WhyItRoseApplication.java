package com.whyitrose.apiserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@EntityScan(basePackages = "com.whyitrose")
@EnableJpaRepositories(basePackages = "com.whyitrose")
public class WhyItRoseApplication {

    public static void main(String[] args) {
        SpringApplication.run(WhyItRoseApplication.class, args);
    }
}

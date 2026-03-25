package com.whyitrose.batch;

import org.springframework.boot.SpringApplication;
import com.whyitrose.batch.config.LsOpenApiProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.whyitrose.batch")
@EnableJpaAuditing
@EntityScan(basePackages = "com.whyitrose.domain")
@EnableJpaRepositories(basePackages = "com.whyitrose.domain")
@EnableConfigurationProperties(LsOpenApiProperties.class)
@EnableScheduling
public class WhyItRoseBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(WhyItRoseBatchApplication.class, args);
    }
}

package com.whyitrose.batch;

import org.springframework.boot.SpringApplication;
import com.whyitrose.batch.config.LsOpenApiProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.whyitrose.batch")
@EntityScan(basePackages = "com.whyitrose.domain")
@EnableJpaRepositories(basePackages = "com.whyitrose.domain")
@EnableConfigurationProperties(LsOpenApiProperties.class)
public class WhyItRoseBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(WhyItRoseBatchApplication.class, args);
    }
}

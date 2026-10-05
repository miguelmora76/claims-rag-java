package dev.claimsrag;

import dev.claimsrag.config.AssistantProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AssistantProperties.class)
public class ClaimsRagApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClaimsRagApplication.class, args);
    }
}

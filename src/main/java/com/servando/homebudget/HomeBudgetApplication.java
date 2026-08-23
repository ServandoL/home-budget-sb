package com.servando.homebudget;

import com.servando.homebudget.configuration.MongoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
public class HomeBudgetApplication {

    public static void main(String[] args) {
        SpringApplication.run(HomeBudgetApplication.class, args);
    }

}

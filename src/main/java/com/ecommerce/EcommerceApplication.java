package com.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * This is the "main" file. Running this class starts the whole web server.
 * Everything else (security, controllers, database) is wired together automatically
 * by Spring Boot when this runs.
 */
@SpringBootApplication
public class EcommerceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EcommerceApplication.class, args);
    }
}

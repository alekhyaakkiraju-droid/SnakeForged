package com.snakeforged;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for SnakeForged Spring Boot application.
 * Starts an embedded Tomcat server with the game API and static asset serving.
 */
@SpringBootApplication
public class SnakeForgedApplication {

    public static void main(String[] args) {
        SpringApplication.run(SnakeForgedApplication.class, args);
    }
}

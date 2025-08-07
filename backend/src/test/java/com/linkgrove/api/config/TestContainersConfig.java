package com.linkgrove.api.config;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Test configuration for Testcontainers integration tests.
 * Sets up PostgreSQL and RabbitMQ containers for testing.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfig implements DisposableBean {

    private PostgreSQLContainer<?> postgresContainer;
    private RabbitMQContainer rabbitContainer;

    /**
     * PostgreSQL container for database testing
     */
    @Bean
    @ServiceConnection
    @Primary
    public PostgreSQLContainer<?> postgresContainer() {
        if (postgresContainer == null || !postgresContainer.isRunning()) {
            PostgreSQLContainer<?> newContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                    .withDatabaseName("linkgrove_test")
                    .withUsername("test")
                    .withPassword("test");
            newContainer.start();
            postgresContainer = newContainer;
        }
        return postgresContainer;
    }

    /**
     * RabbitMQ container for message queue testing
     */
    @Bean
    @ServiceConnection
    @Primary
    public RabbitMQContainer rabbitContainer() {
        if (rabbitContainer == null || !rabbitContainer.isRunning()) {
            RabbitMQContainer newContainer = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3-management-alpine"))
                    .withEnv("RABBITMQ_DEFAULT_USER", "test")
                    .withEnv("RABBITMQ_DEFAULT_PASS", "test");
            newContainer.start();
            rabbitContainer = newContainer;
        }
        return rabbitContainer;
    }

    @Override
    public void destroy() throws Exception {
        if (postgresContainer != null) {
            if (postgresContainer.isRunning()) {
                postgresContainer.stop();
            }
            postgresContainer.close();
        }
        if (rabbitContainer != null) {
            if (rabbitContainer.isRunning()) {
                rabbitContainer.stop();
            }
            rabbitContainer.close();
        }
    }
}

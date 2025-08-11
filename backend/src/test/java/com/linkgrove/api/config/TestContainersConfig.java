package com.linkgrove.api.config;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
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
    private GenericContainer<?> redisContainer;

    /**
     * PostgreSQL container for database testing
     */
    @Bean
    @ServiceConnection
    @Primary
    @SuppressWarnings("resource")
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
    @SuppressWarnings("resource")
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

    /**
     * Redis container for rate limiting and caching tests
     */
    @Bean
    @SuppressWarnings("resource")
    public GenericContainer<?> redisContainer() {
        if (redisContainer == null || !redisContainer.isRunning()) {
            GenericContainer<?> newContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);
            newContainer.start();
            redisContainer = newContainer;
        }
        return redisContainer;
    }

    /**
     * Provide a RedisConnectionFactory wired to the Testcontainers Redis
     */
    @Bean
    @Primary
    public LettuceConnectionFactory redisConnectionFactory(@Qualifier("redisContainer") GenericContainer<?> redisContainer) {
        String host = redisContainer.getHost();
        int port = redisContainer.getMappedPort(6379);
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
        return new LettuceConnectionFactory(config);
    }

    /**
     * Primary StringRedisTemplate backed by the Testcontainers Redis
     */
    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
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
        if (redisContainer != null) {
            if (redisContainer.isRunning()) {
                redisContainer.stop();
            }
            redisContainer.close();
        }
    }
}

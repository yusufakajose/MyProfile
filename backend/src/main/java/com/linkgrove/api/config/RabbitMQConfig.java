package com.linkgrove.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for asynchronous link click event processing.
 * Sets up queues, exchanges, and message serialization.
 */
@Configuration
@EnableRabbit
public class RabbitMQConfig {

    // Queue and Exchange Names
    public static final String LINK_CLICK_QUEUE = "linkgrove.clicks.queue";
    public static final String LINK_CLICK_EXCHANGE = "linkgrove.clicks.exchange";
    public static final String LINK_CLICK_ROUTING_KEY = "click.event";
    
    // Dead Letter Queue for failed processing
    public static final String LINK_CLICK_DLQ = "linkgrove.clicks.dlq";
    public static final String LINK_CLICK_DLX = "linkgrove.clicks.dlx";

    /**
     * Main queue for processing link click events
     */
    @Bean
    public Queue linkClickQueue() {
        return QueueBuilder.durable(LINK_CLICK_QUEUE)
                .withArgument("x-dead-letter-exchange", LINK_CLICK_DLX)
                .withArgument("x-dead-letter-routing-key", "failed.click")
                .build();
    }

    /**
     * Dead letter queue for failed click events
     */
    @Bean
    public Queue linkClickDeadLetterQueue() {
        return QueueBuilder.durable(LINK_CLICK_DLQ).build();
    }

    /**
     * Exchange for routing click events
     */
    @Bean
    public TopicExchange linkClickExchange() {
        return new TopicExchange(LINK_CLICK_EXCHANGE);
    }

    /**
     * Dead letter exchange for failed events
     */
    @Bean
    public DirectExchange linkClickDeadLetterExchange() {
        return new DirectExchange(LINK_CLICK_DLX);
    }

    /**
     * Binding for click events
     */
    @Bean
    public Binding linkClickBinding() {
        return BindingBuilder
                .bind(linkClickQueue())
                .to(linkClickExchange())
                .with(LINK_CLICK_ROUTING_KEY);
    }

    /**
     * Binding for dead letter queue
     */
    @Bean
    public Binding linkClickDeadLetterBinding() {
        return BindingBuilder
                .bind(linkClickDeadLetterQueue())
                .to(linkClickDeadLetterExchange())
                .with("failed.click");
    }

    /**
     * JSON message converter for event serialization
     */
    @Bean
    public MessageConverter messageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // Support for Java 8 time
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * RabbitTemplate with JSON serialization
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }

    /**
     * Container factory for message listeners
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setConcurrentConsumers(3); // Process 3 messages concurrently
        factory.setMaxConcurrentConsumers(10); // Scale up to 10 consumers under load
        factory.setPrefetchCount(10); // Prefetch 10 messages per consumer
        return factory;
    }
}

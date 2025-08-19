package com.rahman.productservice.service.impl;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;

public class RedisTestContainer extends GenericContainer<RedisTestContainer> {

    private static final String IMAGE = "redis:7.2-alpine";
    private static RedisTestContainer container;

    private RedisTestContainer() {
        super(IMAGE);
        withExposedPorts(6379);
        waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(30)));
    }

    public static RedisTestContainer getInstance() {
        if (container == null) {
            container = new RedisTestContainer();
            container.start();
        }
        return container;
    }
}

package com.astral.redis;

import org.slf4j.Logger;

import java.io.IOException;

public final class RedisService {

    private final String host;
    private final int port;
    private final int timeout;
    private final String password;
    private final Logger logger;

    private RedisSocketClient client;

    public RedisService(String host, int port, int timeout, String password, Logger logger) {
        this.host = host;
        this.port = port;
        this.timeout = timeout;
        this.password = password;
        this.logger = logger;
        reconnect();
    }
    public synchronized RedisSocketClient getClient() {
        try {
            if (client == null || !client.isAlive(timeout)) {
                reconnect();
            }
        } catch (Exception e) {
            logger.error("Redis reconnection failed", e);
        }
        return client;
    }

    private synchronized void reconnect() {
        try {
            if (client != null) {
                try { client.close(); } catch (IOException ignored) {}
            }

            client = new RedisSocketClient(host, port, timeout);

            if (password != null && !password.isBlank()) {
                logger.info("Authenticating with Redis...");
                boolean ok = client.auth(password);
                if (!ok) {
                    throw new IOException("AUTH failed");
                }
            } else {
                logger.debug("Redis AUTH skipped (no password)");
            }

            logger.info("Redis connected to {}:{}", host, port);
        } catch (IOException e) {
            logger.error("Unable to connect to Redis {}:{}", host, port, e);
            client = null;
        }
    }

    public boolean isAvailable() {
        return client != null && client.isAlive();
    }
}

package com.astral.info;

import com.astral.redis.RedisService;
import com.astral.redis.RedisSocketClient;
import me.internalizable.numdrassl.api.event.Subscribe;
import me.internalizable.numdrassl.api.event.connection.PostLoginEvent;
import org.slf4j.Logger;

import java.io.IOException;

public final class EventsRegistry {
    private final RedisService redisService;
    private final Logger logger;

    public EventsRegistry(RedisService redisService, Logger logger) {
        this.redisService = redisService;
        this.logger = logger;
    }

    @Subscribe
    public void onPlayerJoin(PostLoginEvent event) {
        updateServerCount(event);
    }

    @Subscribe
    public void onPlayerLeave(PostLoginEvent event) {
        updateServerCount(event);
    }

    private void updateServerCount(PostLoginEvent event) {
        if (!redisService.isAvailable()) {
            logger.warn("Redis no disponible para actualizar servidores");
            return;
        }
        RedisSocketClient redis = redisService.getClient();
        event.getPlayer().getCurrentServer().ifPresent(server -> {
            String serverName = server.getName();
            String newCount = String.valueOf(server.getPlayerCount());
            try {
                String oldCount = redis.hget("servers:players", serverName);
                if (newCount.equals(oldCount)) {
                    return;
                }
                redis.hset("servers:players", serverName, newCount);
                logger.debug("Redis actualizado: {} -> {} jugadores", serverName, newCount);
            } catch (IOException e) {
                logger.error("Error actualizando Redis para server {}", serverName, e);
            }
        });
    }
}

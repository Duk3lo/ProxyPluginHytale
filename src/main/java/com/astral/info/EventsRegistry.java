package com.astral.info;

import com.astral.Main;
import com.astral.redis.RedisService;
import com.astral.redis.RedisSocketClient;
import me.internalizable.numdrassl.api.ProxyServer;
import me.internalizable.numdrassl.api.event.Subscribe;
import me.internalizable.numdrassl.api.event.connection.DisconnectEvent;
import me.internalizable.numdrassl.api.event.connection.PostLoginEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public final class EventsRegistry {
    private static final Main plugin = Main.getInstance();
    private final ProxyServer proxy = plugin.getProxy();
    private final RedisService redisService;
    private final Logger logger;

    public EventsRegistry(RedisService redisService, Logger logger) {
        this.redisService = redisService;
        this.logger = logger;
    }

    @Subscribe
    public void onPlayerJoin(@NotNull PostLoginEvent event) {
        event.getPlayer().getCurrentServer().ifPresent(server -> proxy.getScheduler().runLater(plugin, ()->{
            String serverName = server.getName();
            int countPlayers = server.getPlayerCount();
            updateServerPlayersCount(serverName, countPlayers);
        }, 3, TimeUnit.SECONDS));
    }

    @Subscribe
    public void onPlayerLeave(@NotNull DisconnectEvent event) {
        event.getPlayer().getCurrentServer().ifPresent(server -> proxy.getScheduler().runLater(plugin, ()->{
            String serverName = server.getName();
            int countPlayers = server.getPlayerCount();
            updateServerPlayersCount(serverName, countPlayers);
        }, 3, TimeUnit.SECONDS));
    }

    private void updateServerPlayersCount(String serverName, int playerCount) {
        if (!redisService.isAvailable()) {
            logger.warn("Redis no disponible para actualizar servidores");
            return;
        }
        RedisSocketClient redis = redisService.getClient();
        String newCount = String.valueOf(playerCount);
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
    }

}

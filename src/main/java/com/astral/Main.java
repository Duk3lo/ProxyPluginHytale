package com.astral;

import com.astral.commands.CommandReload;
import com.astral.config.Configuration;
import com.astral.info.EventsRegistry;
import com.astral.redis.RedisService;
import com.astral.redis.RedisSocketClient;
import me.internalizable.numdrassl.api.Numdrassl;
import me.internalizable.numdrassl.api.ProxyServer;
import me.internalizable.numdrassl.api.event.Subscribe;
import me.internalizable.numdrassl.api.event.proxy.ProxyInitializeEvent;
import me.internalizable.numdrassl.api.plugin.DataDirectory;
import me.internalizable.numdrassl.api.plugin.Inject;
import me.internalizable.numdrassl.api.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Plugin(
        id = "HytaleProxyBase",
        name = "Hytale Proxy Base",
        version = "1.0.0",
        authors = {"Duk3lo"},
        description = "Simple proxy Plugin"
)
public final class Main {
    private static Main instance;
    @Inject private final ProxyServer proxy = Numdrassl.getProxy();
    @Inject private Logger logger;
    @Inject @DataDirectory private Path path;
    private RedisService redisService;

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        instance = this;
        Configuration config = new Configuration(path);
        config.load();
        redisService = new RedisService(config.redisHost(), config.redisPort(), config.redisTimeout(), config.redisPassword(), logger);
        registerServers(redisService);
        proxy.getCommandManager().register(instance, new CommandReload(logger, config));
        proxy.getEventManager().register(instance, new EventsRegistry(redisService, logger));
        logger.info("Proxy Plugin initialized");
    }

    private void registerServers(@NotNull RedisService redisService) {
        proxy.getScheduler().runLater(this, () -> {
            if (!redisService.isAvailable()) {
                logger.info("Redis no disponible al intentar registrar servidores, se omite registro inicial");
                return;
            }
            RedisSocketClient client = redisService.getClient();
            if (client == null) {
                logger.info("Redis client es null al intentar registrar servidores");
                return;
            }
            try {
                try {
                    client.del("servers:players");
                    logger.info("Se eliminó la key 'servers:players' anterior");
                } catch (IOException Del) {
                    logger.info("No se pudo eliminar 'servers:players' (continuando): {}", Del.getMessage());
                }

                proxy.getAllServers().forEach(server -> {
                    String name = server.getName();
                    int count = server.getPlayerCount();
                    try {
                        client.hset("servers:players", name, String.valueOf(count));
                    } catch (IOException e) {
                        logger.info("Error al escribir en Redis para server {} -> {}", name, e.getMessage(), e);
                    }
                });
                int totalPlayers = proxy.getPlayerCount();
                try {
                    client.set("global:players", String.valueOf(totalPlayers));
                    logger.info("Global players actualizado -> {}", totalPlayers);
                } catch (IOException e) {
                    logger.info("Error guardando global:players -> {}", e.getMessage(), e);
                }
                logger.info("Registro inicial de servers completado");
            } catch (Exception e) {
                logger.info("Error en registro inicial de servers: {}", e.getMessage(), e);
            }

        }, 2, TimeUnit.SECONDS);
    }

    public void setRedisService(RedisService redisService) {
        this.redisService = redisService;
    }

    public ProxyServer getProxy() {
        return proxy;
    }

    public static Main getInstance() {return instance;}
}

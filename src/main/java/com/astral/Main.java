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
import org.slf4j.Logger;
import java.nio.file.Path;

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


        redisService = new RedisService(
                config.redisHost(),
                config.redisPort(),
                config.redisTimeout(),
                logger
        );

        try {
            RedisSocketClient client = redisService.getClient();
            if (client != null && client.isAlive()) {
                logger.info("Redis conectado en {}:{}", config.redisHost(), config.redisPort());
            } else {
                logger.warn("No se pudo conectar a Redis en {}:{}", config.redisHost(), config.redisPort());
            }
        } catch (Exception e) {
            logger.error("Excepción al conectar a Redis en inicio", e);
        }

        proxy.getCommandManager().register(instance, new CommandReload(logger, config));
        proxy.getEventManager().register(instance, new EventsRegistry(redisService, logger));
        logger.info("Proxy Plugin initialized");
    }

    public void setRedisService(RedisService redisService) {
        this.redisService = redisService;
    }

    public ProxyServer getProxy() {
        return proxy;
    }

    public static Main getInstance() {return instance;}
}

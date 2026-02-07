package com.astral.commands;

import com.astral.Main;
import com.astral.config.Configuration;
import com.astral.redis.RedisService;
import me.internalizable.numdrassl.api.command.Command;
import me.internalizable.numdrassl.api.command.CommandResult;
import me.internalizable.numdrassl.api.command.CommandSource;
import me.internalizable.numdrassl.api.player.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Optional;


public final class CommandReload implements Command {

    private static final Main plugin = Main.getInstance();
    private final Logger logger;
    private final Configuration configuration;

    public CommandReload(Logger logger, Configuration configuration) {
        this.logger = logger;
        this.configuration = configuration;
    }

    @Contract(pure = true)
    @Override
    public @NotNull String getName() {
        return "reload";
    }

    @Contract(pure = true)
    @Override
    public @NotNull String getPermission() {
        return "numdrassl.command.reload_redis";
    }

    @Contract(pure = true)
    @Override
    public @NotNull String getDescription() {
        return "Reloads the plugin";
    }

    @Override
    public @NotNull CommandResult execute(@NotNull CommandSource commandSource, String @NotNull [] args) {
        if (args.length == 0) {
            commandSource.sendMessage("Usage: /reload <type>");
            return CommandResult.failure();
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("redis")) {
            Optional<Player> playerOpt = commandSource.asPlayer();
            playerOpt.ifPresent(player -> player.sendMessage("Reloading redis"));
            Configuration configuration = this.configuration;
            configuration.load();
            plugin.setRedisService(new RedisService(configuration.redisHost(), configuration.redisPort(), configuration.redisTimeout() ,logger));
            commandSource.sendMessage("Reloading redis");
            return CommandResult.success();
        }

        return CommandResult.failure();
    }
}

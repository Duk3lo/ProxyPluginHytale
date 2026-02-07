package com.astral.config;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class Configuration {

    private final Path file;
    private final Properties props = new Properties();

    public Configuration(@NotNull Path pluginDir) {
        this.file = pluginDir.resolve("config.properties");
    }

    public void load() {
        try {
            if (Files.notExists(file)) {
                createDefault();
            }

            try (InputStream in = Files.newInputStream(file)) {
                props.load(in);
            }

        } catch (IOException e) {
            throw new RuntimeException("Error cargando config", e);
        }
    }

    private void createDefault() throws IOException {
        Properties defaults = new Properties();
        defaults.setProperty("redis.host", "127.0.0.1");
        defaults.setProperty("redis.port", "6379");
        defaults.setProperty("redis.timeout", "1000");

        try (OutputStream out = Files.newOutputStream(file)) {
            defaults.store(out, "Astral Proxy Configuration");
        }
    }

    public String redisHost() {
        return props.getProperty("redis.host");
    }

    public int redisPort() {
        return Integer.parseInt(props.getProperty("redis.port"));
    }

    public int redisTimeout() {
        return Integer.parseInt(props.getProperty("redis.timeout"));
    }
}

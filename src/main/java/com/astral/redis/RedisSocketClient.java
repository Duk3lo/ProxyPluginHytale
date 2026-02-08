package com.astral.redis;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class RedisSocketClient implements Closeable {

    private final Socket socket;
    private final OutputStream out;
    private final BufferedInputStream in;
    private static final byte CR = '\r';
    private static final byte LF = '\n';

    public RedisSocketClient(String host, int port, int timeoutMillis) throws IOException {
        this.socket = new Socket();
        this.socket.connect(new java.net.InetSocketAddress(host, port), timeoutMillis);
        this.socket.setSoTimeout(timeoutMillis);
        this.out = socket.getOutputStream();
        this.in = new BufferedInputStream(socket.getInputStream());
    }


    private synchronized void sendCommand(String @NotNull ... args) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("*").append(args.length).append("\r\n");
        for (String arg : args) {
            byte[] data = arg.getBytes(StandardCharsets.UTF_8);
            sb.append("$").append(data.length).append("\r\n");
            sb.append(arg).append("\r\n");
        }
        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private synchronized String readLine() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            if (b == CR) {
                int next = in.read();
                if (next != LF) {
                    throw new IOException("Protocolo RESP inválido: se esperaba \\n");
                }
                break;
            }
            baos.write(b);
        }
        return baos.toString(StandardCharsets.UTF_8);
    }

    // Lee una respuesta RESP y la parsea a un Object Java
    private synchronized @Nullable Object readRESP() throws IOException {
        int prefix = in.read();
        if (prefix == -1) throw new IOException("Stream cerrado inesperadamente");

        switch (prefix) {
            case '+': // Simple String
                return readLine(); // devuelve el texto sin '+'
            case '-': // Error
                String err = readLine();
                throw new IOException("Redis error: " + err);
            case ':': // Integer
                String intLine = readLine();
                return Long.parseLong(intLine);
            case '$': // Bulk String
                String lenLine = readLine();
                int len = Integer.parseInt(lenLine);
                if (len == -1) return null; // nil bulk
                byte[] data = in.readNBytes(len);
                // consumir CRLF
                int cr = in.read();
                int lf = in.read();
                if (cr != CR || lf != LF) throw new IOException("Bulk string no terminó con CRLF");
                return new String(data, StandardCharsets.UTF_8);
            case '*': // Array
                String countLine = readLine();
                int count = Integer.parseInt(countLine);
                if (count == -1) return null;
                List<Object> items = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    items.add(readRESP());
                }
                return items;
            default:
                throw new IOException("Tipo RESP desconocido: " + (char) prefix);
        }
    }


    public @Nullable String ping() throws IOException {
        sendCommand("PING");
        Object resp = readRESP();
        return resp == null ? null : resp.toString();
    }

    public synchronized boolean auth(@NotNull String password) throws IOException {
        sendCommand("AUTH", password);
        Object resp = readRESP();
        return resp != null && "OK".equals(resp.toString());
    }

    public @Nullable String get(String key) throws IOException {
        sendCommand("GET", key);
        Object resp = readRESP();
        if (resp == null) return null;
        if (resp instanceof String) return (String) resp;
        throw new IOException("Respuesta inesperada para GET: " + resp);
    }

    public boolean set(String key, String value) throws IOException {
        sendCommand("SET", key, value);
        Object resp = readRESP();
        // SET devuelve Simple String "OK"
        return resp != null && "OK".equals(resp.toString());
    }

    public long del(String key) throws IOException {
        sendCommand("DEL", key);
        Object resp = readRESP();
        if (resp instanceof Long) return (Long) resp;
        if (resp instanceof String) return Long.parseLong((String) resp);
        throw new IOException("Respuesta inesperada para DEL: " + resp);
    }

    public boolean hset(String key, String field, String value) throws IOException {
        sendCommand("HSET", key, field, value);
        Object resp = readRESP();

        return resp instanceof Long;
    }

    public @Nullable String hget(String key, String field) throws IOException {
        sendCommand("HGET", key, field);
        Object resp = readRESP();

        if (resp == null) return null;
        return resp.toString();
    }

    public boolean hexists(String key, String field) throws IOException {
        sendCommand("HEXISTS", key, field);
        Object resp = readRESP();
        return resp instanceof Long && ((Long) resp) == 1L;
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    public boolean isAlive() {
        return isAlive(1000);
    }

    public boolean isAlive(int timeoutMillis) {
        try {
            int previousTimeout = socket.getSoTimeout();
            try {
                socket.setSoTimeout(timeoutMillis);
                sendCommand("PING");
                Object resp = readRESP();
                return resp != null && "PONG".equals(resp.toString());
            } finally {
                try {
                    socket.setSoTimeout(previousTimeout);
                } catch (IOException ignored) { }
            }
        } catch (IOException e) {
            return false;
        }
    }
}

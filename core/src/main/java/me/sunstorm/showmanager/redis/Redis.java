package me.sunstorm.showmanager.redis;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import me.sunstorm.showmanager.Worker;
import me.sunstorm.showmanager.redis.converter.Converter;
import me.sunstorm.showmanager.redis.converter.GzipConverter;
import me.sunstorm.showmanager.util.Tuple;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

// the redis handling design is loosely based on lucko helper-redis
// https://github.com/lucko/helper/tree/master/helper-redis
@Slf4j
public class Redis {
    private final JedisPool jedisPool;
    private PubSubListener listener = null;
    private final Map<String, MessageHandler<?>> handlers = new ConcurrentHashMap<>();
    private BlockingQueue<Tuple<byte[], byte[]>> sendQueue = new LinkedBlockingDeque<>();

    public Redis(@NotNull RedisCredentials credentials) {
        val executor = Worker.getInstance().getScheduler();
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(16);

        jedisPool = new JedisPool(config, credentials.getAddress(), credentials.getPort(), 2000, credentials.getPassword());

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.ping();
        }

        executor.execute(new Runnable() {
            private boolean broken = false;

            @Override
            public void run() {
                if (broken) {
                    log.info("[redis] Retrying subscription...");
                    broken = false;
                }

                try (Jedis jedis = jedisPool.getResource()) {
                    try {
                        listener = new PubSubListener(Redis.this);
                        jedis.psubscribe(listener, "showmanager".getBytes(StandardCharsets.UTF_8));
                    } catch (Exception e) {
                        log.error("Error subscribing to listener", e);
                        try {
                            listener.unsubscribe();
                        } catch (Exception ignored) {}
                        listener = null;
                        broken = true;
                    }
                }

                if (broken)
                    executor.schedule(this, 1, TimeUnit.SECONDS);
            }
        });

        executor.scheduleAtFixedRate(() -> {
            if (listener == null || !listener.isSubscribed())
                return;

            handlers.forEach((channel, handler) -> listener.subscribe(channel.getBytes(StandardCharsets.UTF_8)));
        }, 100, 100, TimeUnit.MILLISECONDS);

        startSendThread();
    }

    public void addMessageHandler(MessageHandler<?> handler) {
        handlers.put(handler.getChannel(), handler);
        listener.subscribe(handler.getChannel().getBytes(StandardCharsets.UTF_8));
    }

    public void removeHandler(MessageHandler<?> handler) {
        handlers.remove(handler.getChannel());
        listener.unsubscribe(handler.getChannel().getBytes(StandardCharsets.UTF_8));
    }

    public <T> void sendMessage(T message, MessageHandler<T> handler) {
        if (!handlers.containsValue(handler)) {
            log.warn("Tried to send unknown message type: {}", message.getClass().getSimpleName());
            return;
        }
        Converter<T> converter = new GzipConverter<>(handler.getConverter());
        byte[] data = converter.encode(message);
        sendQueue.offer(new Tuple<>(handler.getChannel().getBytes(StandardCharsets.UTF_8), data));
    }

    protected <T> void incomingMessage(String channel, byte[] message) {
        if (!handlers.containsKey(channel)) {
            log.warn("Received message on unknown channel");
            return;
        }
        MessageHandler<T> handler = (MessageHandler<T>) handlers.get(channel);
        try {
            T data = new GzipConverter<>(handler.getConverter()).decode(message);
            handler.handleMessage(data);
        } catch (Exception e) {
            log.error("Failed to handle redis message", e);
        }
    }

    private void startSendThread() {
        Thread t = new Thread(() -> {
            while (true) {
                try (Jedis jedis = jedisPool.getResource()) {
                    Tuple<byte[], byte[]> data = sendQueue.take();
                    jedis.publish(data.getFirst(), data.getSecond());
                } catch (InterruptedException e) {
                    log.error("SendQueue wait interrupted", e);
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }
}

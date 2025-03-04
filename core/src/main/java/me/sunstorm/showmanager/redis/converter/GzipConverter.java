package me.sunstorm.showmanager.redis.converter;

import com.google.common.io.ByteStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GzipConverter<T> implements Converter<T> {
    private static final Logger log = LoggerFactory.getLogger(GzipConverter.class);

    private final Converter<T> parent;

    public GzipConverter(Converter<T> parent) {
        this.parent = parent;
    }

    @Override
    public byte[] encode(T message) {
        byte[] encoded = parent.encode(message);
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(byteOut)) {
            gzipOut.write(encoded);
        } catch (IOException e) {
            log.error("Failed to compress message", e);
        }
        return byteOut.toByteArray();
    }

    @Override
    public T decode(byte[] message) {
        byte[] uncompressed;
        try (GZIPInputStream gzipIn = new GZIPInputStream(new ByteArrayInputStream(message))) {
            uncompressed = ByteStreams.toByteArray(gzipIn);
        } catch (IOException e) {
            log.error("Failed to decompress message", e);
            return null;
        }
        return parent.decode(uncompressed);
    }
}

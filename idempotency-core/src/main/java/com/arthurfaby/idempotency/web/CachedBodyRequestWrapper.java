package com.arthurfaby.idempotency.web;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.springframework.util.StreamUtils;

/**
 * Wraps a request so its body can be read multiple times: the bytes are read once
 * upfront and served from memory. Needed because we fingerprint the body in the
 * interceptor before the controller reads it.
 */
public class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] body;

    public CachedBodyRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        this.body = StreamUtils.copyToByteArray(request.getInputStream());
    }

    /** The cached request body. */
    public byte[] getBody() {
        return body.clone();
    }

    @Override
    public ServletInputStream getInputStream() {
        return new CachedBodyServletInputStream(body);
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(body), StandardCharsets.UTF_8));
    }

    /** Serves the cached bytes as a (re-createable) servlet input stream. */
    private static final class CachedBodyServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream buffer;

        CachedBodyServletInputStream(byte[] body) {
            this.buffer = new ByteArrayInputStream(body);
        }

        @Override
        public int read() {
            return buffer.read();
        }

        @Override
        public boolean isFinished() {
            return buffer.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException("Async reads are not supported");
        }
    }
}

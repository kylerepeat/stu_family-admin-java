package com.stufamily.backend.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stufamily.backend.shared.api.ApiResponse;
import com.stufamily.backend.shared.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ReplayGuardFilter extends OncePerRequestFilter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final ConcurrentHashMap<String, Long> GET_LAST_REQUEST_NS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> WRITE_LAST_REQUEST_NS = new ConcurrentHashMap<>();
    private static final AtomicLong GET_REQUEST_COUNTER = new AtomicLong(0);
    private static final AtomicLong WRITE_REQUEST_COUNTER = new AtomicLong(0);

    private static final int CLEANUP_INTERVAL = 1024;
    private static final int CLEANUP_SCAN_LIMIT = 512;
    private static final int MAX_KEYS_PER_MAP = 20_000;

    private static final long GET_WINDOW_NS = 200_000_000L;      // 200ms
    private static final long WRITE_WINDOW_NS = 2_000_000_000L;  // 2s

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean isGet = "GET".equalsIgnoreCase(method);
        long windowNs = isGet ? GET_WINDOW_NS : WRITE_WINDOW_NS;
        long nowNs = System.nanoTime();
        String key = buildReplayKey(request, method);

        ConcurrentHashMap<String, Long> map = isGet ? GET_LAST_REQUEST_NS : WRITE_LAST_REQUEST_NS;
        if (isReplay(map, key, nowNs, windowNs)) {
            writeRejectedResponse(response);
            return;
        }

        cleanupIfNeeded(
            map,
            isGet ? GET_REQUEST_COUNTER : WRITE_REQUEST_COUNTER,
            nowNs,
            windowNs * 10
        );
        filterChain.doFilter(request, response);
    }

    private boolean isReplay(ConcurrentHashMap<String, Long> map, String key, long nowNs, long windowNs) {
        AtomicBoolean replayed = new AtomicBoolean(false);
        map.compute(key, (k, lastNs) -> {
            if (lastNs != null && nowNs - lastNs < windowNs) {
                replayed.set(true);
                return lastNs;
            }
            return nowNs;
        });
        return replayed.get();
    }

    private String buildReplayKey(HttpServletRequest request, String method) {
        String clientFingerprint = resolveClientFingerprint(request);
        String uri = request.getRequestURI();
        String query = request.getQueryString();

        int capacity = method.length() + uri.length() + clientFingerprint.length() + 3;
        if (StringUtils.hasText(query)) {
            capacity += query.length() + 1;
        }
        StringBuilder sb = new StringBuilder(capacity);
        sb.append(method).append('|').append(uri);
        if (StringUtils.hasText(query)) {
            sb.append('?').append(query);
        }
        sb.append('|').append(clientFingerprint);
        return sb.toString();
    }

    private String resolveClientFingerprint(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasText(authorization)) {
            String token = authorization.trim();
            // Store only a compact token fingerprint to reduce memory use.
            return "auth:" + Integer.toHexString(token.hashCode());
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            String firstIp = xff.split(",")[0].trim();
            if (StringUtils.hasText(firstIp)) {
                return "ip:" + firstIp;
            }
        }
        return "ip:" + request.getRemoteAddr();
    }

    private void cleanupIfNeeded(ConcurrentHashMap<String, Long> map, AtomicLong counter, long nowNs, long staleThresholdNs) {
        long current = counter.incrementAndGet();
        if ((current & (CLEANUP_INTERVAL - 1)) != 0) {
            return;
        }
        if (map.size() <= MAX_KEYS_PER_MAP) {
            return;
        }

        int scanned = 0;
        for (Map.Entry<String, Long> entry : map.entrySet()) {
            if (scanned++ >= CLEANUP_SCAN_LIMIT) {
                break;
            }
            Long ts = entry.getValue();
            if (ts != null && nowNs - ts > staleThresholdNs) {
                map.remove(entry.getKey(), ts);
            }
        }

        int oversize = map.size() - MAX_KEYS_PER_MAP;
        if (oversize <= 0) {
            return;
        }
        int forceRemove = Math.min(oversize, CLEANUP_SCAN_LIMIT / 2);
        int removed = 0;
        for (String key : map.keySet()) {
            if (removed >= forceRemove) {
                break;
            }
            if (map.remove(key) != null) {
                removed++;
            }
        }
    }

    private void writeRejectedResponse(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String body = OBJECT_MAPPER.writeValueAsString(
            ApiResponse.failure(ErrorCode.TOO_MANY_REQUESTS.code(), "请求过于频繁，请稍后再试"));
        response.getWriter().write(body);
    }

    public static void clearForTest() {
        GET_LAST_REQUEST_NS.clear();
        WRITE_LAST_REQUEST_NS.clear();
        GET_REQUEST_COUNTER.set(0);
        WRITE_REQUEST_COUNTER.set(0);
    }
}

package com.world.i2v.i2vauthapi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple Rate Limiting Filter Implementation without external dependencies.
 * Uses a sliding window counter approach.
 */
@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int HTTP_TOO_MANY_REQUESTS = 429;     // HTTP 429 status code

    @Value("${security.rate-limit.auth.requests:5}")
    private int authRateLimit;

    @Value("${security.rate-limit.auth.window-ms:60000}")
    private long authRateLimitWindowMs;

    @Value("${security.rate-limit.general.requests:300}")
    private int generalRateLimit;

    @Value("${security.rate-limit.general.window-ms:60000}")
    private long generalRateLimitWindowMs;

    private static class RequestCount {
        private long firstRequestTime;
        private int count;

        RequestCount(long time) {
            this.firstRequestTime = time;
            this.count = 1;
        }

        boolean isExpired(long currentTime, long windowSize) {
            return (currentTime - firstRequestTime) > windowSize;
        }
    }

    private final Map<String, RequestCount> authBuckets = new ConcurrentHashMap<>();
    private final Map<String, RequestCount> generalBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = getClientIp(request);
        String requestPath = request.getRequestURI();
        long currentTime = Instant.now().toEpochMilli();

        // Apply stricter rate limiting to sensitive auth endpoints
        if (requestPath.startsWith("/api/auth/login") || 
            requestPath.startsWith("/api/auth/signup") ||
            requestPath.startsWith("/api/auth/forgot-password")) {
            
            if (!checkRateLimit(clientIp, currentTime, authRateLimit, authRateLimitWindowMs, authBuckets)) {
                log.warn("Rate limit exceeded for IP: {} on path: {} (Auth endpoint)", clientIp, requestPath);
                response.setStatus(HTTP_TOO_MANY_REQUESTS);
                response.setContentType("application/json");
                response.setHeader("Retry-After", String.valueOf(authRateLimitWindowMs / 1000));
                response.getWriter().write(
                    "{\"error\":\"Too many authentication attempts. Please try again after 1 minute.\",\"retryAfter\":60}"
                );
                return;
            }
        } else {
            // Keep general API limits per route so normal dashboard startup bursts do not
            // exhaust a single IP-wide bucket shared by every endpoint.
            String generalBucketKey = clientIp + ":" + request.getMethod() + ":" + requestPath;
            if (!checkRateLimit(generalBucketKey, currentTime, generalRateLimit, generalRateLimitWindowMs, generalBuckets)) {
                log.warn("Rate limit exceeded for IP: {} on path: {} (General endpoint)", clientIp, requestPath);
                response.setStatus(HTTP_TOO_MANY_REQUESTS);
                response.setContentType("application/json");
                response.setHeader("Retry-After", String.valueOf(generalRateLimitWindowMs / 1000));
                response.getWriter().write(
                    "{\"error\":\"Too many requests. Please try again later.\"}"
                );
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean checkRateLimit(String bucketKey, long currentTime, int limit, long windowSize,
                                    Map<String, RequestCount> buckets) {
        if (limit <= 0) {
            return true;
        }

        RequestCount counter = buckets.compute(bucketKey, (key, existing) -> {
            if (existing == null || existing.isExpired(currentTime, windowSize)) {
                return new RequestCount(currentTime);
            }
            existing.count++;
            return existing;
        });
        return counter.count <= limit;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // Don't apply rate limiting to preflight, non-API, or health/status endpoints.
        String path = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || !path.startsWith("/api/")
                || path.equals("/health")
                || path.equals("/actuator/health")
                || path.startsWith("/public/")
                || path.startsWith("/api/public/");
    }
}

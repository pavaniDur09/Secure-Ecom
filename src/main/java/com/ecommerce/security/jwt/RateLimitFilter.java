package com.ecommerce.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A beginner-friendly rate limiter: it counts requests per IP address in a fixed
 * time window and rejects requests once the limit is hit. Only applied to the
 * login endpoint here, to stop brute-force password guessing.
 *
 * The real project spec suggests Resilience4j (a proper library for this) - swap
 * this class out for that once you're comfortable with the basic idea below.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${security.rate-limit.max-requests}")
    private int maxRequests;

    @Value("${security.rate-limit.window-seconds}")
    private int windowSeconds;

    private final ConcurrentHashMap<String, RequestWindow> requestCounts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        // Only rate-limit the login endpoint (where brute force attacks happen)
        if (!"/api/auth/login".equals(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = request.getRemoteAddr();
        long nowSeconds = System.currentTimeMillis() / 1000;

        RequestWindow window = requestCounts.computeIfAbsent(clientIp, ip -> new RequestWindow(nowSeconds));

        synchronized (window) {
            if (nowSeconds - window.windowStartSecond >= windowSeconds) {
                // window expired, start a fresh count
                window.windowStartSecond = nowSeconds;
                window.count.set(0);
            }

            int currentCount = window.count.incrementAndGet();
            int remaining = Math.max(0, maxRequests - currentCount);
            response.setHeader("X-Rate-Limit-Limit", String.valueOf(maxRequests));
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(remaining));

            if (currentCount > maxRequests) {
                response.setStatus(429); // 429 Too Many Requests (not a predefined constant in HttpServletResponse)
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"error\":\"Too many login attempts. Please try again later.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private static class RequestWindow {
        volatile long windowStartSecond;
        final AtomicInteger count = new AtomicInteger(0);

        RequestWindow(long windowStartSecond) {
            this.windowStartSecond = windowStartSecond;
        }
    }
}

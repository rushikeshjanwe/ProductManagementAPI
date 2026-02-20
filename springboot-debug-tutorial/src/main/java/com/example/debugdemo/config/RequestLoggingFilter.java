package com.example.debugdemo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Request Logging Filter
 * 
 * ═══════════════════════════════════════════════════════════════════
 * DEBUGGING MASTERCLASS - HTTP REQUEST/RESPONSE LOGGING
 * ═══════════════════════════════════════════════════════════════════
 * 
 * This filter logs every HTTP request and response passing through
 * your application. It's invaluable for debugging:
 * 
 * 1. REQUEST TRACING:
 *    - Each request gets a unique ID (requestId)
 *    - Use this ID to correlate logs across layers
 *    - Find all logs for a specific request using: grep "requestId=ABC123"
 * 
 * 2. TIMING ANALYSIS:
 *    - Logs how long each request takes
 *    - Helps identify slow endpoints
 * 
 * 3. PAYLOAD INSPECTION:
 *    - Can log request/response bodies (careful with sensitive data!)
 *    - Useful for debugging serialization issues
 * 
 * DEBUGGING TIP: Set a breakpoint in doFilterInternal() to inspect
 * every request before it reaches your controller.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response,
                                    FilterChain filterChain) 
            throws ServletException, IOException {
        
        // Generate unique request ID for tracing
        String requestId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        // Add request ID to MDC (Mapped Diagnostic Context)
        // This makes it appear in all logs during this request
        MDC.put("requestId", requestId);
        
        // Wrap request and response to allow reading body multiple times
        ContentCachingRequestWrapper wrappedRequest = 
                new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = 
                new ContentCachingResponseWrapper(response);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Log incoming request
            logRequest(requestId, wrappedRequest);
            
            // Process the request
            filterChain.doFilter(wrappedRequest, wrappedResponse);
            
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            // Log response
            logResponse(requestId, wrappedResponse, duration);
            
            // IMPORTANT: Copy response body back to the actual response
            wrappedResponse.copyBodyToResponse();
            
            // Clean up MDC
            MDC.remove("requestId");
        }
    }

    private void logRequest(String requestId, ContentCachingRequestWrapper request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String fullPath = queryString != null ? uri + "?" + queryString : uri;
        
        // Skip logging for actuator endpoints (too noisy)
        if (uri.startsWith("/actuator")) {
            return;
        }
        
        logger.info("[{}] --> {} {}", requestId, method, fullPath);
        
        // Log headers at debug level
        if (logger.isDebugEnabled()) {
            request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
                // Don't log sensitive headers
                if (!headerName.equalsIgnoreCase("Authorization") && 
                    !headerName.equalsIgnoreCase("Cookie")) {
                    logger.debug("[{}] Header: {}={}", 
                            requestId, headerName, request.getHeader(headerName));
                }
            });
        }
        
        // Log request body at trace level (careful with large payloads!)
        if (logger.isTraceEnabled()) {
            byte[] content = request.getContentAsByteArray();
            if (content.length > 0 && content.length < 10000) {  // Limit size
                String body = new String(content, StandardCharsets.UTF_8);
                logger.trace("[{}] Request body: {}", requestId, body);
            }
        }
    }

    private void logResponse(String requestId, ContentCachingResponseWrapper response, 
                            long duration) {
        int status = response.getStatus();
        
        // Skip logging for actuator endpoints
        if (response.getContentType() != null && 
            response.getContentType().contains("actuator")) {
            return;
        }
        
        // Use appropriate log level based on status
        String statusCategory = getStatusCategory(status);
        
        if (status >= 500) {
            logger.error("[{}] <-- {} {} ({}ms)", requestId, status, statusCategory, duration);
        } else if (status >= 400) {
            logger.warn("[{}] <-- {} {} ({}ms)", requestId, status, statusCategory, duration);
        } else {
            logger.info("[{}] <-- {} {} ({}ms)", requestId, status, statusCategory, duration);
        }
        
        // Log slow requests
        if (duration > 1000) {
            logger.warn("[{}] SLOW REQUEST: took {}ms", requestId, duration);
        }
        
        // Log response body at trace level
        if (logger.isTraceEnabled()) {
            byte[] content = response.getContentAsByteArray();
            if (content.length > 0 && content.length < 10000) {
                String body = new String(content, StandardCharsets.UTF_8);
                logger.trace("[{}] Response body: {}", requestId, body);
            }
        }
    }

    private String getStatusCategory(int status) {
        if (status >= 200 && status < 300) return "OK";
        if (status >= 300 && status < 400) return "REDIRECT";
        if (status >= 400 && status < 500) return "CLIENT_ERROR";
        if (status >= 500) return "SERVER_ERROR";
        return "UNKNOWN";
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Don't filter static resources
        String path = request.getRequestURI();
        return path.contains("/static/") || 
               path.contains("/favicon.ico") ||
               path.contains("/h2-console");
    }
}

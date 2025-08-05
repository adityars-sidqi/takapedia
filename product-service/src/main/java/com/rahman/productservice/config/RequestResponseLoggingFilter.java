package com.rahman.productservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String traceId = Optional.ofNullable(request.getHeader(TRACE_ID_HEADER))
                .orElse(UUID.randomUUID().toString());

        MDC.put("traceId", traceId);

        // Wrap request and response sebelum doFilter
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        wrappedResponse.addHeader(TRACE_ID_HEADER, traceId);

        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            logRequest(wrappedRequest);
            logResponse(wrappedRequest, wrappedResponse, duration);

            wrappedResponse.copyBodyToResponse(); // penting untuk menyalin ulang response body
            MDC.remove("traceId");
        }
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        String body = getRequestBody(request);
        String headers = Collections.list(request.getHeaderNames())
                .stream()
                .map(name -> name + ": " + request.getHeader(name))
                .collect(Collectors.joining(", "));

        log.info("[HTTP REQUEST] {} {} | Headers: [{}] | Body: {}",
                request.getMethod(),
                request.getRequestURI(),
                headers,
                body
        );
    }

    private void logResponse(ContentCachingRequestWrapper request,
                             ContentCachingResponseWrapper response,
                             long duration) {
        String responseBody = getResponseBody(response);
        String headers = response.getHeaderNames()
                .stream()
                .map(name -> name + ": " + response.getHeader(name))
                .collect(Collectors.joining(", "));
        log.info("[HTTP RESPONSE] {} {} | Headers: [{}] | Status: {} | Time: {} ms | Body: {}",
                request.getMethod(),
                request.getRequestURI(),
                headers,
                response.getStatus(),
                duration,
                responseBody
        );
    }

    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] buf = request.getContentAsByteArray();
        if (buf.length == 0) return "";
        try {
            return new String(buf, 0, buf.length, request.getCharacterEncoding());
        } catch (UnsupportedEncodingException ex) {
            return "[Unsupported Encoding]";
        }
    }

    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] buf = response.getContentAsByteArray();
        if (buf.length == 0) return "";
        try {
            return new String(buf, 0, buf.length, response.getCharacterEncoding());
        } catch (UnsupportedEncodingException ex) {
            return "[Unsupported Encoding]";
        }
    }
}


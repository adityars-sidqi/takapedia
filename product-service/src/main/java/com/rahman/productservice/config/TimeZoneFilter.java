package com.rahman.productservice.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.ZoneId;

@Component
public class TimeZoneFilter implements Filter{
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        var httpRequest = (HttpServletRequest) servletRequest;
        String timeZoneHeader = httpRequest.getHeader("Time-Zone");

        try {
            if (timeZoneHeader != null) {
                TimeZoneContext.setZoneId(ZoneId.of(timeZoneHeader));
            }
        } catch (Exception ignored) {
            TimeZoneContext.setZoneId(ZoneId.of("UTC"));
        }

        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            TimeZoneContext.clear();
        }
    }
}

package com.example.demo.config;

import com.example.demo.service.CustomUserDetailsService;
import com.example.demo.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Skip authentication for public resources
        String requestPath = request.getRequestURI();
        if (requestPath.equals("/login") || requestPath.equals("/register") ||
                requestPath.startsWith("/css/") || requestPath.startsWith("/js/") ||
                requestPath.startsWith("/images/") || requestPath.equals("/favicon.ico")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        String jwt = null;
        String username = null;

        // Extract JWT from Authorization header or cookie
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
        } else {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("jwtToken".equals(cookie.getName())) {
                        jwt = cookie.getValue();
                        break;
                    }
                }
            }
        }

        // If no JWT found, continue without authentication
        if (jwt == null || jwt.trim().isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            username = jwtService.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    // Verify role consistency between JWT and database
                    Claims claims = jwtService.extractAllClaims(jwt);
                    String jwtRole = claims.get("role", String.class);
                    String userRole = userDetails.getUser().getRole().name();

                    if (userRole.equals(jwtRole)) {
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);

                        logger.info("Authenticated user: {} with role: {}", username, userRole);
                    } else {
                        logger.warn("Role mismatch for user: {}. JWT: {}, DB: {}", username, jwtRole, userRole);
                        SecurityContextHolder.clearContext();
                    }
                } else {
                    logger.warn("Invalid JWT token for user: {}", username);
                    SecurityContextHolder.clearContext();
                }
            }
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            logger.warn("JWT token expired for user: {}", username);
            SecurityContextHolder.clearContext();
        } catch (io.jsonwebtoken.SignatureException e) {
            logger.warn("Invalid JWT signature for user: {}", username);
            SecurityContextHolder.clearContext();
        } catch (Exception e) {
            logger.error("JWT authentication failed for user: {}", username, e);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
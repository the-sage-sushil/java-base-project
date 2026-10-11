package com.sushil.base_project.core.security;

import java.io.IOException;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration 
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // 1. Check if the Authorization header is missing or doesn't start with "Bearer
        // "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 2. Extract the token
            final String jwt = authHeader.substring(7);

            // 3. Extract the username or user identifier (e.g., email) from the token
            final String userEmail = jwtService.extractUserId(jwt);

            // 4. Check if username is present and there is no active authentication in the
            // context
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Load user details from the database (via CustomUserDetailsService)
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                // 5. Validate the token against the user details
                if (jwtService.validateToken(jwt, userDetails)) {

                    // Create the authentication token with roles/authorities
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities());

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 6. Set the authenticated user in the Spring Security context
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Token validation failed (expired, malformed, tampered, etc.)
            // We clear context or let it pass through; Spring Security will handle missing
            // auth downstream if required
            SecurityContextHolder.clearContext();
        }

        // 7. Proceed with the remaining filters in the chain
        filterChain.doFilter(request, response);
    }
}
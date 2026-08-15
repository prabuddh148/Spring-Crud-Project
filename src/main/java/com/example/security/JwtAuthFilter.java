package com.example.security;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.model.User;
import com.example.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

        @Autowired
        private JwtService jwtService;

        @Autowired
        private UserRepository userRepository;

        @Override
        protected void doFilterInternal(
                        HttpServletRequest request,
                        HttpServletResponse response,
                        FilterChain filterChain)
                        throws ServletException, IOException {

                // 1. Get Authorization header

                String authHeader = request.getHeader("Authorization");

                // 2. Check Bearer token

                if (authHeader == null ||
                                !authHeader.startsWith("Bearer ")) {

                        filterChain.doFilter(request, response);
                        return;
                }

                // 3. Remove "Bearer "

                String token = authHeader.substring(7);

                // 4. Validate token

                if (!jwtService.isTokenValid(token)) {

                        filterChain.doFilter(request, response);
                        return;
                }

                // 5. Get email from token

                String email = jwtService.extractEmail(token);

                // 6. Find user from database

                User user = userRepository
                                .findByEmail(email)
                                .orElse(null);

                if (user != null &&
                                SecurityContextHolder
                                                .getContext()
                                                .getAuthentication() == null) {

                        // 7. Get user's role

                        String role = user.getRole().getName();

                        // 8. Convert role into Spring authority

                        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(
                                        "ROLE_" + role);

                        // 9. Create authentication

                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                        user,
                                        null,
                                        List.of(authority));

                        // 10. Add request details

                        authentication.setDetails(
                                        new WebAuthenticationDetailsSource()
                                                        .buildDetails(request));

                        // 11. Store authentication

                        SecurityContextHolder
                                        .getContext()
                                        .setAuthentication(authentication);
                }

                // 12. Continue request

                filterChain.doFilter(request, response);
        }
}
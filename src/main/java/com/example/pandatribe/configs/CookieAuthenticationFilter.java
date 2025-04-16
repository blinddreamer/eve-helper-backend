package com.example.pandatribe.configs;

import com.example.pandatribe.models.dbmodels.auth.OAuthToken;
import com.example.pandatribe.services.authentication.OAuthTokenService;
import com.example.pandatribe.utils.Helper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CookieAuthenticationFilter extends OncePerRequestFilter {

    private final OAuthTokenService tokenService;
    private final Helper encryptor;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String encryptedUUID = getCookieValue(request, "sessionUUID");

        if (encryptedUUID != null) {
            try {
                String uuid = encryptor.decompressUUID(encryptedUUID).toString();
                OAuthToken token = tokenService.checkTokenExist(uuid);

                if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // Create authentication token
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(uuid, null, List.of(new SimpleGrantedAuthority("USER")));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }


            } catch (Exception e) {
                logger.error("Cookie authentication failed: " + e.getMessage());
            }
        }

        chain.doFilter(request, response);
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals(name)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}

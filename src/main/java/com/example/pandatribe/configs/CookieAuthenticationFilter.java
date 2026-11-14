package com.example.pandatribe.configs;

import com.example.pandatribe.services.authentication.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates requests carrying a valid session cookie. The principal name is the account id.
 */
@Component
@RequiredArgsConstructor
public class CookieAuthenticationFilter extends OncePerRequestFilter {

    private final SessionService sessionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        var cookie = WebUtils.getCookie(request, SessionService.SESSION_COOKIE);
        if (cookie != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            sessionService.resolveAccountId(cookie.getValue()).ifPresent(accountId ->
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(accountId, null,
                                    List.of(new SimpleGrantedAuthority("USER")))));
        }

        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only the auth endpoints need the session; skip the DB lookup everywhere else
        return !request.getRequestURI().startsWith("/api/v1/auth/");
    }
}

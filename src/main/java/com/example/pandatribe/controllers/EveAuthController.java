package com.example.pandatribe.controllers;

import com.example.pandatribe.models.authentication.TokenResponse;
import com.example.pandatribe.models.results.CharResult;
import com.example.pandatribe.services.authentication.OAuthTokenService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/")
@AllArgsConstructor
public class EveAuthController {
    private final OAuthTokenService tokenService;



    @GetMapping("/callback")
    public void handleCallback(@RequestParam("code") String code, HttpServletResponse response) throws IOException {
        log.info("Received code {}", code);
        // Use the authorization code to get access and refresh tokens
        CharResult charResult = tokenService.exchangeCodeForTokens(code);

        response.setContentType("text/html");
        response.getWriter().write("<script>"
                + "window.opener.postMessage({ character: { name: '"
                + charResult.getName() + "', portrait: '"
                + charResult.getAvatar() + "', id: '"
                + charResult.getCharId() + "' } }, 'http://localhost:3000');"
                + "window.close();</script>");



    }


}

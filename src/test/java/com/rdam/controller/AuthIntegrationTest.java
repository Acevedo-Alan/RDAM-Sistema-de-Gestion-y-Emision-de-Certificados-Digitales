package com.rdam.controller;

import com.rdam.BaseIntegrationTest;
import com.rdam.service.EmailService;
import com.rdam.service.OtpService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql(scripts = "/test-data-auth.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class AuthIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OtpService otpService;

    @MockitoBean
    private EmailService emailService;

    @Test
    void login_credencialesValidas_retorna200ConMensajeOtp() throws Exception {
        when(otpService.generarOtp(anyString())).thenReturn("123456");
        doNothing().when(emailService).enviarOtp(anyString(), anyString());

        Map<String, String> request = Map.of("username", "20111111111", "password", "Test1234");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void login_credencialesInvalidas_retorna401() throws Exception {
        Map<String, String> request = Map.of("username", "20111111111", "password", "wrongpassword");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void verify_otpValido_retorna200ConToken() throws Exception {
        when(otpService.generarOtp(anyString())).thenReturn("123456");
        when(otpService.validarOtp("test-ciudadano@test.com", "123456")).thenReturn(true);
        doNothing().when(emailService).enviarOtp(anyString(), anyString());

        Map<String, String> loginReq = Map.of("username", "20111111111", "password", "Test1234");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk());

        Map<String, String> verifyReq = Map.of("email", "test-ciudadano@test.com", "codigo", "123456");
        mockMvc.perform(post("/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void login_cuilInexistente_retorna401() throws Exception {
        Map<String, String> request = Map.of("username", "99999999999", "password", "password123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
package com.rdam.controller;

import com.rdam.BaseIntegrationTest;
import com.rdam.JwtTestHelper;
import com.rdam.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql(scripts = "/test-data-solicitud.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class SolicitudIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmailService emailService;

    private static final int CIUDADANO_USER_ID = 100;
    private static final String CIUDADANO_CUIL = "20222222222";
    private static final int INTERNO_USER_ID = 101;
    private static final String INTERNO_LEGAJO = "LEG-TEST-001";
    private static final int CIRCUNSCRIPCION_ID = 1;
    private static final int OTRO_CIUDADANO_USER_ID = 102;
    private static final String OTRO_CIUDADANO_CUIL = "20333333333";

    private String jwtCiudadano;
    private String jwtInterno;
    private String jwtOtroCiudadano;

    @BeforeEach
    void setUpTokens() {
        jwtCiudadano = JwtTestHelper.generarTokenCiudadano(CIUDADANO_USER_ID, CIUDADANO_CUIL);
        jwtInterno = JwtTestHelper.generarTokenInterno(INTERNO_USER_ID, INTERNO_LEGAJO, CIRCUNSCRIPCION_ID);
        jwtOtroCiudadano = JwtTestHelper.generarTokenCiudadano(OTRO_CIUDADANO_USER_ID, OTRO_CIUDADANO_CUIL);
    }

    @Test
    void crearSolicitud_conJwtCiudadano_retorna201ConEstadoPendiente() throws Exception {
        Map<String, Object> request = Map.of("tipoCertificadoId", 1, "circunscripcionId", 1);

        mockMvc.perform(post("/api/solicitudes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtCiudadano)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE_REVISION"));
    }

    @Test
    void crearSolicitud_sinJwt_retorna403() throws Exception {
        Map<String, Object> request = Map.of("tipoCertificadoId", 1, "circunscripcionId", 1);

        mockMvc.perform(post("/api/solicitudes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void misSolicitudes_conJwtCiudadano_retorna200ConLista() throws Exception {
        Map<String, Object> createReq = Map.of("tipoCertificadoId", 1, "circunscripcionId", 1);
        mockMvc.perform(post("/api/solicitudes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtCiudadano)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/solicitudes/mis-solicitudes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtCiudadano))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].estado").value("PENDIENTE_REVISION"));
    }

    @Test
    void tomarSolicitud_conJwtInterno_retorna200() throws Exception {
        Map<String, Object> createReq = Map.of("tipoCertificadoId", 1, "circunscripcionId", 1);
        String createResponse = mockMvc.perform(post("/api/solicitudes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtCiudadano)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Integer solicitudId = objectMapper.readTree(createResponse).get("id").asInt();

        mockMvc.perform(post("/api/solicitudes/" + solicitudId + "/tomar")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtInterno))
                .andExpect(status().isOk());
    }

    @Test
    void obtenerPorId_ciudadanoPropietario_retorna200() throws Exception {
        Map<String, Object> createReq = Map.of("tipoCertificadoId", 1, "circunscripcionId", 1);
        String createResponse = mockMvc.perform(post("/api/solicitudes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtCiudadano)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Integer solicitudId = objectMapper.readTree(createResponse).get("id").asInt();

        mockMvc.perform(get("/api/solicitudes/" + solicitudId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtCiudadano))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(solicitudId));
    }

    @Test
    void obtenerPorId_ciudadanoNoPropietario_retorna403() throws Exception {
        Map<String, Object> createReq = Map.of("tipoCertificadoId", 1, "circunscripcionId", 1);
        String createResponse = mockMvc.perform(post("/api/solicitudes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtCiudadano)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Integer solicitudId = objectMapper.readTree(createResponse).get("id").asInt();

        mockMvc.perform(get("/api/solicitudes/" + solicitudId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtOtroCiudadano))
                .andExpect(status().isForbidden());
    }
}
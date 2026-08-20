package com.example.app;

import com.example.app.domain.ItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * The endpoint contract the deploy pipeline depends on.
 *
 * <p>These run without a real database: {@link ItemRepository} is mocked so the
 * full Spring context loads in CI without a DataSource. This mirrors the
 * {@code database=none} deployment shape and is also the state the health probe
 * sees on first pod startup before Flyway migrations complete.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class HealthEndpointsTest {

    @MockBean
    ItemRepository itemRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthReportsOkWithoutTouchingTheDatabase() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/health"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("ok"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.uptime_s").exists());
    }

    @Test
    void readyIsReadyWhenNoDatabaseIsConfigured() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/ready"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("ready"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.database").value("not configured"));
    }

    @Test
    void infoDescribesTheRunningService() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/info"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.service").value("udap-spring-boot-eks-api"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.database").value("none"));
    }

    @Test
    void echoReturnsTheQueryString() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/echo").param("any", "value"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.received.any").value("value"));
    }

    @Test
    void rootResolvesToTheWelcomePage() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.forwardedUrl("index.html"));
    }
}

package ma.locavite.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RedisCacheActuatorIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CacheManager cacheManager;

    @Test
    @DisplayName("Le CacheManager est configuré et contient les caches requis")
    void cacheManager_isConfiguredWithRequiredCaches() {
        assertThat(cacheManager).isNotNull();
        // Vérifie la présence des caches définis dans la spécification
        assertThat(cacheManager.getCacheNames()).contains("brands", "categories", "vehicleSearch", "vehicles");
    }

    @Test
    @DisplayName("GET /actuator/health : accessible et retourne un status")
    void actuatorHealth_isAccessible() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().is(anyOf(is(200), is(503))))
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    @DisplayName("GET /actuator/metrics : expose les métriques applicatives")
    void actuatorMetrics_isExposed() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").isArray());
    }
}

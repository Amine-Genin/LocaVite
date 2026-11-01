package ma.locavite.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageDeserializerTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Page.class, new PageDeserializer());
        mapper.registerModule(module);
    }

    @Test
    @DisplayName("Désérialise un flux JSON représentant une PageImpl complète")
    void deserialize_pageImplFromJson_succeeds() throws Exception {
        Page<String> originalPage = new PageImpl<>(List.of("item1", "item2"), PageRequest.of(0, 10), 2);
        String json = mapper.writeValueAsString(originalPage);

        Page<?> deserializedPage = mapper.readValue(json, Page.class);

        assertThat(deserializedPage).isNotNull();
        assertThat(deserializedPage.getTotalElements()).isEqualTo(2);
        assertThat(deserializedPage.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("Désérialise un JSON avec 'total' au lieu de 'totalElements'")
    void deserialize_withTotalProperty_succeeds() throws Exception {
        String json = "{\"content\":[\"a\"],\"number\":1,\"size\":5,\"total\":42}";
        Page<?> page = mapper.readValue(json, Page.class);

        assertThat(page).isNotNull();
        assertThat(page.getTotalElements()).isEqualTo(42);
        assertThat(page.getNumber()).isEqualTo(1);
        assertThat(page.getSize()).isEqualTo(5);
    }

    @Test
    @DisplayName("Désérialise un JSON sans content et sans pagination explicite")
    void deserialize_emptyJson_fallbacksToDefaults() throws Exception {
        String json = "{}";
        Page<?> page = mapper.readValue(json, Page.class);

        assertThat(page).isNotNull();
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getNumber()).isZero();
        assertThat(page.getSize()).isEqualTo(20);
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("Désérialise un JSON dont 'content' n'est pas un tableau")
    void deserialize_nonArrayContent_handlesGracefully() throws Exception {
        String json = "{\"content\":\"not-an-array\",\"size\":0}";
        Page<?> page = mapper.readValue(json, Page.class);

        assertThat(page).isNotNull();
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getSize()).isEqualTo(20);
    }
}

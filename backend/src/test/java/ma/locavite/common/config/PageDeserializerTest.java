package ma.locavite.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageDeserializerTest {

    @Test
    @DisplayName("Désérialise un flux JSON représentant une PageImpl")
    void deserialize_pageImplFromJson_succeeds() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Page.class, new PageDeserializer());
        mapper.registerModule(module);

        Page<String> originalPage = new PageImpl<>(List.of("item1", "item2"), PageRequest.of(0, 10), 2);
        String json = mapper.writeValueAsString(originalPage);

        Page<?> deserializedPage = mapper.readValue(json, Page.class);

        assertThat(deserializedPage).isNotNull();
        assertThat(deserializedPage.getTotalElements()).isEqualTo(2);
        assertThat(deserializedPage.getContent()).hasSize(2);
    }
}


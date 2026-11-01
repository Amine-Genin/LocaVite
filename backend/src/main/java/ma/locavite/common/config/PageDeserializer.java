package ma.locavite.common.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PageDeserializer extends JsonDeserializer<Page<?>> {

    @Override
    public Page<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode root = mapper.readTree(p);

        List<Object> content = new ArrayList<>();
        JsonNode contentNode = root.get("content");
        if (contentNode != null && contentNode.isArray()) {
            for (JsonNode item : contentNode) {
                content.add(mapper.treeToValue(item, Object.class));
            }
        }

        int number = root.has("number") ? root.get("number").asInt() : 0;
        int size = root.has("size") ? root.get("size").asInt() : (content.isEmpty() ? 20 : content.size());
        long totalElements = root.has("totalElements") ? root.get("totalElements").asLong() :
                (root.has("total") ? root.get("total").asLong() : content.size());

        return new PageImpl<>(content, PageRequest.of(number, size > 0 ? size : 20), totalElements);
    }
}


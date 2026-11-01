package ma.locavite.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class Md5KeyGeneratorTest {

    private final Md5KeyGenerator keyGenerator = new Md5KeyGenerator();

    @Test
    @DisplayName("Génère un hash MD5 hexadécimal de 32 caractères sans arguments")
    void generate_withoutParams_produces32HexCharString() throws NoSuchMethodException {
        Method method = Object.class.getMethod("toString");
        Object key = keyGenerator.generate(new Object(), method);

        assertThat(key).isInstanceOf(String.class);
        String hexKey = (String) key;
        assertThat(hexKey).matches("^[a-f0-9]{32}$");
    }

    @Test
    @DisplayName("Génère des clés identiques pour des paramètres identiques")
    void generate_sameParams_producesIdenticalKeys() throws NoSuchMethodException {
        Method method = Object.class.getMethod("equals", Object.class);

        Object key1 = keyGenerator.generate(new Object(), method, 42L);
        Object key2 = keyGenerator.generate(new Object(), method, 42L);

        assertThat(key1).isEqualTo(key2);
    }

    @Test
    @DisplayName("Génère des clés distinctes pour des paramètres différents")
    void generate_differentParams_producesDistinctKeys() throws NoSuchMethodException {
        Method method = Object.class.getMethod("equals", Object.class);

        Object key1 = keyGenerator.generate(new Object(), method, 42L);
        Object key2 = keyGenerator.generate(new Object(), method, 99L);

        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    @DisplayName("Prend en compte les attributs de pagination dans le calcul du hash MD5")
    void generate_withPageable_takesPaginationAttributesIntoAccount() throws NoSuchMethodException {
        Method method = Object.class.getMethod("equals", Object.class);

        var page1 = PageRequest.of(0, 10, Sort.by("name").ascending());
        var page2 = PageRequest.of(1, 10, Sort.by("name").ascending());

        Object key1 = keyGenerator.generate(new Object(), method, page1);
        Object key2 = keyGenerator.generate(new Object(), method, page2);

        assertThat(key1).isNotEqualTo(key2);
        assertThat(key1.toString()).matches("^[a-f0-9]{32}$");
        assertThat(key2.toString()).matches("^[a-f0-9]{32}$");
    }
}


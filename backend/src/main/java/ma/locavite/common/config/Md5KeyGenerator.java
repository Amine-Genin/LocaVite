package ma.locavite.common.config;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.data.domain.Pageable;
import org.springframework.util.DigestUtils;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

/**
 * Générateur de clé de cache basé sur le hash MD5 des paramètres de méthode.
 * MD5 est employé ici comme fonction de dispersion rapide et non à des fins cryptographiques.
 */
@SuppressWarnings({"java:S4790", "squid:S4790"})
public class Md5KeyGenerator implements KeyGenerator {

    @Override
    @SuppressWarnings({"java:S4790", "squid:S4790"})
    public Object generate(Object target, Method method, Object... params) {
        if (params == null || params.length == 0) {
            return DigestUtils.md5DigestAsHex(method.getName().getBytes(StandardCharsets.UTF_8));
        }

        StringBuilder sb = new StringBuilder();
        for (Object param : params) {
            if (param == null) {
                sb.append("null;");
            } else if (param instanceof Pageable pageable) {
                sb.append("page:").append(pageable.getPageNumber())
                  .append(",size:").append(pageable.getPageSize())
                  .append(",sort:").append(pageable.getSort())
                  .append(";");
            } else {
                sb.append(param.toString()).append(";");
            }
        }
        return DigestUtils.md5DigestAsHex(sb.toString().getBytes(StandardCharsets.UTF_8));
    }
}

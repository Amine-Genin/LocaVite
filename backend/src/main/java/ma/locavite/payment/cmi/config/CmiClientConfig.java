package ma.locavite.payment.cmi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class CmiClientConfig {

    @Bean
    public RestClient cmiRestClient(CmiProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));

        return RestClient.builder()
                .baseUrl(properties.getGatewayUrl())
                .requestFactory(factory)
                .build();
    }
}

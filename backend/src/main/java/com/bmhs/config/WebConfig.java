package com.bmhs.config;

import com.bmhs.auth.CurrentUserArgumentResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final List<String> allowedOrigins;
    private final CurrentUserArgumentResolver currentUserArgumentResolver;

    public WebConfig(@Value("${bm-hs.allowed-origin:http://127.0.0.1:8000,http://localhost:8000}") String allowedOrigin,
                     CurrentUserArgumentResolver currentUserArgumentResolver) {
        this.allowedOrigins = parseAllowedOrigins(allowedOrigin);
        this.currentUserArgumentResolver = currentUserArgumentResolver;
    }

    static List<String> parseAllowedOrigins(String configuredOrigins) {
        List<String> origins = Arrays.stream(configuredOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .distinct()
                .toList();
        if (origins.isEmpty()) {
            throw new IllegalArgumentException("CORS must use one or more explicit origins");
        }
        origins.forEach(WebConfig::validateExactOrigin);
        return origins;
    }

    private static void validateExactOrigin(String origin) {
        URI uri;
        try {
            uri = URI.create(origin);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("CORS origin must be a valid exact HTTP(S) origin", exception);
        }

        String path = uri.getRawPath();
        if (origin.contains("*")
                || uri.getScheme() == null
                || !(uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https"))
                || uri.getHost() == null
                || uri.getRawUserInfo() != null
                || (path != null && !path.isEmpty())
                || uri.getRawQuery() != null
                || uri.getRawFragment() != null) {
            throw new IllegalArgumentException("CORS origin must be a valid exact HTTP(S) origin without path or wildcard");
        }

        if (!origin.equals(canonicalOrigin(uri))) {
            throw new IllegalArgumentException("CORS origin must use its canonical browser representation");
        }
    }

    private static String canonicalOrigin(URI uri) {
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        int port = uri.getPort();
        if ((scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443)) {
            port = -1;
        }

        String authorityHost = host.contains(":") && !host.startsWith("[") ? "[" + host + "]" : host;
        return scheme + "://" + authorityHost + (port < 0 ? "" : ":" + port);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "OPTIONS")
                .allowedHeaders("Content-Type", "Idempotency-Key")
                .allowCredentials(true);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }
}

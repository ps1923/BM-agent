package com.bmhs.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WebConfigTest {
    @Test
    void parsesAndDeduplicatesExplicitCorsOrigins() {
        assertEquals(List.of("http://127.0.0.1:8000", "http://localhost:8000"),
                WebConfig.parseAllowedOrigins(" http://127.0.0.1:8000, http://localhost:8000, http://localhost:8000 "));
    }

    @Test
    void rejectsWildcardAndEmptyCorsOrigins() {
        assertThrows(IllegalArgumentException.class, () -> WebConfig.parseAllowedOrigins("*"));
        assertThrows(IllegalArgumentException.class, () -> WebConfig.parseAllowedOrigins("https://*.example.edu"));
        assertThrows(IllegalArgumentException.class, () -> WebConfig.parseAllowedOrigins(" , "));
    }

    @Test
    void rejectsNonOriginUrlComponents() {
        assertThrows(IllegalArgumentException.class,
                () -> WebConfig.parseAllowedOrigins("https://student.example.edu/portal"));
        assertThrows(IllegalArgumentException.class,
                () -> WebConfig.parseAllowedOrigins("https://student.example.edu?mode=dev"));
        assertThrows(IllegalArgumentException.class,
                () -> WebConfig.parseAllowedOrigins("ftp://student.example.edu"));
        assertThrows(IllegalArgumentException.class,
                () -> WebConfig.parseAllowedOrigins("https://user:pass@student.example.edu"));
    }

    @Test
    void rejectsOriginsThatBrowsersWouldSerializeDifferently() {
        assertThrows(IllegalArgumentException.class,
                () -> WebConfig.parseAllowedOrigins("HTTPS://student.example.edu"));
        assertThrows(IllegalArgumentException.class,
                () -> WebConfig.parseAllowedOrigins("https://Student.example.edu"));
        assertThrows(IllegalArgumentException.class,
                () -> WebConfig.parseAllowedOrigins("https://student.example.edu:443"));
        assertThrows(IllegalArgumentException.class,
                () -> WebConfig.parseAllowedOrigins("http://student.example.edu:80"));
    }

    @Test
    void retainsCanonicalNonDefaultPorts() {
        assertEquals(List.of("https://student.example.edu:8443"),
                WebConfig.parseAllowedOrigins("https://student.example.edu:8443"));
    }
}

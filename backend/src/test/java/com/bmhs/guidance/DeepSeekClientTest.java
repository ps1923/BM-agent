package com.bmhs.guidance;

import com.bmhs.experimentcreation.ApiException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeepSeekClientTest {
    @Test
    void refusesToCallWhenModelSecretIsNotConfigured() {
        DeepSeekClient client = new DeepSeekClient("", "", "deepseek-chat", Duration.ofSeconds(2));

        ApiException exception = assertThrows(ApiException.class,
                () -> client.complete("system", "user"));

        assertEquals("MODEL_NOT_CONFIGURED", exception.code());
    }

    @Test
    void rejectsEndpointsContainingUserInfo() {
        DeepSeekClient client = new DeepSeekClient("https://user:secret@example.com/v1", "key",
                "deepseek-chat", Duration.ofSeconds(2));

        ApiException exception = assertThrows(ApiException.class,
                () -> client.complete("system", "user"));

        assertEquals("MODEL_NOT_CONFIGURED", exception.code());
    }

    @Test
    void rejectsPlainHttpForRemoteModelEndpoints() {
        DeepSeekClient client = new DeepSeekClient("http://api.example.com/v1", "key",
                "deepseek-chat", Duration.ofSeconds(2));

        ApiException exception = assertThrows(ApiException.class,
                () -> client.complete("system", "user"));

        assertEquals("MODEL_NOT_CONFIGURED", exception.code());
    }
}

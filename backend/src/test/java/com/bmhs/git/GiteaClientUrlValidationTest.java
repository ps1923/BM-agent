package com.bmhs.git;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GiteaClientUrlValidationTest {
    @Test
    void acceptsHttpAndHttpsPublicUrls() {
        assertTrue(GiteaClient.isAllowedPublicUrl("http://git.example.test"));
        assertTrue(GiteaClient.isAllowedPublicUrl("https://git.example.test/gitea"));
    }

    @Test
    void rejectsUnsupportedSchemesAndMalformedUrls() {
        assertFalse(GiteaClient.isAllowedPublicUrl("ftp://git.example.test"));
        assertFalse(GiteaClient.isAllowedPublicUrl("file:///tmp/gitea"));
        assertFalse(GiteaClient.isAllowedPublicUrl("git.example.test"));
        assertFalse(GiteaClient.isAllowedPublicUrl("http://[malformed"));
        assertFalse(GiteaClient.isAllowedPublicUrl(""));
        assertFalse(GiteaClient.isAllowedPublicUrl(null));
    }

    @Test
    void rejectsEmbeddedUserInfoForHttpAndHttps() {
        assertFalse(GiteaClient.isAllowedPublicUrl("http://student:token@git.example.test"));
        assertFalse(GiteaClient.isAllowedPublicUrl("https://student:token@git.example.test"));
    }
}

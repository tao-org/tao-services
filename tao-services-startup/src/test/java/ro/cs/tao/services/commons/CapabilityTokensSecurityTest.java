package ro.cs.tao.services.commons;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class CapabilityTokensSecurityTest {
    @Test public void acceptsOnlyUnmodifiedPurposeBoundCapabilities() {
        String token = CapabilityTokens.issue("download", "{\"user\":\"alice\"}", Duration.ofMinutes(15));
        assertEquals("{\"user\":\"alice\"}", CapabilityTokens.verify("download", token));
        assertThrows(IllegalArgumentException.class, () -> CapabilityTokens.verify("activation", token));
        String[] parts = token.split("\\.");
        String forged = parts[0] + "." + parts[1] + ".e30." + parts[3];
        assertThrows(IllegalArgumentException.class, () -> CapabilityTokens.verify("download", forged));
    }

    @Test public void rejectsPlaintextMalformedLegacyAndExpiredTokens() {
        for (String invalid : new String[]{"{\"user\":\"victim\"}", "YWJjZA==", "", "download.1.e30.bad"}) {
            assertThrows(IllegalArgumentException.class, () -> CapabilityTokens.verify("download", invalid));
        }
        String token = CapabilityTokens.issue("download", "resource", Duration.ofMinutes(15));
        assertThrows(IllegalArgumentException.class, () -> CapabilityTokens.verify("download", token, Instant.now().plus(Duration.ofHours(1))));
    }
}

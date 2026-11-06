package ro.cs.tao.services.commons;

import ro.cs.tao.configuration.ConfigurationManager;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/** Purpose-bound expiring capabilities. Legacy encrypted tokens are intentionally not accepted. */
public final class CapabilityTokens {
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private static final byte[] KEY = key();

    private CapabilityTokens() { }

    public static String issue(String purpose, String payload, Duration lifetime) {
        if (lifetime.isNegative() || lifetime.isZero() || lifetime.compareTo(Duration.ofDays(2)) > 0) {
            throw new IllegalArgumentException("Invalid capability lifetime");
        }
        String body = purpose + "." + Instant.now().plus(lifetime).getEpochSecond() + "."
                + ENCODER.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return body + "." + ENCODER.encodeToString(sign(body));
    }

    public static String verify(String purpose, String token) {
        return verify(purpose, token, Instant.now());
    }

    static String verify(String purpose, String token, Instant now) {
        try {
            if (token == null || token.length() > 16384) throw new IllegalArgumentException();
            String[] parts = token.split("\\.", -1);
            if (parts.length != 4 || !purpose.equals(parts[0])) throw new IllegalArgumentException();
            String body = parts[0] + "." + parts[1] + "." + parts[2];
            if (!MessageDigest.isEqual(sign(body), DECODER.decode(parts[3]))) throw new IllegalArgumentException();
            long expires = Long.parseLong(parts[1]);
            if (expires <= now.getEpochSecond()) throw new IllegalArgumentException();
            return new String(DECODER.decode(parts[2]), StandardCharsets.UTF_8);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid or expired capability");
        }
    }

    private static byte[] sign(String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(KEY, "HmacSHA256"));
            return mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] key() {
        String configured = ConfigurationManager.getInstance().getValue("security.capability.secret");
        if (configured != null && !configured.isBlank()) {
            if (configured.getBytes(StandardCharsets.UTF_8).length < 32) {
                throw new IllegalStateException("security.capability.secret must contain at least 32 bytes");
            }
            return configured.getBytes(StandardCharsets.UTF_8);
        }
        // Safe single-process default: restart revokes all outstanding links.
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        return random;
    }
}

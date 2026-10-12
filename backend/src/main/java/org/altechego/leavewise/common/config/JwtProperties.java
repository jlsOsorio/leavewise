package org.altechego.leavewise.common.config;

import java.time.Duration;
import java.util.Base64;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("leavewise.jwt")
public record JwtProperties(
        @NotBlank String secret,
        @NotNull Duration accessTtl,
        @NotNull Duration refreshTtl) {

    // HS256 needs a key at least as long as its 256-bit output
    private static final int MIN_SECRET_BYTES = 32;


    public byte[] secretBytes() {
        return Base64.getDecoder().decode(secret);
    }

    @AssertTrue(message = "must be base64 and decode to at least " + MIN_SECRET_BYTES + " bytes")
    public boolean isSecretStrongEnough() {
        if (secret == null) {
            return true; // reported by @NotBlank
        }
        try {
            return secretBytes().length >= MIN_SECRET_BYTES;
        } catch (IllegalArgumentException _) {
            return false;
        }
    }
}

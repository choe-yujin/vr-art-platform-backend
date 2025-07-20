package com.bauhaus.livingbrushbackendapi.auth.dto;

import com.bauhaus.livingbrushbackendapi.user.entity.enumeration.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Meta (Oculus) login request DTO (Refactoring v2.2)
 *
 * Defined as a Java record for an immutable and concise data object,
 * and implements the OAuthLoginRequest interface to support polymorphism.
 *
 * @author Bauhaus Team
 * @version 2.2
 */
public record MetaLoginRequest(
        @NotBlank(message = "Meta Access Token is required.")
        String metaAccessToken,

        @NotNull(message = "Platform information is required.")
        Platform platform
) implements OAuthLoginRequest {

        /**
         * Explicitly implements the getPlatform() method from the OAuthLoginRequest interface.
         * Returns the value of the record's 'platform' component to adhere to the interface contract.
         */
        @Override
        public Platform getPlatform() {
                return this.platform;
        }
}
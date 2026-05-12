package com.snakeforged.domain;

/**
 * Raised when {@link DifficultyConfig#fromName(String)} cannot resolve a label.
 * The API maps this to localized text via Spring {@code MessageSource}.
 */
public final class DifficultyResolutionException extends RuntimeException {

    private final String requestedName;

    public DifficultyResolutionException(String requestedName) {
        this.requestedName = requestedName != null ? requestedName : "";
    }

    /** Raw difficulty string supplied by the client (may be empty). */
    public String getRequestedName() {
        return requestedName;
    }
}

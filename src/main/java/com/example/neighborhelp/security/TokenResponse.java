package com.example.neighborhelp.security;

public class TokenResponse {
    private String accessToken;
    private String refreshToken; // Add refresh token
    private String tokenType;
    private long expiresIn;

    // Constructor
    public TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken; // Initialize refresh token
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
    }

    // Getters
    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken; // Getter for refresh token
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    // Builder pattern for easier instantiation
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String accessToken;
        private String refreshToken; // Add refresh token to builder
        private String tokenType;
        private long expiresIn;

        public Builder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public Builder refreshToken(String refreshToken) { // Builder method for refresh token
            this.refreshToken = refreshToken;
            return this;
        }

        public Builder tokenType(String tokenType) {
            this.tokenType = tokenType;
            return this;
        }

        public Builder expiresIn(long expiresIn) {
            this.expiresIn = expiresIn;
            return this;
        }

        public TokenResponse build() {
            return new TokenResponse(accessToken, refreshToken, tokenType, expiresIn);
        }
    }
}

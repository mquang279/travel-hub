package edu.uet.travel_hub.domain.enums;

public enum Role {
    ADMIN("ROLE_ADMIN"),
    USER("ROLE_USER");

    private String description;

    private Role(String description) {
        this.description = description;
    }

    public String getDescription() {
        return this.description;
    }
}

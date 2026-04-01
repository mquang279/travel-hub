package edu.uet.travel_hub.domain.model;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class UserModel {
    private final Long id;
    private final String email;
    private final String username;
    private final String password;

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Long getId() {
        return id;
    }
}
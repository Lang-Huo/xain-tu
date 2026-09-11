package com.xiantu.web.dto;

import com.xiantu.entity.User;
import lombok.Data;

@Data
public class AuthResponse {

    private String token;
    private UserProfile user;

    public static AuthResponse of(String token, User user) {
        AuthResponse r = new AuthResponse();
        r.setToken(token);
        r.setUser(UserProfile.of(user));
        return r;
    }
}

package com.pachy.highlight.dto.admin;

import com.pachy.highlight.dto.response.Response;

import lombok.Getter;
import lombok.Setter;

import org.springframework.http.HttpStatus;

@Getter
@Setter
public class LoginResponse extends Response {
    private String token;
    private String username;
    private String role;

    public LoginResponse(String token, String username, String role) {
        super(HttpStatus.OK);
        this.token = token;
        this.username = username;
        this.role = role;
    }
}

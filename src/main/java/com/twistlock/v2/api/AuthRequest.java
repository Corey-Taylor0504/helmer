package com.twistlock.v2.api;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthRequest {

    private final String username;
    private final String password;

}

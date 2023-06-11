package com.twistlock.v2.api;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Token {

    private final String token;
    private final long timestamp;

}

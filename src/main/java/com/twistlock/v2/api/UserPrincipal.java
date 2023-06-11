package com.twistlock.v2.api;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserPrincipal implements Serializable {

	private String username;
	private String password;
	private String role;
}
	
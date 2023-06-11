package com.twistlock.v2.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@Getter
@Setter
public class EmbeddedConfig {

	@Value("${twistlock.token.cache.ttl.seconds}")
	private long ttl;
	@Value("${auth.renew.enabled}")
	private boolean renewEnabled;
	@Value(("${twistlock.projects}"))
	private String projects;
	@Value("${mongodb.enabled}")
	private boolean mongoDBEnabled;
	@Value("${twistlock.version}")
	private String twistLockVersion;
	@Value("${saml.enabled}")
	private boolean samlEnabled;
}

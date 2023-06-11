package com.twistlock.v2.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsFilter implements Filter {

	@Value("${allow.origin}")
	private String allowOrigins;

	@Value("${allow.headers}")
	private String allowHeaders;

	@Value("${allow.credentials}")
	private String allowCredentials;

	@Value("${allow.methods}")
	private String allowMethods;

	@Value("${allow.maxage}")
	private String allowMaxAge;

	@Override
	public void init(FilterConfig filterConfig) {

	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {

		HttpServletResponse response = (HttpServletResponse) res;
		HttpServletRequest request = (HttpServletRequest) req;
		response.setHeader("Access-Control-Allow-Origin", allowOrigins);
		response.setHeader("Access-Control-Allow-Credentials", allowCredentials);
		response.setHeader("Access-Control-Allow-Methods", allowMethods);
		response.setHeader("Access-Control-Max-Age", allowMaxAge);
		response.setHeader("Access-Control-Allow-Headers", allowHeaders);

		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			response.setStatus(HttpServletResponse.SC_OK);
		} else {
			chain.doFilter(req, res);
		}
	}

	@Override
	public void destroy() {

	}

}

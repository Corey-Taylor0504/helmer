package com.twistlock.v2.security;

import com.twistlock.v2.config.EnvironmentConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.authentication.www.BasicAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

@Component
public class RestFilter extends OncePerRequestFilter {
    private final List<String> matchList = new ArrayList<>();
    private EnvironmentConfig environmentConfig;
    private BasicAuthenticationConverter authenticationConverter = new BasicAuthenticationConverter();
    private BiPredicate<List<String>, HttpServletRequest> matchRequest = (matchList, request) -> matchList
            .stream().anyMatch(matcher -> request.getRequestURI().endsWith(matcher));

    @Autowired
    public RestFilter(EnvironmentConfig environmentConfig) {
        this.environmentConfig = environmentConfig;
        matchList();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        try {
            if (matchRequest.test(matchList, request)) {
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = authenticationConverter.convert(request);
                if (usernamePasswordAuthenticationToken == null) {
                    requestAttributes.setAttribute("exception", new RuntimeException("Authorization header not found"), WebRequest.SCOPE_REQUEST);
                } else if (!environmentConfig.getApiUserName().equals(usernamePasswordAuthenticationToken.getPrincipal()) || !environmentConfig.getApiPassword().equals(usernamePasswordAuthenticationToken.getCredentials())) {
                    requestAttributes.setAttribute("exception", new BadCredentialsException("Invalid Credentials provided"), WebRequest.SCOPE_REQUEST);
                }
            }
        } catch (Exception e) {
            requestAttributes.setAttribute("exception", new RuntimeException(e.getMessage()), WebRequest.SCOPE_REQUEST);
        }
        filterChain.doFilter(request, response);
    }

    private void matchList() {
        matchList.add("/packages");
        matchList.add("/generatePackage");
    }
}
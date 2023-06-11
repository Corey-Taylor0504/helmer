package com.twistlock.v2.security;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.session.SessionManagementFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CustomSessionManagementFilter extends SessionManagementFilter {

    static final String FILTER_APPLIED = "__spring_security_session_mgmt_filter_applied";
    private static final Logger logger = Logger.getLogger(CustomSessionManagementFilter.class);
    private final SecurityContextRepository securityContextRepository;
    private final AuthenticationTrustResolver authenticationTrustResolver = new AuthenticationTrustResolverImpl();
    private SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private AuthenticationFailureHandler failureHandler = new SimpleUrlAuthenticationFailureHandler();

    public CustomSessionManagementFilter(SecurityContextRepository securityContextRepository) {
        super(securityContextRepository);
        this.securityContextRepository = securityContextRepository;
    }

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        if (request.getAttribute(FILTER_APPLIED) != null) {
            chain.doFilter(request, response);
        } else {
            request.setAttribute(FILTER_APPLIED, Boolean.TRUE);
            if (!securityContextRepository.containsContext(request) && request.getHeader("Authorization") == null) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && !authenticationTrustResolver.isAnonymous(authentication) && sessionAuthenticationStrategy != null) {
                    try {
                        sessionAuthenticationStrategy.onAuthentication(authentication, request, response);
                    } catch (SessionAuthenticationException e) {
                        logger.debug("SessionAuthenticationStrategy rejected the authentication object" + e.getMessage());
                        SecurityContextHolder.clearContext();
                        failureHandler.onAuthenticationFailure(request, response, e);
                        return;
                    }
                    securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);
                }
            }
            String contextValue = request.getContextPath();
            if (StringUtils.isNotEmpty(contextValue) && !"/".equalsIgnoreCase(contextValue)) {
                contextValue = String.join("", contextValue, "/");
            }
            String requestURI = ((HttpServletRequest) req).getRequestURI().replaceAll(contextValue, "");
            if (isMemberAlreadyLoggedIn(request)) {
                if ("".equalsIgnoreCase(requestURI) || "/".equalsIgnoreCase(requestURI)) {
                    String redirectUrl = "/cloud";
                    RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
                    try {
                        logger.debug("Redirect URL :: " + redirectUrl);
                        redirectStrategy.sendRedirect(request, response, redirectUrl);
                    } catch (IOException e) {
                        logger.error("IO Exception while trying to redirect to redirectURL");
                    }
                    return;
                }
                if (requestURI.endsWith("logout")) {
                    String redirectUrl = "/index";
                    RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
                    logger.debug("Redirect URL :: " + redirectUrl);
                    redirectStrategy.sendRedirect(request, response, redirectUrl);
                    return;
                }
            }
            if (requestURI != null && !requestURI.trim().equalsIgnoreCase("") && !requestURI.equalsIgnoreCase("index")
                    && !requestURI.equalsIgnoreCase("logout") && !isMemberAlreadyLoggedIn(request)) {
                request.setAttribute("loginFlag", "Session Expired");
            }
            chain.doFilter(request, response);
        }
    }

    private boolean isMemberAlreadyLoggedIn(HttpServletRequest request) {

        boolean isMemberAlreadyLoggedIn = false;
        if (request.getSession().getAttribute("userPrincipal") != null) {
            isMemberAlreadyLoggedIn = true;
        }
        return isMemberAlreadyLoggedIn;
    }
}

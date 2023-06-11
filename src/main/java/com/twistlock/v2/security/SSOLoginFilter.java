package com.twistlock.v2.security;

import com.twistlock.v2.api.UserPrincipal;
import com.twistlock.v2.config.EnvironmentConfig;
import org.apache.log4j.Logger;
import org.opensaml.saml2.core.NameID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

public class SSOLoginFilter extends GenericFilterBean {

    private static final Logger LOGGER = Logger.getLogger(SSOLoginFilter.class);

    private boolean samlEnabled;
    private EnvironmentConfig environmentConfig;

    public SSOLoginFilter(boolean samlEnabled, EnvironmentConfig environmentConfig) {
        this.samlEnabled = samlEnabled;
        this.environmentConfig = environmentConfig;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getCredentials() instanceof SAMLCredential && samlEnabled) {
            SAMLCredential samlCredential = (SAMLCredential) authentication.getCredentials();
            NameID nameID = samlCredential.getNameID();
            LOGGER.debug("SAML Authentication user name:" + nameID.getValue());
            String[] roleArray = environmentConfig.getAdminUsers().contains(nameID.getValue()) ? new String[]{"ADMIN_USER", "ROLE_USER"} : new String[]{"ROLE_USER"};
            List<GrantedAuthority> grantedAuths = AuthorityUtils.createAuthorityList(roleArray);
            UserDetails userDetails = new User(nameID.getValue(), "SAML_SSO_LOGIN", grantedAuths);
            UserPrincipal principal = new UserPrincipal();
            principal.setUsername(nameID.getValue());
            String role = environmentConfig.getAdminUsers().contains(nameID.getValue()) ? "Admin" : "CIUser";
            principal.setRole(role);
            principal.setPassword("SAML_SSO_LOGIN");
            if (request.getSession().getAttribute("userPrincipal") == null) {
                request.getSession().setAttribute("userPrincipal", principal);
            }
            Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, "SAML_SSO_LOGIN", grantedAuths);
            SecurityContextHolder.getContext().setAuthentication(auth);
        } else if (!samlEnabled) {
            if (request.getSession().getAttribute("userPrincipal") == null) {
                UserPrincipal principal = new UserPrincipal();
                principal.setUsername("DUMMY_USER");
                principal.setRole("CIUser");
                principal.setPassword("SAML_SSO_LOGIN");
                request.getSession().setAttribute("userPrincipal", principal);
                List<GrantedAuthority> grantedAuths = AuthorityUtils.createAuthorityList("ROLE_USER");
                UserDetails userDetails = new User("DUMMY_USER", "SAML_SSO_LOGIN", grantedAuths);
                Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, "SAML_SSO_LOGIN", grantedAuths);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                UserPrincipal userPrincipal = (UserPrincipal) request.getSession().getAttribute("userPrincipal");
                List<GrantedAuthority> grantedAuths = AuthorityUtils.createAuthorityList("ROLE_USER");
                UserDetails userDetails = new User(userPrincipal.getUsername(), "SAML_SSO_LOGIN", grantedAuths);
                Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, "SAML_SSO_LOGIN", grantedAuths);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        filterChain.doFilter(request, response);
    }
}
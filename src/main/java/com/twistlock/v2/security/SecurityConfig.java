package com.twistlock.v2.security;

import com.twistlock.v2.config.EnvironmentConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.session.SessionManagementFilter;

import static org.springframework.security.extensions.saml2.config.SAMLConfigurer.saml;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("${security.saml2.metadata-url}")
    String metadataUrl;
    @Value("${server.ssl.key-alias}")
    String keyAlias;
    @Value("${server.ssl.key-store-password}")
    String password;
    @Value("${server.ssl.key-store}")
    String keyStoreFilePath;
    @Value("${saml.sso.auth.host}")
    String hostName;
    @Value("${saml.sso.protocol}")
    String protocol;
    @Value("${security.invalidsessionurl}")
    private String invalidSessionURL;
    @Value("${server.servlet.context-path:/}")
    private String contextPath;
    @Value("${saml.enabled}")
    private boolean samlEnabled;
    @Autowired
    private EnvironmentConfig environmentConfig;

    @Bean
    public HttpFirewall defaultFirewall() {
        DefaultHttpFirewall fireWall = new DefaultHttpFirewall();
        fireWall.setAllowUrlEncodedSlash(true);
        return fireWall;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.headers().xssProtection();
        http.headers().cacheControl();
        http.headers().httpStrictTransportSecurity();
        http.headers().frameOptions();
        http.headers().contentSecurityPolicy(
                "default-src *; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline' 'unsafe-eval'");
        http.httpBasic().authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/index"));
        http.authorizeRequests().antMatchers("/").access("hasAnyRole('ROLE_ANONYMOUS','ROLE_USER')");
        http.authorizeRequests().antMatchers("/**").access("hasRole('ROLE_USER')");
        http.authorizeRequests().antMatchers("/saml*").hasAnyRole("ROLE_USER").anyRequest().access("hasRole('ROLE_USER')").and().apply(saml())
                .serviceProvider().keyStore().storeFilePath(this.keyStoreFilePath).password(this.password)
                .keyname(this.keyAlias).keyPassword(this.password).and().protocol(protocol)
                .hostname(String.format("%s", hostName)).basePath(contextPath).and().identityProvider()
                .metadataFilePath(this.metadataUrl);
        http.addFilterBefore(new CustomSessionManagementFilter(new HttpSessionSecurityContextRepository()), SessionManagementFilter.class);
        http.addFilterBefore(new SSOLoginFilter(samlEnabled, environmentConfig), UsernamePasswordAuthenticationFilter.class);
        http.logout().logoutUrl("logout");
        http.sessionManagement().invalidSessionUrl(invalidSessionURL);
        http.csrf().disable();
    }

    @Override
    public void configure(WebSecurity http) {
        http.httpFirewall(defaultFirewall());
        http.ignoring().antMatchers("/*.html").antMatchers("/error/**").antMatchers("/index/**").antMatchers("/api/v2/rest/**").antMatchers("/css/**")
                .antMatchers("/img/**").antMatchers("/font-awesome/**").antMatchers("/webfonts/**")
                .antMatchers("/image/**").antMatchers("/js/**").antMatchers("/images/**").antMatchers("/fonts/**")
                .antMatchers("/loginTheme/**").antMatchers("/favicon.ico")
                .mvcMatchers("/swagger-ui/**", "/configuration/**", "/swagger-resources/**", "/v3/api-docs/**");
    }
}
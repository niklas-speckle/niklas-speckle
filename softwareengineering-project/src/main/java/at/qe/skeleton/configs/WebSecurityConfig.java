package at.qe.skeleton.configs;

import javax.sql.DataSource;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Spring configuration for web security.
 * <p>
 * This class is part of the skeleton project provided for students of the
 * course "Software Engineering" offered by Innsbruck University.
 */
@Configuration
@EnableWebSecurity
@EnableJpaAuditing
@EnableScheduling
public class WebSecurityConfig {


    private static final String LOGIN = "/login.xhtml";
    private static final String EMPLOYEE = "EMPLOYEE";
    private static final String MANAGER = "MANAGER";
    private static final String GROUP_LEADER = "GROUP_LEADER";
    private static final String ADMINISTRATOR = "ADMINISTRATOR";

    @Autowired
    DataSource dataSource;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        try {

            http
                    .cors(cors -> cors.disable())
                    .csrf(csrf -> csrf.disable())
                    .headers(headers -> headers.frameOptions(FrameOptionsConfig::sameOrigin)) // needed for H2 console
                    .authorizeHttpRequests(authorize -> authorize
                            .requestMatchers(new AntPathRequestMatcher("/api/**")).permitAll()
                            .requestMatchers(new AntPathRequestMatcher("/")).permitAll()
                            .requestMatchers(new AntPathRequestMatcher("/**.jsf")).permitAll()
                            .requestMatchers(new AntPathRequestMatcher("/resources/**")).permitAll()
                            .requestMatchers(new AntPathRequestMatcher("/jakarta.faces.resource/**")).permitAll()
                            .requestMatchers(new AntPathRequestMatcher("/error/**")).permitAll()
                            .requestMatchers(new AntPathRequestMatcher("/secured/**")).hasAnyAuthority(MANAGER, GROUP_LEADER, ADMINISTRATOR, EMPLOYEE)
                            .requestMatchers(new AntPathRequestMatcher("/admin/**")).hasAnyAuthority(ADMINISTRATOR)
                            .requestMatchers(new AntPathRequestMatcher("/projects/**")).hasAnyAuthority(MANAGER, GROUP_LEADER, ADMINISTRATOR)
                            .requestMatchers(new AntPathRequestMatcher("/workgroups/**")).hasAnyAuthority(MANAGER, GROUP_LEADER, ADMINISTRATOR)


                            .anyRequest().authenticated()
                    )
                    .formLogin(form -> form
                            .loginPage(LOGIN)
                            .permitAll()
                            .defaultSuccessUrl("/secured/welcome.xhtml")
                            .loginProcessingUrl("/login")
                            .successForwardUrl("/secured/welcome.xhtml")
                            .failureUrl("/login.xhtml?error=true")
                    )
                    .logout(logout -> logout
                            .logoutSuccessUrl(LOGIN)
                            .deleteCookies("JSESSIONID")
                            .invalidateHttpSession(true)
                            .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                    )
                    .sessionManagement(session -> session
                            .invalidSessionUrl("/error/invalid_session.xhtml")
                    );

            return http.build();
        } catch (Exception ex) {
            throw new BeanCreationException("Wrong spring security configuration", ex);
        }
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        // Configure roles and passwords via datasource
        auth.jdbcAuthentication().dataSource(dataSource)
                .usersByUsernameQuery("select username, password, enabled from userx where username=?")
                .authoritiesByUsernameQuery("select userx_id, roles from userx_userx_role where userx_id=(select id from userx where username=?)")
                .passwordEncoder(passwordEncoder());
    }

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
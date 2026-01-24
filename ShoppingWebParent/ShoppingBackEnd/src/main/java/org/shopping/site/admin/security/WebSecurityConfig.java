package org.shopping.site.admin.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, DaoAuthenticationProvider authProvider) throws Exception {
        http.authenticationProvider(authProvider);

        http.authorizeHttpRequests(auth -> auth
                        // PUBLIC PAGES (no login needed)
                        .requestMatchers("/login", "/register", "/reviews/public").permitAll()

                        // CUSTOMER-SUBMIT ENDPOINT (secured by controller logic)
                        .requestMatchers("/reviews/submit").authenticated() // ← must be logged in

                        // ADMIN-ONLY: manage reviews (edit/delete)
                        .requestMatchers("/reviews/**").hasAuthority("Admin")

                        // Admin + Editor: users, categories, etc.
                        .requestMatchers("/users/**", "/categories/**").hasAnyAuthority("Admin", "Editor")

                        // Everything else: any logged-in user (Customer, Salesperson, etc.)
                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login")
                        .permitAll()
                )
                .rememberMe(rm -> rm
                        .key("shopme-admin-secret-key")
                        .tokenValiditySeconds(86400)
                );

        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring()
                .requestMatchers(
                        "/images/**",
                        "/js/**",
                        "/css/**",
                        "/webjars/**",
                        "/style.css"
                );
    }
}
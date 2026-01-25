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
                        // === PUBLIC (no login needed) ===
                        .requestMatchers("/login", "/register").permitAll()

                        // === CREATE/EDIT/DELETE — RESTRICTED ===
                        .requestMatchers("/products/new", "/products/save", "/brands/new", "/brands/save")
                        .hasAnyAuthority("Admin", "Editor")

                        .requestMatchers("/products/edit/**", "/products/delete/**")
                        .hasAnyAuthority("Admin", "Editor", "Salesperson") // ← allow Salesperson to edit?

                        .requestMatchers("/brands/edit/**", "/brands/delete/**", "/brands/**")
                        .hasAnyAuthority("Admin", "Editor")

                        // === CUSTOMER-ONLY PAGES (logged-in Customers + others can view) ===
                        .requestMatchers("/products/**", "/reviews/public").authenticated()

                        // === ADMIN-ONLY: manage reviews (edit/delete) ===
                        .requestMatchers("/reviews/**").hasAuthority("Admin")

                        // === Other admin areas ===
                        .requestMatchers("/users/**", "/categories/**").hasAnyAuthority("Admin", "Editor")

                        // === Default: any authenticated user can access remaining pages ===
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
                )

                // Handle access denied → show toast/error
                .exceptionHandling(ex -> ex
                    .accessDeniedHandler((request, response, accessDeniedException) -> {
                        // Redirect to home with error message
                        request.getSession().setAttribute("error", "Access denied: Customers cannot access this page.");
                        response.sendRedirect(request.getContextPath() + "/");
                    })
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
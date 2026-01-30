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
                // === PUBLIC PAGES (no login needed) ===
                .requestMatchers("/login", "/register").permitAll()
                .requestMatchers("/products/detail/**", "/reviews/public").permitAll()
                .requestMatchers("/addresses/states/**").permitAll()

                // === CUSTOMER ACTIONS (logged-in users only) ===
                .requestMatchers("/reviews/submit", "/reviews/delete/**", "/cart/**", "/checkout/**", "/orders/**", "/addresses/**").authenticated()

                // === PRODUCT MANAGEMENT (restricted roles) ===
                .requestMatchers("/products/new", "/products/save")
                .hasAnyAuthority("Admin", "Editor")
                .requestMatchers("/products/edit/**", "/products/delete/**")
                .hasAnyAuthority("Admin", "Editor", "Shipper")

                // === BRANDS ===
                .requestMatchers("/brands/**").hasAnyAuthority("Admin", "Editor")

                // === ADMIN-ONLY REVIEW MANAGEMENT ===
                .requestMatchers("/reviews/", "/reviews/page/**", "/reviews/edit/**", "/reviews/detail/**")
                .hasAuthority("Admin")

                // === OTHER ADMIN AREAS ===
                .requestMatchers("/users/**", "/categories/**").hasAnyAuthority("Admin", "Editor")

                // === DEFAULT: any authenticated user can access remaining pages ===
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
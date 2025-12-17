package swp391.fa25.lms.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import swp391.fa25.lms.config.security.CustomAuthenticationFailureHandler;
import swp391.fa25.lms.config.security.CustomUserDetailsService;
import swp391.fa25.lms.config.security.CustomAuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomAuthenticationSuccessHandler successHandler;

    @Autowired
    private CustomAuthenticationFailureHandler failureHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           CustomUserDetailsService userDetailsService) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .userDetailsService(userDetailsService)

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/home", "/home/**",
                                "/verify", "/verify-reset",
                                "/forgot-password",     // ⭐ đúng URL
                                "/reset-password",
                                "/change-password",
                                "/css/**", "/js/**", "/images/**", "/uploads/**",
                                "/toollist", "/toollist/**",
                                "/login", "/register", "/error"
                        ).permitAll()


                        // Blog public pages
                        .requestMatchers("/blog", "/blog/**").permitAll()

                        // Role-based pages
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/manager/**").hasRole("MANAGER")

                        // Everything else requires authentication
                        .requestMatchers("/moderator/**").hasRole("MOD")
                        .requestMatchers("/tools/moderator/**").hasRole("MOD")
                        .requestMatchers("/customer/**").hasRole("CUSTOMER")

                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/perform_login")
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/perform_logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true)
                        .permitAll()
                );

        return http.build();
    }
}

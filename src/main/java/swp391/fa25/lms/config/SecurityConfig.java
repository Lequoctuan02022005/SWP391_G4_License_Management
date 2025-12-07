package swp391.fa25.lms.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import swp391.fa25.lms.config.security.CustomUserDetailsService;
import swp391.fa25.lms.config.security.CustomAuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomAuthenticationSuccessHandler successHandler;

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
                        // Public resources
                        .requestMatchers("/", "/home", "/home/**",
                                "/css/**", "/js/**", "/images/**", "/uploads/**",
                                "/login", "/register", "/error").permitAll()

                        // Blog public pages
                        .requestMatchers("/blog", "/blog/**").permitAll()

                        // Role-based pages
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/manager/**").hasRole("MANAGER")
                        .requestMatchers("/mod/**").hasRole("MODERATOR")

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/perform_login")
                        .successHandler(successHandler)  // ⭐ Redirect theo ROLE
                        .failureUrl("/login?error=true")
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/perform_logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll()
                );

        return http.build();
    }
}

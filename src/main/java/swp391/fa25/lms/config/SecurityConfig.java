package swp391.fa25.lms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())           // tắt CSRF
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()       // CHO PHÉP MỌI REQUEST
                )
                .formLogin(login -> login.disable())    // tắt form login
                .logout(logout -> logout.disable());    // tắt logout

        return http.build();
    }
}

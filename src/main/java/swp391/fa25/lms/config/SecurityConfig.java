package swp391.fa25.lms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // CHO PHÉP TRUY CẬP TRANG CHỦ VÀ LOGIN KHÔNG CẦN ĐĂNG NHẬP
                        .requestMatchers("/", "/home", "/home/**", "/css/**", "/js/**", "/images/**", "/login", "/register", "/error").permitAll()

                        // Các trang cần quyền
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/mod/**").hasRole("MODERATOR")
                        .requestMatchers("/seller/**").hasRole("SELLER")

                        // Còn lại phải login
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        // QUAN TRỌNG: bỏ "true" đi → chỉ redirect khi login thành công, không redirect khi chưa login
                        .defaultSuccessUrl("/home")     // <-- bỏ ", true"
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login")
                        .permitAll()
                );

        return http.build();
    }

}


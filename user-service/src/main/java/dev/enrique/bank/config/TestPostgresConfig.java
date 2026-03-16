package dev.enrique.bank.config;

import org.modelmapper.ModelMapper;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Profile("test-postgres")
@Configuration
@EnableWebSecurity
public class TestPostgresConfig {
    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(auth -> auth.anyRequest().permitAll()).build();
    }

    @Bean
    public FilterRegistrationBean<RegisterStatusFilter> disableRegisterStatusFilter(
            RegisterStatusFilter registerStatusFilter) {
        FilterRegistrationBean<RegisterStatusFilter> registration =
                new FilterRegistrationBean<>(registerStatusFilter);
        registration.setEnabled(false);
        return registration;
    }
}

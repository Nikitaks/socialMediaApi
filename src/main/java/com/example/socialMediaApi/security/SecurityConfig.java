package com.example.socialMediaApi.security;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.socialMediaApi.config.JwtAuthenticationEntryPoint;
import com.example.socialMediaApi.config.JwtRequestFilter;

@EnableWebSecurity
public class SecurityConfig {
	@Autowired
	private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	@Autowired
	private JwtRequestFilter jwtRequestFilter;

	@Autowired
	MySQLUserDetailsService userDetailsService;

	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		// configure AuthenticationManager so that it knows from where to load
		// user for matching credentials
		auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
	}

	@Bean
	public AuthenticationManager authenticationManagerBean(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

	@Bean
	 public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
		   .csrf().disable()
		   .authorizeRequests()
		   .antMatchers("/swagger-ui/**").permitAll()
		   .antMatchers("/v3/**").permitAll()
		   .antMatchers("/authenticate").permitAll()
		   .antMatchers("/account/**").permitAll()
	       .antMatchers("/**").authenticated()
		   .and()
		   .exceptionHandling().authenticationEntryPoint(jwtAuthenticationEntryPoint)
		   .and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

		http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

	    return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
	   return new BCryptPasswordEncoder(12);
	}
}
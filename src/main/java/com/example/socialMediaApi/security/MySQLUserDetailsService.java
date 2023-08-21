package com.example.socialMediaApi.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.socialMediaApi.entity.User;
import com.example.socialMediaApi.repo.UserRepository;

@Service
public class MySQLUserDetailsService implements UserDetailsService{
  @Autowired
  private UserRepository repository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
	  User user = repository.findByName(username).get(0);

    if(user == null) {
      throw new UsernameNotFoundException("User not found");
    }

    List<SimpleGrantedAuthority> authorities = Arrays.asList(new SimpleGrantedAuthority("user"));

    return new org.springframework.security.core.userdetails.User(user.getName(), user.getPassword(), authorities);
  }
}
package com.example.socialMediaApi.repo;

import java.util.List;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import com.example.socialMediaApi.entity.User;

public interface UserRepository extends PagingAndSortingRepository<User, Long> {
    List<User> findByName(@Param("name") String name);
    List<User> findByEmail(@Param("email") String email);
    User findFirstUser_idByName(String name);
}

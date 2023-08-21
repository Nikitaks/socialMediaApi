package com.example.socialMediaApi.repo;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import com.example.socialMediaApi.entity.Post;


public interface PostRepository extends PagingAndSortingRepository<Post, Long> {
    List<Post> findByUser(@Param("user") Long user);
    List<Post> findByUserIn(List<Long> users, Pageable pageable);
}

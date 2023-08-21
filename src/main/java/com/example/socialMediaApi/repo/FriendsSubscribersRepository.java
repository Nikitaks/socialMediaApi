package com.example.socialMediaApi.repo;

import java.util.List;

import org.springframework.data.repository.PagingAndSortingRepository;

import com.example.socialMediaApi.entity.FriendsSubscribers;


public interface FriendsSubscribersRepository extends PagingAndSortingRepository<FriendsSubscribers, Long> {
	List<FriendsSubscribers> findByUser1AndUser2(Long User1, Long User2);
	List<FriendsSubscribers> findByUser1AndStatus(Long User1, FriendsSubscribers.Status status);
	List<FriendsSubscribers> findByUser2AndStatus(Long User2, FriendsSubscribers.Status status);
	List<Long> findUser1ByUser2AndStatus(Long User2, FriendsSubscribers.Status status);
}

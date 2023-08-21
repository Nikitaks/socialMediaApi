package com.example.socialMediaApi.repo;


import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.example.socialMediaApi.entity.Message;

public interface MessageRepository extends CrudRepository<Message, Long> {
	@Query("select m from Message m "
			+ "where m.fromUser = :fromUser and m.toUser = :toUser "
			+ "or m.toUser = :fromUser and m.fromUser = :toUser "
			+ "order by m.dateAndTime")
    List<Message> findMessages(@Param("fromUser") Long fromUser, @Param("toUser") Long toUser);
}

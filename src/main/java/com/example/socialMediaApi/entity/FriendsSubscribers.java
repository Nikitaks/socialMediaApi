package com.example.socialMediaApi.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "FRIENDS_AND_SUBSCRIBERS")
public class FriendsSubscribers {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private long user1;
    private long user2;
    private Status status;

    public FriendsSubscribers(long id, long user1, long user2, Status status) {
		this.id = id;
		this.user1 = user1;
		this.user2 = user2;
		this.status = status;
	}

	public FriendsSubscribers() {
	}

	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public long getUser1() {
		return user1;
	}
	public void setUser1(long user1) {
		this.user1 = user1;
	}
	public long getUser2() {
		return user2;
	}
	public void setUser2(long user2) {
		this.user2 = user2;
	}
	public Status getStatus() {
		return status;
	}
	public void setStatus(Status status) {
		this.status = status;
	}

	public enum Status {
		User1subscribedUser2,
		User2subscribedUser1,
		UsersFrends
	}
}



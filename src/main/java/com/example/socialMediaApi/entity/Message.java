package com.example.socialMediaApi.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "MESSAGES")
public class Message {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

	@Column(name = "FROM_USER")
    private long fromUser;
	@Column(name = "TO_USER")
    private long toUser;
    private LocalDateTime dateAndTime;
    private String content;

	public Message(long id, long fromUser, long toUser, LocalDateTime dateAndTime, String content) {
		super();
		this.id = id;
		this.fromUser = fromUser;
		this.toUser = toUser;
		this.dateAndTime = dateAndTime;
		this.content = content;
	}
	public Message() {
	}

	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public long getFromUser() {
		return fromUser;
	}
	public void setFromUser(long fromUser) {
		this.fromUser = fromUser;
	}
	public long getToUser() {
		return toUser;
	}
	public void setToUser(long toUser) {
		this.toUser = toUser;
	}
	public LocalDateTime getDateAndTime() {
		return dateAndTime;
	}
	public void setDateAndTime(LocalDateTime dateAndTime) {
		this.dateAndTime = dateAndTime;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
}

package com.example.socialMediaApi.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "POSTS")
public class Post {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

	@Column(updatable = false)
    private long user;

    private String header;
	private String content;
	//Comma separated
	private String imageurl;

	@Column(updatable = false)
	private LocalDateTime dateAndTime;

	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public long getUser() {
		return user;
	}
	public void setUser(long user) {
		this.user = user;
	}
	public String getHeader() {
		return header;
	}
	public void setHeader(String header) {
		this.header = header;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public String getImageurl() {
		return imageurl;
	}
	public void setImageurl(String imageurl) {
		this.imageurl = imageurl;
	}
	public LocalDateTime getDateAndTime() {
		return dateAndTime;
	}
	public void setDateAndTime(LocalDateTime dateAndTime) {
		this.dateAndTime = dateAndTime;
	}
	public boolean isTheSamePost(Post post) {
		return (this.header == null ? post.getHeader() == null : this.header.equals(post.getHeader()))
				&& (this.content == null ? post.getContent() == null : this.content.equals(post.getContent()))
				&& (this.imageurl == null ? post.getImageurl() == null : this.imageurl.equals(post.getImageurl()));
	}
}

package com.cm.domain;

import java.io.Serializable;

public class UserBasic implements Serializable {

	private static final long serialVersionUID = 1L;

	private int userId;
	private String userName;
	private String userPwd;
	private String nickyName;
	private String email;
	private String userTypeCd;
	private String userIconUrl;
	public int getUserId() {
		return userId;
	}
	public void setUserId(int userId) {
		this.userId = userId;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getUserPwd() {
		return userPwd;
	}
	public void setUserPwd(String userPwd) {
		this.userPwd = userPwd;
	}
	public String getNickyName() {
		return nickyName;
	}
	public void setNickyName(String nickyName) {
		this.nickyName = nickyName;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getUserTypeCd() {
		return userTypeCd;
	}
	public void setUserTypeCd(String userTypeCd) {
		this.userTypeCd = userTypeCd;
	}
	public String getUserIconUrl() {
		return userIconUrl;
	}
	public void setUserIconUrl(String userIconUrl) {
		this.userIconUrl = userIconUrl;
	}
	
	
}

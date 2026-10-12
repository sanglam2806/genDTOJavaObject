package com.mitsuru.gen.dto;

public class ComMitsuruNaDtoImpl implements ComMitsuruNaDto {
    private Long id;
    private String user;
    private String password;
    private String phoneNumber;
    private java.util.Date creationTime;

   	/** ユーザIDを取得する */
    @Override 
	public Long getId() {
		return id;
	}
   
   	/** ユーザIDを設定する */
   @Override 
	public void setId(Long id) { 
		this.id = id;
	}

   	/** ユーザを取得する */
    @Override 
	public String getUser() {
		return user;
	}
   
   	/** ユーザを設定する */
   @Override 
	public void setUser(String user) { 
		this.user = user;
	}

   	/** パスワードを取得する */
    @Override 
	public String getPassword() {
		return password;
	}
   
   	/** パスワードを設定する */
   @Override 
	public void setPassword(String password) { 
		this.password = password;
	}

   	/** 携帯番号を取得する */
    @Override 
	public String getPhoneNumber() {
		return phoneNumber;
	}
   
   	/** 携帯番号を設定する */
   @Override 
	public void setPhoneNumber(String phoneNumber) { 
		this.phoneNumber = phoneNumber;
	}

   	/** 作成日時を取得する */
    @Override 
	public java.util.Date getCreationTime() {
		return creationTime;
	}
   
   	/** 作成日時を設定する */
   @Override 
	public void setCreationTime(java.util.Date creationTime) { 
		this.creationTime = creationTime;
	}
}

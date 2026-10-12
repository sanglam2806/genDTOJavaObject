package com.mitsuru.gen.dto;

public interface ComMitsuruNaDto {
	/** ユーザIDを設定する */
    Long getId();

	/** ユーザIDを取得する */
    void setId(Long id);

	/** ユーザを設定する */
    String getUser();

	/** ユーザを取得する */
    void setUser(String user);

	/** パスワードを設定する */
    String getPassword();

	/** パスワードを取得する */
    void setPassword(String password);

	/** 携帯番号を設定する */
    String getPhoneNumber();

	/** 携帯番号を取得する */
    void setPhoneNumber(String phoneNumber);

	/** 作成日時を設定する */
    java.util.Date getCreationTime();

	/** 作成日時を取得する */
    void setCreationTime(java.util.Date creationTime);

}

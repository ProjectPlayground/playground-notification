package com.playground.notification.ds.google;


import com.google.gson.annotations.SerializedName;

public final class Distance {
	@SerializedName("text")
	private String mText;

	public String getText() {
		return mText;
	}
}

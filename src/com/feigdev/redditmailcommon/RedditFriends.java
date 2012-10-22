package com.feigdev.redditmailcommon;

public class RedditFriends {
	private String friend_name;
	private String friend_id;
	
	public RedditFriends(){
		friend_name = "";
		setFriendId("");
	}
	
	public String getFriend(){
		return friend_name;
	}
	
	public void setFriend(String f){
		friend_name = f;
	}

	public String getFriendId() {
		return friend_id;
	}

	public void setFriendId(String friend_id) {
		this.friend_id = friend_id;
	}
	
	
	
}

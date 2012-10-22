package com.feigdev.redditmailcommon;

public interface RedditMailListener {
	/***
	 * This method indicates that the login attempt was successful
	 * there should be a modhash and cookie in the prefs
	 */
	public void onLoginSuccess();
	
	/***
	 * This method indicates that the login attempt failed
	 * the user either entered something wrong or does not have
	 * a connection  or reddit down
	 */
	public void onLoginFailure(String message);
	
	/***
	 * The inbox has new content, refresh if the inbox is being viewed
	 */
	public void onInboxUpdate();
	
	/***
	 * The inbox failed to update
	 */
	public void onInboxFailure(String message);
	
	/***
	 * User has new friends in their friends list
	 */
	public void onFriendsUpdate();
	
	/***
	 * User's friends' comments updated
	 */
	public void onFriendsMessages();
	
	/***
	 * User's message posted successfully
	 */
	public void onPostSuccess();
	
	/***
	 * User's message post was a failure, either network error or reddit down
	 */
	public void onPostFailure(String message);
	
	/***
	 * User was mentioned - this feature is not yet implemented 
	 */
	public void onMentions();
	
	/***
	 * User's sent messages were updated
	 */
	public void onSentUpdate();
	
	/***
	 * User successfully added a friend
	 */
	public void onAddFriendSuccess();
	
	/***
	 * User successfully removed a friend
	 */
	public void onRemoveFriendSuccess();
	
	/***
	 * Failed to add friend
	 * @param message provided by SimpleResponse in WebCom
	 */
	public void onAddFriendFailure(String message);
	
	/***
	 * Failed to remove a friend
	 * @param message  provided by SimpleResponse in WebCom
	 */
	public void onRemoveFriendFailure(String message);
	
	/***
	 * User's moderator mail was updated
	 */
	public void onModMailUpdate();
	
	/***
	 * User successfully logged out
	 */
	public void onLogoutSuccess();

}

package com.feigdev.redditmailcommon;

import java.util.ArrayList;

import org.apache.http.cookie.Cookie;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.feigdev.R;
import com.feigdev.redditmailfree.RedditMailFreeActivity;
import com.feigdev.webcom.PersistentCookieStore;
import com.feigdev.webcom.SimpleResponse;
import com.feigdev.webcom.WebComListener;
import com.feigdev.webcom.WebModel;

public class RedditMailService extends Service implements WebComListener {
	private static final String USER_AGENT = "RedditMail for Android 1.6";
	private static final String TAG = "RedditMailService";
	private RedditMailListener rml;
	private Prefs prefs;
	private static final int LOGIN = 10123;
	private static final int GET_INBOX = 10124;
	private static final int GET_FRIENDS = 10125;
	private static final int POST_REPLY = 10126;
	private static final int FIND_MENTIONS = 10127;
	private static final int FRIENDS_POSTS = 10128;
	private static final int MOD_MAIL = 10129;
	private static final int GET_SENT = 10130;
	private static final int ADD_FRIEND = 10131;
	private static final int REMOVE_FRIEND = 10132;
	private static final int USER_STATUS = 10133;
	private static final int MARK_READ = 10134;
	private static final int POST_PM = 10135;
	private ArrayList<RedditMessages> messages = new ArrayList<RedditMessages>();
	private ArrayList<RedditMessages> sent_messages = new ArrayList<RedditMessages>();
	private ArrayList<RedditFriends> friends = new ArrayList<RedditFriends>();
	private ArrayList<RedditMessages> friends_messages = new ArrayList<RedditMessages>();
	private PersistentCookieStore cookieStore;
	private NotificationManager mNm;
	private ArrayList<String> notNewMessages = new ArrayList<String>();
	
	@Override
    public void onCreate() {
		super.onCreate();
		if (Constants.DEBUG){ Log.d(TAG,"onCreate called"); }
		cookieStore = new PersistentCookieStore(RedditMailService.this);
		mNm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		init();
    }
	
	@Override
    public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		if (Constants.DEBUG){ Log.d(TAG,"onStart called");}
		refreshAll();
	}

    @Override
    public void onDestroy() {
    	if (Constants.DEBUG){ Log.d(TAG,"onDestroy called");}
    	super.onDestroy();
    }
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return mBinder;
	}
	
	// This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    public final IBinder mBinder = new LocalBinder();
	
	 /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
    	public RedditMailService getService() {
            return RedditMailService.this;
        }
    }
	
    
	private void init(){
		if (Constants.DEBUG){ Log.d(TAG,"init() called");}
		prefs = new Prefs(this);
		messages = null;
		notNewMessages = new ArrayList<String>();
		refreshAll();
	}
	
	public void setRedditMailListener(RedditMailListener rml){
		this.rml = rml;
	}
	
	public ArrayList<RedditMessages> getMessages(){
		return messages;
	}
	
	public boolean isFriend(String name){
		if (null == getFriends()){
			return false;
		}
		else if (0 == getFriends().size()){
			return false;
		}
		for (int i=0; i<getFriends().size(); i++){
			if (getFriends().get(i).getFriend().equals(name)){
				return true;
			}
		}
		return false;
	}
	
	public boolean isPM(RedditMessages message){
		if (!message.isComment()){
				return true;
		}
		else if (!message.getSubject().contains("reply") && message.getSubreddit() == null){
			return true;
		}
		return false;
		
	}
	
	Handler handler = new Handler();
	
	public void refreshAll(){
		if (!isLoggedIn()){
			return;
		}
		requestInbox();
		requestSent();
		requestFriends();
		requestFriendsMessages();
		if (Constants.PV) { 
			handler.postDelayed(new Runnable(){
				public void run(){
					refreshAll();
				}
			},Constants.REQUEST_TIME); 
		}
	}
	
	public boolean isLoggedIn(){
		return prefs.isLoggedIn();
	}
	
	public void logout(){
		prefs.resetCookie();
		prefs.resetModhash();
		cookieStore.clear();
		if (rml != null){ rml.onLogoutSuccess(); }
	}
	
	public void addReadMessageId(String id){
		notNewMessages.add(id);
	}
	
	private void testNewMessages(){
		for (RedditMessages message: messages){
			if (notNewMessages.contains(message.getId())){
				message.setNew(false);
			}
		}
	}
	
	private void notifyNewMessage(){
		if (Constants.PV) {
	    	CharSequence text = "You have new Reddit Mail";
	        Notification notification = new Notification(R.drawable.orange_envelope, text, System.currentTimeMillis());
	        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, RedditMailFreeActivity.class), 0);
	        notification.setLatestEventInfo(this, "New RedditMail", text, contentIntent);
	        mNm.notify(R.string.notify_message, notification);
		}
	}
	
	public void cancelNotify(){
		mNm.cancel(R.string.notify_message);
	}
	
	
	public void requestLogin(String username, String password){
		if (Constants.DEBUG){ Log.d(TAG,"requestLogin called");}
		String url = "https://ssl.reddit.com/api/login/" + username;
		WebModel wm = new WebModel(url, this, LOGIN);
		wm.addHeadParam("User-Agent",USER_AGENT);
    	wm.setContentType("text/json");
    	wm.setRequestType(WebModel.POST);
    	wm.addParameter("user", username.replaceAll(" ", ""));
    	wm.addParameter("passwd", password);
    	wm.addParameter("api_type", "json");
    	wm.interact();
	}
	public void requestInbox() {
    	WebModel wm = new WebModel("http://www.reddit.com/message/inbox/.json", this, GET_INBOX);
		wm.addHeadParam("User-Agent",USER_AGENT);

    	wm.setContentType("text/json");
    	wm.setRequestType(WebModel.GET);

    	wm.addParameter("uh", prefs.getModhash());
    	wm.setCookies(cookieStore.getCookieStore());
    	wm.addParameter("api_type", "json");
    	wm.interact();
    }
	public void requestSent() {
    	WebModel wm = new WebModel("http://www.reddit.com/message/sent/.json", this, GET_SENT);
		wm.addHeadParam("User-Agent",USER_AGENT);

    	wm.setContentType("text/json");
    	wm.setRequestType(WebModel.GET);

    	wm.addParameter("uh", prefs.getModhash());
    	wm.setCookies(cookieStore.getCookieStore());
    	wm.addParameter("api_type", "json");
    	wm.interact();
    }
	public void requestFriends(){
		WebModel wm = new WebModel("https://ssl.reddit.com/prefs/friends/.json", this, GET_FRIENDS);
		wm.addHeadParam("User-Agent",USER_AGENT);

    	wm.setContentType("text/json");
    	wm.setRequestType(WebModel.GET);

    	wm.addParameter("uh", prefs.getModhash());
    	wm.setCookies(cookieStore.getCookieStore());
    	wm.addParameter("api_type", "json");
    	wm.interact();
	}
	public void requestFriendsMessages(){
		WebModel wm = new WebModel("http://www.reddit.com/r/friends/comments/.json", this, FRIENDS_POSTS);
		wm.addHeadParam("User-Agent",USER_AGENT);

    	wm.setContentType("text/json");
    	wm.setRequestType(WebModel.GET);

    	wm.addParameter("uh", prefs.getModhash());
    	wm.setCookies(cookieStore.getCookieStore());
    	wm.addParameter("api_type", "json");
    	wm.interact();
	}
	public void postMessages(RedditMessages message){
		WebModel wm = new WebModel("http://www.reddit.com/api/comment", this, POST_REPLY);
		wm.addHeadParam("User-Agent",USER_AGENT);

    	wm.setContentType("text/json");
    	wm.setRequestType(WebModel.POST);

    	wm.addParameter("uh", prefs.getModhash());
    	wm.setCookies(cookieStore.getCookieStore());
    	wm.addParameter("api_type", "json");
    	wm.addParameter("text", message.getContent());
    	wm.addParameter("thing_id", message.getId());
    	if (!message.getParent().equals("")){
    		wm.addParameter("parent_id", message.getParent());
    	}
    	
    	wm.interact();
	}
	public void markRead(RedditMessages message){
		WebModel wm = new WebModel("http://www.reddit.com/api/read_message", this, MARK_READ);
		wm.addHeadParam("User-Agent",USER_AGENT);

    	wm.setContentType("text/json");
    	wm.setRequestType(WebModel.POST);

    	wm.addParameter("uh", prefs.getModhash());
    	wm.setCookies(cookieStore.getCookieStore());
    	wm.addParameter("api_type", "json");
    	wm.addParameter("id", message.getId());
    	
    	wm.interact();
	}
	public void postPM(RedditMessages message){
		WebModel wm = new WebModel("http://www.reddit.com/api/compose", this, POST_PM);
		wm.addHeadParam("User-Agent",USER_AGENT);

    	wm.setContentType("text/json");
    	wm.setRequestType(WebModel.POST);

    	wm.addParameter("uh", prefs.getModhash());
    	wm.setCookies(cookieStore.getCookieStore());
    	wm.addParameter("api_type", "json");
    	wm.addParameter("to", message.getRecipient());
    	wm.addParameter("subject", message.getSubject());
    	wm.addParameter("text", message.getContent());
    	wm.addParameter("thing_id","compose-message");
    	
    	wm.interact();
	}
 
	
	@Override
	public void onResponse(SimpleResponse response) {
		if (response != null){
				if (Constants.DEBUG){Log.d(TAG,"response - " + response.getMessage());}
        		switch (response.getId()){
        		case LOGIN:
        			if (response.getStatus() == SimpleResponse.FAIL){
        				if (rml != null){ rml.onLoginFailure(response.getMessage()); }
                	}
        			else {
	        			switch (RedditJsonParser.parseLoginResponse(response.getMessage(), prefs)){
	        			case 0:
	        				if (response.getCookies() != null){
	        					cookieStore.clear();
	        					for (Cookie cookie : response.getCookies().getCookies()){
	        						cookieStore.addCookie(cookie);
	        						if (Constants.DEBUG){ Log.d(TAG, "adding cookie: " + cookie.toString());}
	        					}
	        				}
	        				if (Constants.DEBUG){ Log.d(TAG,"login successful");}
	        				refreshAll();
	        				if (rml != null){ rml.onLoginSuccess();}
	        				break;
	        			case 1:
	        				if (Constants.DEBUG){ Log.w(TAG,"login failed");}
	        				if (rml != null){ rml.onLoginFailure(RedditJsonParser.getLoginErrors(response.getMessage()));}
	        				break;
	        			case -1:
	        				if (Constants.DEBUG){ Log.w(TAG,"login failed - could not parse response");}
	        				if (rml != null){ rml.onLoginFailure("Unexpected response");}
	        				break;
	        			}
        			}
        			break;
        		case GET_INBOX:
        			if (response.getStatus() == SimpleResponse.FAIL){
        				if (Constants.DEBUG){ Log.w(TAG,"inbox failed - " + response.getMessage());}
        				if (rml != null){ rml.onInboxFailure(response.getMessage());}
                	}
        			else {
	        			messages = RedditJsonParser.parseMessageResponse(response.getMessage()); 
	        			if (0 == messages.size()){
	        				if (rml != null){ rml.onInboxUpdate();}
	        			}
	        			else if (messages != null){
	        				testNewMessages();
	        				if (rml != null){ rml.onInboxUpdate();}
	        				if (Constants.PV){
	        					if (messages.get(0).isNew() && !notNewMessages.contains(messages.get(0).getId())){
	        						notifyNewMessage();
	        					}else {
	        						cancelNotify();
	        					}
	        				}
	        			}
	        			else {
	        				if (Constants.DEBUG){ Log.w(TAG,"inbox failed - could not parse response");}
	        			}
	        		}
        			break;
        		case GET_FRIENDS:
        			if (response.getStatus() == SimpleResponse.FAIL){
        				if (Constants.DEBUG){ Log.w(TAG,"friends failed - " + response.getMessage());}
        			}
        			else {
	        			setFriends(RedditJsonParser.parseFriendResponse(response.getMessage())); 
	        			if (getFriends() != null){
	        				if (rml != null){ rml.onFriendsUpdate();}
	        			}
	        			else {
	        				if (Constants.DEBUG){ Log.w(TAG,"friends failed - could not parse response");}
	        			}
	        		}
        			break;
        		case POST_REPLY:
        			if (response.getStatus() == SimpleResponse.FAIL){
        				if (rml != null){ rml.onPostFailure(response.getMessage());}
                	}
        			RedditMessages replyMessage = RedditJsonParser.parsePostMessageResponse(response.getMessage()); 
        			if (replyMessage != null){
        				if (rml != null){ rml.onPostSuccess();}
        			}
        			else {
        				String error = RedditJsonParser.parsePostForError(response.getMessage());
        				if (Constants.DEBUG){ Log.w(TAG,"post failed - could not parse response");}
        				if (rml != null && null != error && "" != error){ rml.onPostFailure(error);}
        				else if (null != rml){rml.onPostFailure("could not parse response");}
        			}
        			break;
        		case FIND_MENTIONS:
        			// No way to do this yet
        			break;
        		case FRIENDS_POSTS:
        			if (response.getStatus() == SimpleResponse.FAIL){
        				if (Constants.DEBUG){ Log.w(TAG,"get friends messages failed - " + response.getMessage());}
        			}
        			else {
	        			setFriendsMessages(RedditJsonParser.parseMessageResponse(response.getMessage())); 
	        			if (getFriendsMessages() != null){
	        				if (rml != null){ rml.onFriendsMessages();}
	        			}
	        			else {
	        				if (Constants.DEBUG){ Log.w(TAG,"friends messages failed - could not parse response");}
	        			}
	        		}
        			break;
        		case MOD_MAIL:
        			break;
        		case GET_SENT:
        			if (response.getStatus() == SimpleResponse.FAIL){
        				if (Constants.DEBUG){ Log.w(TAG,"get sent messages failed - " + response.getMessage());}
        				if (rml != null){ rml.onInboxFailure(response.getMessage());}
                	}
        			else {
	        			setSentMessages(RedditJsonParser.parseMessageResponse(response.getMessage())); 
	        			if (getSentMessages() != null){
	        				if (rml != null){ rml.onSentUpdate();}
	        			}
	        			else {
	        				if (Constants.DEBUG){ Log.w(TAG,"get sent messages failed - could not parse response");}
	        			}
	        		}
        			break;
        		case ADD_FRIEND:
        			break;
        		case REMOVE_FRIEND:
        			break;
        		case USER_STATUS:
        			// kick off other requests based on response
        			// see here - https://github.com/reddit/reddit/wiki/API%3A-me.json
        			break;
        		default: 
        			if (Constants.DEBUG){ Log.d(TAG,"ID: " + response.getId() + ", Message: " + response.getMessage());}
        			break;
        		}
    	}
		
	}

	public ArrayList<RedditFriends> getFriends() {
		return friends;
	}

	public void setFriends(ArrayList<RedditFriends> friends) {
		this.friends = friends;
	}

	public ArrayList<RedditMessages> getSentMessages() {
		return sent_messages;
	}

	public void setSentMessages(ArrayList<RedditMessages> sent_messages) {
		this.sent_messages = sent_messages;
	}

	public ArrayList<RedditMessages> getFriendsMessages() {
		return friends_messages;
	}

	public void setFriendsMessages(ArrayList<RedditMessages> friends_messages) {
		this.friends_messages = friends_messages;
	}


}

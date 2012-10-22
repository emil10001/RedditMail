package com.feigdev.redditmailcommon;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.Html;
import android.util.Log;

public class RedditJsonParser {
	private static final String JSON = "json";
	private static final String ERRORS = "errors";
	private static final String DATA = "data";
	private static final String THINGS = "things";
	private static final String CHILDREN = "children";
	private static final String MODHASH = "modhash";
	private static final String COOKIE = "cookie";
	private static final String AUTHOR = "author";
	private static final String BODY = "body_html";
	private static final String NAME = "name";
	private static final String ID = "id";
	private static final String DEST = "dest";
	private static final String CREATED = "created";
	private static final String CONTEXT = "context";
	private static final String LINK_ID = "link_id";
	private static final String NEW = "new";
	private static final String KIND = "kind";
	private static final String PARENT_ID = "parent_id";
	private static final String WAS_COMMENT = "was_comment";
	private static final String SUBREDDIT = "subreddit";
	private static final String SUBJECT = "subject";
	
	public static int parseLoginResponse(String obj, Prefs prefs){
		try {
			JSONObject jObj = new JSONObject(obj);
			JSONObject json = (JSONObject) jObj.get(JSON);
			JSONArray errors = (JSONArray) json.get(ERRORS);
			if (errors.length() > 0){
				return 1;
			}
			JSONObject data = (JSONObject) json.get(DATA);
			prefs.setModhash(data.getString(MODHASH));
			prefs.setCookie(data.getString(COOKIE));
			return 0;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
	}
	
	public static String getLoginErrors(String obj){
		String errorList = "Errors: ";
		try {
			JSONObject jObj = new JSONObject(obj);
			JSONObject json = (JSONObject) jObj.get(JSON);
			JSONArray errors = (JSONArray) json.get(ERRORS);
			for (int i=0; i<errors.length(); i++){
				errorList += errors.get(i).toString() + ", ";
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}		
		return errorList;
	}
	

	public static ArrayList<RedditMessages> parseMessageResponse(String obj){
		ArrayList<RedditMessages> messages = new ArrayList<RedditMessages>();
		try {
			JSONObject jObj = new JSONObject(obj);
			if (jObj.isNull(DATA)){
				return messages;
			}
			JSONObject data = (JSONObject) jObj.get(DATA);
			JSONArray children = (JSONArray) data.get(CHILDREN);
			for (int i = 0; i < children.length(); i++){
				data = (JSONObject) children.getJSONObject(i).get(DATA);
				RedditMessages rm = new RedditMessages();
				rm.setSender(data.optString(AUTHOR));
				rm.setRecipient(data.optString(DEST));
				rm.setParent(data.optString(PARENT_ID));
				rm.setId(data.optString(NAME));
				if (rm.getId().equals("")){
					rm.setId(((JSONObject)children.getJSONObject(i)).optString(KIND) + "_" + data.optString(ID));
				}
				rm.setContent(Html.fromHtml(data.optString(BODY)).toString());
				rm.setTime(data.optString(CREATED));
				rm.setContext(data.optString(CONTEXT));
				rm.setNew(data.optBoolean(NEW));
				rm.setComment(data.optBoolean(WAS_COMMENT,true));
				rm.setSubreddit(data.optString(SUBREDDIT));
				rm.setSubject(data.optString(SUBJECT));
				messages.add(rm);
				
			}
			return messages;
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static ArrayList<RedditFriends> parseFriendResponse(String message) {
		ArrayList<RedditFriends> friends = new ArrayList<RedditFriends>();
		try {
			JSONArray jArr = new JSONArray(message);
			if (jArr.isNull(0)){
				return friends;
			}
			JSONObject jObj = jArr.getJSONObject(0);
			if (jObj.isNull(DATA)){
				return friends;
			}
			JSONObject data = (JSONObject) jObj.get(DATA);
			JSONArray children = (JSONArray) data.get(CHILDREN);
			for (int i = 0; i < children.length(); i++){
				RedditFriends friend = new RedditFriends();
				friend.setFriend(((JSONObject)children.get(i)).getString(NAME));
				friend.setFriendId(((JSONObject)children.get(i)).getString(ID));
				friends.add(friend);
			}
			return friends;
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
	}


	public static RedditMessages parsePostMessageResponse(String obj){
		RedditMessages rm = new RedditMessages();
		try {
			JSONObject data = (JSONObject)((JSONArray)((JSONArray)((JSONArray)((JSONArray)(new JSONObject(obj)).get("jquery")).get(18)).get(3)).get(0)).get(0);
			rm.setId(data.optString(ID));
			return rm;
		} catch (JSONException e) {
			try {
				String id = (new JSONObject(obj)).getJSONObject(JSON).getJSONObject(DATA).getJSONArray(THINGS).getJSONObject(0).getJSONObject(DATA).getString(ID);
				rm.setId(id);
				return rm;
			} catch (JSONException e2){
				return null;
			}
		}
	}

	public static String parsePostForError(String message) {
		String rm = "";
		try {
			JSONArray errors = (new JSONObject(message)).getJSONObject(JSON).getJSONArray(ERRORS).getJSONArray(0);
			for (int i=0; i< errors.length(); i++){
				rm += errors.getString(i);
				rm += " ";
			}
			return rm;
		} catch (JSONException e) {
			return null;
		}
	}
}

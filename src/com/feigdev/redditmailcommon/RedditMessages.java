package com.feigdev.redditmailcommon;

public class RedditMessages {
	//	"author": "jameswptv" sender
	private String sender;
	//	"dest": "emil10001" recipient
	private String recipient;
	// "body_html" needs to be parsed twice
	private String content;
	// "created" long time in seconds
	private String time;
	//	"name": "t1_c3nkfhi" id
	// or
	// "id": "c3nkfhi" kind + _ + id = name
	private String id;
	// "parent_id": t3_pa4ic - this is for posting responses to PMs 
	private String parent;
	// "new": true/false
	private boolean isNew;
	//	"was_comment": true/false isComment true if comment on thread, false if PM
	private boolean isComment; 
	//	"subreddit": "westpalmatheists"
	private String subreddit;
	//	"context": "/r/westpalmatheists/comments/p0o4m/travel_plans_for_the_reason_rally/c3nkfhi?context=3" 
	private String context;
	//	"subject": "post reply" or "comment reply" or "an actual subject for PMs"
	private String subject;
	
	
	public RedditMessages(){
		setSender("");
		setContent("");
		setTime("");
		setRecipient("");
		setId("");
		setParent("");
		setNew(false);
		setComment(true);
		setSubreddit("");
		setContext("");
		setSubject("");
	}
	
	public RedditMessages(RedditMessages another){
		setSender(another.getSender());
		setContent(another.getContent());
		setTime(another.getTime());
		setRecipient(another.getRecipient());
		setId(another.getId());
		setParent(another.getParent());
		setNew(another.isNew());
		setComment(another.isComment());
		setSubreddit(another.getSubreddit());
		setContext(another.getContext());
		setSubject(another.getSubject());
	}
	
	public String getSender() {
		return sender;
	}
	public void setSender(String sender) {
		this.sender = sender;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public String getTime() {
		return time;
	}
	public void setTime(String time) {
		this.time = time;
	}
	public String getRecipient() {
		return recipient;
	}
	public void setRecipient(String recipient) {
		this.recipient = recipient;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	public boolean isNew() {
		return isNew;
	}

	public void setNew(boolean isNew) {
		this.isNew = isNew;
	}

	public boolean isComment() {
		return isComment;
	}

	public void setComment(boolean isComment) {
		this.isComment = isComment;
	}

	public String getSubreddit() {
		return subreddit;
	}

	public void setSubreddit(String subreddit) {
		this.subreddit = subreddit;
	}

	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}
}

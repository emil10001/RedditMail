package com.feigdev.redditmailcommon;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Prefs {
	private SharedPreferences app_preferences;
	private SharedPreferences.Editor editor;
	private Context context;
	public static final String MODHASH = "modhash";
	public static final String COOKIE = "cookie";
	public static final String PING_TIME = "ping_time";
	
	/***
	 * Constructor for the class. This will initialize any null variables
	 *  
	 * @param context Need to pass in the current context, usually 'this'
	 */
	public Prefs(Context context){
		this.context = context;
		app_preferences = PreferenceManager.getDefaultSharedPreferences(context);
		editor = app_preferences.edit();
        if (!app_preferences.contains(MODHASH)){
        	this.resetModhash();
        }
        if (!app_preferences.contains(COOKIE)){
        	this.resetCookie();
        }
        if (!app_preferences.contains(PING_TIME)){
        	this.setPingTime(1000*60);
        }
	}

	public boolean isLoggedIn() {
		app_preferences = PreferenceManager.getDefaultSharedPreferences(context);
		return !( (app_preferences.getString(MODHASH, MODHASH).equals(MODHASH)) || (app_preferences.getString(COOKIE, COOKIE).equals(COOKIE)) );
	}
	
	public String getModhash(){
		app_preferences = PreferenceManager.getDefaultSharedPreferences(context);
		return app_preferences.getString(MODHASH, MODHASH);
	}

	public String getCookie(){
		app_preferences = PreferenceManager.getDefaultSharedPreferences(context);
		return app_preferences.getString(COOKIE, COOKIE);
	}
	
	public int getPingTime(){
		app_preferences = PreferenceManager.getDefaultSharedPreferences(context);
		return app_preferences.getInt(PING_TIME, 300000);
	}
	
	public void setPingTime(int sIn) {
		app_preferences = PreferenceManager.getDefaultSharedPreferences(context);
		if (app_preferences.contains(PING_TIME)){
        	editor.remove(PING_TIME);
        }
        editor.putInt(PING_TIME, sIn);
        editor.commit(); 
    }
	
	public void setModhash(String sIn) {
		app_preferences = PreferenceManager.getDefaultSharedPreferences(context);
		if (app_preferences.contains(MODHASH)){
        	editor.remove(MODHASH);
        }
        editor.putString(MODHASH, sIn);
        editor.commit(); 
    }
	
	public void setCookie(String sIn) {
		app_preferences = PreferenceManager.getDefaultSharedPreferences(context);
		if (app_preferences.contains(COOKIE)){
        	editor.remove(COOKIE);
        }
        editor.putString(COOKIE, sIn);
        editor.commit(); 
    }
	

	public void resetModhash() {
		app_preferences = PreferenceManager.getDefaultSharedPreferences(context);
		if (app_preferences.contains(MODHASH)){
        	editor.remove(MODHASH);
        }
        editor.putString(MODHASH, MODHASH);
        editor.commit(); 
    }
	
	public void resetCookie() {
		app_preferences = PreferenceManager.getDefaultSharedPreferences(context);
		if (app_preferences.contains(COOKIE)){
        	editor.remove(COOKIE);
        }
        editor.putString(COOKIE, COOKIE);
        editor.commit(); 
    }
}

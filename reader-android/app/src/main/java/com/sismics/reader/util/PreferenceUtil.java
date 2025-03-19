package com.sismics.reader.util;

import java.util.List;

import org.apache.http.cookie.Cookie;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.loopj.android.http.PersistentCookieStore;

/**
 * Utility class on preferences.
 * 
 * @author bgamard
 */
public class PreferenceUtil {

    public static final String PREF_CACHED_USER_INFO_JSON = "pref_cachedUserInfoJson";
    public static final String PREF_CACHED_SUBSCRIPTION_JSON = "pref_cachedSubscriptionJson";
    public static final String PREF_SERVER_URL = "pref_ServerUrl";
    public static final String PREF_SUBSCRIPTION_UNREAD = "pref_subscriptionsUnread";
    public static final String PREF_ARTICLES_FETCHED = "pref_articlesFetched";
    public static final String PREF_DEFAULT_SUBSCRIPTION = "pref_defaultSubscription";
    public static final String PREF_FONT_SIZE = "pref_fontSize";
    
    /**
     * Returns a preference of boolean type.
     * @param context Context
     * @param key Shared preference key
     * @return Shared preference value
     */
    public static boolean getBooleanPreference(Context context, String key, boolean defaultValue) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(key, defaultValue);
    }
    
    /**
     * Returns a preference of string type.
     * @param context Context
     * @param key Shared preference key
     * @return Shared preference value
     */
    public static String getStringPreference(Context context, String key) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(key, null);
    }
    
    /**
     * Returns a preference of integer type.
     * @param context Context
     * @param key Shared preference key
     * @return Shared preference value
     */
    public static int getIntegerPreference(Context context, String key, int defaultValue) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            String pref = sharedPreferences.getString(key, "");
            try {
                return Integer.parseInt(pref);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        } catch (ClassCastException e) {
            return sharedPreferences.getInt(key, defaultValue);
        }
        
    }
    
    /**
     * Update JSON cache.
     * @param context Context
     * @param key Shared preference key
     * @param json JSON data
     */
    public static void setCachedJson(Context context, String key, JSONObject json) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().putString(key, json != null ? json.toString() : null).commit();
    }
    
    /**
     * Returns a JSON cache.
     * @param context Context
     * @param key Shared preference key
     * @return JSON data
     */
    public static JSONObject getCachedJson(Context context, String key) {
        try {
            return new JSONObject(getStringPreference(context, key));
        } catch (Exception e) {
            // The cache is not parsable, clean this up
            setCachedJson(context, key, null);
            return null;
        }
    }
    
    /**
     * Update server URL.
     * @param context Context
     */
    public static void setServerUrl(Context context, String serverUrl) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().putString(PREF_SERVER_URL, serverUrl).commit();
    }
    
    /**
     * Empty user caches.
     * @param context Context
     */
    public static void resetUserCache(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        Editor editor = sharedPreferences.edit();
        editor.putString(PREF_CACHED_USER_INFO_JSON, null);
        editor.commit();
    }
    
    /**
     * Returns auth token cookie from shared preferences.
     * @return Auth token
     */
    public static String getAuthToken(Context context) {
        PersistentCookieStore cookieStore = new PersistentCookieStore(context);
        List<Cookie> cookieList = cookieStore.getCookies();
        for (Cookie cookie : cookieList) {
            if (cookie.getName().equals("auth_token")) {
                return cookie.getValue();
            }
        }
        
        return null;
    }

    /**
     * Returns cleaned server URL.
     * @param context Context
     * @return Server URL
     */
    public static String getServerUrl(Context context) {
        String serverUrl = getStringPreference(context, PREF_SERVER_URL);
        if (serverUrl == null) {
            return null;
        }
        
        // Trim
        serverUrl = serverUrl.trim();
        
        if (!serverUrl.startsWith("http")) {
            // Try to add http
            serverUrl = "http://" + serverUrl;
        }
        
        if (serverUrl.endsWith("/")) {
            // Delete last /
            serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
        }
        
        // Remove /api
        if (serverUrl.endsWith("/api")) {
            serverUrl = serverUrl.substring(0, serverUrl.length() - 4);
        }
        
        return serverUrl;
    }
}

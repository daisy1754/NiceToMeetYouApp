package jp.gr.java_conf.daisy.n2mu;

import android.content.Context;
import android.content.SharedPreferences;

public class Preferences {
    public static final String PREFERENCES_NAME = "N2mu";

    public static SharedPreferences getDefault(Context context) {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public static SharedPreferences getDefault(Context context, int mode) {
        return context.getSharedPreferences(PREFERENCES_NAME, mode);
    }

    public static SharedPreferences.Editor getDefaultEditor(Context context) {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
    }

    public static boolean getBooleanFromDefaultPref(
            Context context, String key, boolean defaultValue) {
        return getDefault(context).getBoolean(key, defaultValue);
    }

    public static String getPreferencesName() {
        return PREFERENCES_NAME;
    }

    public static final String KEY_LINKEDIN_ACCESS_TOKEN = "linkedinToken";
    public static final String KEY_LINKEDIN_ACCESS_TOKEN_SECRET = "linkedinTokenSecret";
    public static final String KEY_LINKEDIN_ACCESS_TOKEN_EXPIRES = "linkedinTokenExpires";
    public static final String KEY_TWITTER_OAUTH_TOKEN = "oauth_token";
    public static final String KEY_TWITTER_OAUTH_SECRET = "oauth_token_secret";
}
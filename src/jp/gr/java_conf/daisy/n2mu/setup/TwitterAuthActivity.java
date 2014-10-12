package jp.gr.java_conf.daisy.n2mu.setup;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import jp.gr.java_conf.daisy.n2mu.BuildConfig;
import jp.gr.java_conf.daisy.n2mu.MainActivity;
import jp.gr.java_conf.daisy.n2mu.Preferences;
import jp.gr.java_conf.daisy.n2mu.R;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterAuthActivity extends Activity {
    static final String TWITTER_CALLBACK_URL = "oauth://t4jsample";
    static final String URL_TWITTER_OAUTH_VERIFIER = "oauth_verifier";

    private static Twitter mTwitter;
    private static RequestToken mRequestToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth_with_twitter);
        if (getIntent() != null) {
            Uri uri = getIntent().getData();
            if (uri != null && uri.toString().startsWith(TWITTER_CALLBACK_URL)) {
                handleCallback(uri);
                return;
            }
        }
        startAuth();

        findViewById(R.id.pinButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText editText = new EditText(TwitterAuthActivity.this);
                new AlertDialog.Builder(TwitterAuthActivity.this)
                        .setView(editText)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    String pin = editText.getText().toString();
                                    saveTokenAndFinish(pin);
                                } catch (TwitterException e) {
                                    throw new IllegalStateException("", e);
                                }
                            }
                        }).show();
            }
        });
    }

    private void startAuth() {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setOAuthConsumerKey(BuildConfig.TWITTER_CONSUMER_KEY);
        builder.setOAuthConsumerSecret(BuildConfig.TWITTER_CONSUMER_SECRET);

        Configuration configuration = builder.build();

        TwitterFactory factory = new TwitterFactory(configuration);
        mTwitter = factory.getInstance();

        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    mRequestToken = mTwitter.getOAuthRequestToken(TWITTER_CALLBACK_URL);
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(mRequestToken.getAuthenticationURL())));
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new IllegalStateException("", e);
                }
            }
        });
        thread.start();
    }

    private void handleCallback(Uri uri) {
        // oAuth verifier
        final String verifier = uri.getQueryParameter(URL_TWITTER_OAUTH_VERIFIER);
        try {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        saveTokenAndFinish(verifier);
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new IllegalStateException("", e);
                    }
                    return null;
                }
            }.execute();
        } catch (Exception e) {
            throw new IllegalStateException("", e);
        }
    }

    private void saveTokenAndFinish(String verifier) throws TwitterException {
        AccessToken accessToken = mTwitter.getOAuthAccessToken(mRequestToken, verifier);
        SharedPreferences.Editor editor = Preferences.getDefaultEditor(TwitterAuthActivity.this);
        editor.putString(Preferences.KEY_TWITTER_OAUTH_TOKEN, accessToken.getToken());
        editor.putString(Preferences.KEY_TWITTER_OAUTH_SECRET, accessToken.getTokenSecret());
        editor.commit();
        finish();
    }
}

package jp.gr.java_conf.daisy.n2mu.setup;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

import com.google.code.linkedinapi.client.oauth.LinkedInAccessToken;
import com.google.code.linkedinapi.client.oauth.LinkedInOAuthService;
import com.google.code.linkedinapi.client.oauth.LinkedInOAuthServiceFactory;
import com.google.code.linkedinapi.client.oauth.LinkedInRequestToken;

import jp.gr.java_conf.daisy.n2mu.BuildConfig;
import jp.gr.java_conf.daisy.n2mu.Preferences;
import jp.gr.java_conf.daisy.n2mu.R;

public class AuthWithLinkedinActivity extends Activity {
    public static final int AUTH_RESULT_OK = 100;
    public static final String OAUTH_CALLBACK_SCHEME = "x-oauthflow-linkedin";
    public static final String OAUTH_CALLBACK_HOST = "callback";
    public static final String OAUTH_CALLBACK_URL = OAUTH_CALLBACK_SCHEME + "://" + OAUTH_CALLBACK_HOST;

    private final LinkedInOAuthService oAuthService = LinkedInOAuthServiceFactory.getInstance().createLinkedInOAuthService(BuildConfig.LINKEDIN_CONSUMER_KEY, BuildConfig.LINKEDIN_CONSUMER_SECRET);
    private LinkedInRequestToken liToken;

    static boolean mAuthStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth_with_linkedin);
        if (mAuthStarted) {
            return;
        }
        mAuthStarted = true;
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                liToken = oAuthService.getOAuthRequestToken(OAUTH_CALLBACK_URL);
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(liToken.getAuthorizationUrl()));
                startActivity(i);
                return null;
            }
        }.execute();
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                String verifier = intent.getData().getQueryParameter("oauth_verifier");
                LinkedInAccessToken accessToken = oAuthService.getOAuthAccessToken(liToken, verifier);
                Preferences.getDefaultEditor(AuthWithLinkedinActivity.this)
                        .putString(Preferences.KEY_LINKEDIN_ACCESS_TOKEN, accessToken.getToken())
                        .putString(Preferences.KEY_LINKEDIN_ACCESS_TOKEN, accessToken.getTokenSecret())
                        .putLong(Preferences.KEY_LINKEDIN_ACCESS_TOKEN_EXPIRES, accessToken.getExpirationTime().getTime())
                        .commit();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                Toast.makeText(AuthWithLinkedinActivity.this, "Auth ok", Toast.LENGTH_SHORT).show();
                finishActivity(AUTH_RESULT_OK);
            }
        }.execute();

    }
}

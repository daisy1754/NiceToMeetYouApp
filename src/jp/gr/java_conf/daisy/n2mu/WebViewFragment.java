package jp.gr.java_conf.daisy.n2mu;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.HashSet;
import java.util.Set;

/**
 * Webview for showing Linkedin/Twitter information
 * TODO: implemented native view.
 */
public class WebViewFragment extends Fragment {
    public static final String VIEW_TYPE_LINKEDIN = "Linkedin";
    public static final String VIEW_TYPE_TWITTER = "Twitter";
    private static final String KEY_TYPE = "WebViewFragment:Type";
    private static final String USER_ID = "WebViewFragment:UserId";
    private WebView mWebView;
    private String initialUrl;
    private String mType;
    private String mUserId;

    public static WebViewFragment newInstance(String type, String userId) {
        WebViewFragment fragment = new WebViewFragment();
        fragment.mType = type;
        fragment.mUserId = userId;
        return fragment;
    }

    public void loadUrl(String url) {
        if (mWebView != null) {
            mWebView.loadUrl(url);
        } else {
            initialUrl = url;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if ((savedInstanceState != null) && savedInstanceState.containsKey(KEY_TYPE)) {
            mType = savedInstanceState.getString(KEY_TYPE);
        }
        if ((savedInstanceState != null) && savedInstanceState.containsKey(USER_ID)) {
            mType = savedInstanceState.getString(USER_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.fragment_web_view, container, false);
        mWebView = (WebView) rootView.findViewById(R.id.webView);
        if (initialUrl != null) {
            mWebView.loadUrl(initialUrl);
        }
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(getActivity(), description, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (mType == VIEW_TYPE_LINKEDIN) {
                    extractKeywordIfNecessary();
                }
            }
        });
        return rootView;
    }

    private void extractKeywordIfNecessary() {
        SQLiteDatabase db = new DBHelper(getActivity()).getReadableDatabase();
        Cursor cursor = db.query("users", new String[]{"gotKeywordFromLinkedIn"}, "forceUserId=?",
                new String[]{mUserId}, null, null, null);
        cursor.moveToFirst();
        if (cursor.getInt(cursor.getColumnIndex("gotKeywordFromLinkedIn")) == 1) {
            return;
        }

        mWebView.evaluateJavascript("\n" +
                "  var keywords = []\n" +
                "  var endoses = document.querySelectorAll(\".endorse-item-name a\");\n" +
                "  for (var i = 0; i < 3 && i < endoses.length; i++) {\n" +
                "    var endose = endoses[i];\n" +
                "    keywords.push(endose.innerText);\n" +
                "  }\n" +
                "  var pastPositions = document.querySelectorAll(\".past-position a\");\n" +
                "  for (var i = 0; i < 6 && i < pastPositions.length; i++) {\n" +
                "    var post = pastPositions[i];\n" +
                "    var href = post.getAttribute(\"href\");\n" +
                "    if (href.indexOf(\"title\") >= 0 || href.indexOf(\"company\") >= 0) {\n" +
                "      keywords.push(post.innerText);\n" +
                "    }\n" +
                "  }\n" +
                "  var educations = document.getElementsByClassName(\"education a\");\n" +
                "  for (var i = 0; i < 6 && i < educations.length; i++) {\n" +
                "    var edu = educations[i];\n" +
                "    keywords.push(edu.innerText);\n" +
                "  }\n" +
                "  var interests = document.querySelectorAll(\"#interests li a\");\n" +
                "  for (var i = 0; i < 6 && i < interests.length; i++) {\n" +
                "    var int = interests[i];\n" +
                "    keywords.push(int.innerText);\n" +
                "  }\n" +
                "  keywords", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(final String text) {
                if (text.length() > 10) {
                    new AlertDialog.Builder(getActivity())
                            .setMessage("We found keywords for this person: "
                            + text + "\n"
                            + "Do you want to save this?")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    saveKeywords(text);
                                }
                            }).show();
                }
            }

            private void saveKeywords(String jsText) {
                StringBuilder builder = new StringBuilder();
                Set<String> words = new HashSet<String>();
                boolean escape = false;
                boolean inWords = false;
                for (int i = 1; i < jsText.length() - 1; i++) {
                    char c = jsText.charAt(i);
                    if (escape) {
                        builder.append(c);
                        escape = false;
                    } else if (c == '\\') {
                        escape = !escape;
                    } else if (c == '"' && !escape) {
                        if (inWords) {
                            if (builder.length() != 0) {
                                words.add(builder.toString());
                                builder = new StringBuilder();
                            }
                        }
                        inWords = !inWords;
                    } else if (c == ',' && !inWords) {
                        // ignore
                    } else {
                        builder.append(c);
                    }
                }

                SQLiteDatabase db = DBHelper.getWritableDatabase(getActivity());
                for (String word: words) {
                    ContentValues values = new ContentValues();
                    values.put("userId", mUserId);
                    values.put("keyword", word);
                    db.insert("keywords", "", values);
                }
                ContentValues values = new ContentValues();
                values.put("gotKeywordFromLinkedIn", 1);
                db.update("users", values, "forceUserId=?", new String[]{mUserId});
            }
        });
    }
}

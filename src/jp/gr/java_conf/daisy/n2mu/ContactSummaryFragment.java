package jp.gr.java_conf.daisy.n2mu;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class ContactSummaryFragment extends Fragment {
    private String mUserId;
    private String mKeywordText;
    private String mTwitterScreenName;

    public void setUserId(String userId) {
        mUserId = userId;
    }

    public void setTwitterScreenName(String twitterScreenName) {
        mTwitterScreenName = twitterScreenName;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contact_summary, container, false);
        SQLiteDatabase db = new DBHelper(getActivity()).getReadableDatabase();
        Cursor cursor = db.query("keywords", new String[]{"keyword"}, "userId=?", new String[]{mUserId}, null, null, null);
        if (cursor.moveToFirst()) {
            StringBuilder builder = new StringBuilder();
            do {
                String string = cursor.getString(cursor.getColumnIndex("keyword"));
                if (string.length() > 2 && string.length() < 20) {
                    builder.append(string).append("      ");
                }
            } while (cursor.moveToNext());
            mKeywordText = builder.toString();
            ((TextView) view.findViewById(R.id.keywordTexts)).setText(mKeywordText);
        }
        db.close();
        view.findViewById(R.id.sendToWearable).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SQLiteDatabase db = new DBHelper(getActivity()).getReadableDatabase();
                Cursor cursor = db.query("users", new String[]{"name"}, "forceUserId=?", new String[]{mUserId},
                        null, null, null);
                cursor.moveToFirst();
                String name = cursor.getString(cursor.getColumnIndex("name"));
                db.close();

                // Simply sending notification for now.
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getActivity());
                notificationManager.notify(001, new NotificationCompat.Builder(getActivity())
                        .setContentTitle("Keywords of " +  name )
                        .setContentText(mKeywordText)
                        .setSmallIcon(R.drawable.sf__icon)
                        .build());
            }
        });
        loadTwitterImages(view, mTwitterScreenName);
        return view;
    }


    private void loadTwitterImages(final View parent, final String screenName) {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setOAuthConsumerKey(BuildConfig.TWITTER_CONSUMER_KEY);
        builder.setOAuthConsumerSecret(BuildConfig.TWITTER_CONSUMER_SECRET);
        builder.setOAuthAccessToken(Preferences.getDefault(getActivity()).getString(Preferences.KEY_TWITTER_OAUTH_TOKEN, ""));
        builder.setOAuthAccessTokenSecret(Preferences.getDefault(getActivity()).getString(Preferences.KEY_TWITTER_OAUTH_SECRET, ""));
        builder.setIncludeEntitiesEnabled(true);
        Configuration configuration = builder.build();
        TwitterFactory factory = new TwitterFactory(configuration);
        final Twitter twitter = factory.getInstance();

        new AsyncTask<Void, Void, List<String>>() {
            @Override
            protected List<String> doInBackground(Void... params) {
                List<String> urls = new ArrayList<String>();
                try {
                    List<twitter4j.Status> statuses = twitter.getUserTimeline(screenName);
                    for (twitter4j.Status status: statuses) {
                        if (status.getMediaEntities().length > 0) {
                            MediaEntity entity = status.getMediaEntities()[0];
                            urls.add(entity.getMediaURL());
                        }
                    }
                } catch (TwitterException e) {
                    // TODO
                    return urls;
                }
                return urls;
            }

            @Override
            protected void onPostExecute(List<String> urls) {
                if (urls != null) {
                    int count = 0;
                    for (String url: urls) {
                        Picasso.with(getActivity()).load(url).placeholder(R.drawable.hoge)
                                .error(R.drawable.g2013).into((ImageView) parent.findViewById(imageViewId(count)));
                        count++;
                        if (count == 3) {
                            return;
                        }
                    }
                }
                super.onPostExecute(urls);
            }
        }.execute();
    }

    private int imageViewId(int count) {
        if (count == 0) {
            return R.id.image01;
        } else if (count == 1) {
            return R.id.image02;
        } else if (count == 2) {
            return R.id.image03;
        }
        return 0;
    }
}

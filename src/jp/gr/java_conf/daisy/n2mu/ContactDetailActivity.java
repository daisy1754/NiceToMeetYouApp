package jp.gr.java_conf.daisy.n2mu;


import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.viewpagerindicator.TabPageIndicator;

import java.util.Set;

public class ContactDetailActivity extends FragmentActivity {
    public static final String EXTRA_KEY_USER_ID = "contactDetail:extra:userId";
    private KeywordHelper mKeywordHelper;
    private String mUserId;
    private String mName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_detail);

        getActionBar().setDisplayShowHomeEnabled(false);
        getActionBar().setTitle("Profile");
        // Fetch user info from local DB
        SQLiteDatabase db = new DBHelper(this).getReadableDatabase();
        mUserId = getIntent().getStringExtra(EXTRA_KEY_USER_ID);
        Cursor cursor = db.query("users",
                new String[]{"forceUserId, name, iconUrl, company, linkedInId, twitterId, twitterScreenName, gotKeywordFromLinkedIn, gotKeywordFromTwitter"},
                "forceUserId=?", new String[]{mUserId}, null, null, null);
        mKeywordHelper = new KeywordHelper(this);
        if (cursor.moveToFirst()) {
            mName = cursor.getString(cursor.getColumnIndex("name"));
            initHeaderView(
                    cursor.getString(cursor.getColumnIndex("iconUrl")),
                    mName,
                    cursor.getString(cursor.getColumnIndex("company")));

//            // Ask for SNS Ids
//            String[] keys = new String[] {"linkedInId", "twitterScreenName"};
//            for (String key: keys) {
//                if (cursor.getString(cursor.getColumnIndex(key)) == null) {
//                    showTellMeDataDialog(key);
//                }
//            }
//
//            String linkedinId = cursor.getString(cursor.getColumnIndex("linkedInId"));
//            if (cursor.getInt(cursor.getColumnIndex("gotKeywordFromLinkedIn")) <= 0 && linkedinId != null) {
//                mKeywordHelper.fetchKeywordWithLinkedInId(linkedinId, new KeywordHelper.OnKeywordObtainedListener() {
//                    @Override
//                    public void keywordObtained(Set<String> keywords) {
//
//                    }
//                });
//            }
        }
        db.close();

        ViewPager pager = (ViewPager)findViewById(R.id.pager);
        pager.setAdapter(new ContactInfoAdapter(getSupportFragmentManager()));
        TabPageIndicator tabIndicator = (TabPageIndicator)findViewById(R.id.titles);
        tabIndicator.setViewPager(pager);

        ImageView fab = (ImageView) findViewById(R.id.postReportButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: apply effect to fab
                Intent intent = new Intent(ContactDetailActivity.this, NewReportActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void initHeaderView(String iconUrl, String name, String company) {
        final ImageView avatarImageView = (ImageView) findViewById(R.id.avatarImage);
        Picasso.with(this).load(iconUrl).placeholder(R.drawable.hoge)
                .transform(new RoundTransformation())
                .error(R.drawable.g2013).into(avatarImageView);
        ((TextView) findViewById(R.id.nameText)).setText(name);
        ((TextView) findViewById(R.id.titleText)).setText(company);
    }

    private class ContactInfoAdapter extends FragmentPagerAdapter {
        private final String[] CONTENT = new String[] { "Summary", "Linkedin", "Twitter" };
        private int mCount = CONTENT.length;

        public ContactInfoAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                ContactSummaryFragment fragment = new ContactSummaryFragment();
                fragment.setUserId(mUserId);
                return fragment;
            } else if (position == 1) {
                WebViewFragment fragment = WebViewFragment.newInstance(WebViewFragment.VIEW_TYPE_LINKEDIN, mUserId);
                fragment.loadUrl("https://www.linkedin.com/pub/wilson-assis-o-hora/4/718/4ab");
                return fragment;
            } else if (position == 2) {
                WebViewFragment fragment = WebViewFragment.newInstance(WebViewFragment.VIEW_TYPE_TWITTER, mUserId);
                fragment.loadUrl("https://twitter.com/" + "ushikusamaru");
                return fragment;
            } else {
                throw new IllegalStateException("Unknown tab position");
            }
        }

        @Override
        public int getCount() {
            return mCount;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return CONTENT[position];
        }

        public void setCount(int count) {
            if (count > 0 && count <= 10) {
                mCount = count;
                notifyDataSetChanged();
            }
        }
    }

    // TODO: implement search functionality
    // Note: We cannot implement search for linkedin at this moment because I need get approved,
    // I applied, but they said approval process takes 15 days.
    private void showTellMeDataDialog(final String key) {
        final EditText input = new EditText(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Please tell me " + key + " of " + mName)
                .setView(input)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = input.getText().toString();
                        if (value.length() > 0) {
                            SQLiteDatabase db = DBHelper.getWritableDatabase(ContactDetailActivity.this);
                            ContentValues values = new ContentValues();
                            values.put(key, value);
                            db.update("users", values, "forceUserId=?", new String[]{mUserId});
                            Toast.makeText(ContactDetailActivity.this, key + " is updated", Toast.LENGTH_SHORT).show();
                            db.close();
                        }
                    }
                });
        if (key.equals("linkedInId")) {
            builder.setMessage("Note: linkedInId is trickey one. For API call, we have to use diffrent Id than" +
                    "that is shown on linkedIn web page or url. So, you should wait until linkedin " +
                    "team allow me to implement search");
        }

        builder.show();
    }
}

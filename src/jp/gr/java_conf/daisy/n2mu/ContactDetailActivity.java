package jp.gr.java_conf.daisy.n2mu;


import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.viewpagerindicator.TabPageIndicator;
import com.viewpagerindicator.TitlePageIndicator;

import java.util.HashSet;
import java.util.Set;

public class ContactDetailActivity extends FragmentActivity {
    public static final String EXTRA_KEY_USER_ID = "contactDetail:extra:userId";
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
                new String[]{"forceUserId, name, iconUrl, company, linkedinId, twitterId, twitterScreenName"},
                "forceUserId=?", new String[]{mUserId}, null, null, null);
        if (cursor.moveToFirst()) {
            mName = cursor.getString(cursor.getColumnIndex("name"));
            initHeaderView(
                    cursor.getString(cursor.getColumnIndex("iconUrl")),
                    mName,
                    cursor.getString(cursor.getColumnIndex("company")));

        }

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

    private class ContactInfoAdapter extends FragmentPagerAdapter {
        private final String[] CONTENT = new String[] { "Summary", "Linkedin", "Twitter" };
        private int mCount = CONTENT.length;

        public ContactInfoAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                return new ContactSummaryFragment();
            } else if (position == 1) {
                return WebViewFragment.newInstance(WebViewFragment.VIEW_TYPE_LINKEDIN);
            } else if (position == 2) {
                return WebViewFragment.newInstance(WebViewFragment.VIEW_TYPE_TWITTER);
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
}

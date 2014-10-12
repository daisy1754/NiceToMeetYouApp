package jp.gr.java_conf.daisy.n2mu;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.salesforce.androidsdk.app.SalesforceSDKManager;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestClient.AsyncRequestCallback;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;
import com.salesforce.androidsdk.ui.sfnative.SalesforceActivity;
import com.squareup.picasso.Picasso;

import jp.gr.java_conf.daisy.n2mu.debug.PrefDumpActivity;
import jp.gr.java_conf.daisy.n2mu.debug.SQLDumpActivity;
import jp.gr.java_conf.daisy.n2mu.setup.AuthWithLinkedinActivity;
import jp.gr.java_conf.daisy.n2mu.setup.TwitterAuthActivity;

/**
 * Main activity
 */
public class MainActivity extends SalesforceActivity {
    // flag for debugging
    boolean uiDebug = true;

    private static final String ISO_8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private final int mMonth;
    private final int mDay;
    private RestClient mClient;
    private ProgressDialog mProgressDialog;
    private Map<Date, List<String>> mDateToContactId;
    private Map<String, Contact> mIdToContact;
    private boolean mLoadingFired;
    private String mBaseUrl;

    public MainActivity() {
        super();
        Date date = new Date();
        mMonth = date.getMonth();
        mDay = date.getDay();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        String linkedinToken = Preferences.getDefault(this).getString(Preferences.KEY_LINKEDIN_ACCESS_TOKEN, "");
        if (linkedinToken.length() == 0) { // I don't have token
            Intent intent = new Intent(this, AuthWithLinkedinActivity.class);
            startActivityForResult(intent, 0);
        }
        // TODO: token expire check

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setMessage("Loading Your Schedule...");
        mProgressDialog.show();

        setTitle("Upcoming Appointments");
        getActionBar().setDisplayShowHomeEnabled(false);
    }

    @Override
    public void onResume(RestClient client) {
        mClient = client;
        mBaseUrl = mClient.getClientInfo().getInstanceUrlAsString();
        if (mLoadingFired) {
            return;
        }
        mLoadingFired = true;
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                fetchIncomingEvent();
            }
        }, 1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_search:
                // TODO:
                Toast.makeText(this, "Not implemented yet :)", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.action_debug_sql: {
                Intent intent = new Intent(this, SQLDumpActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.action_debug_sharedpref: {
                Intent intent = new Intent(this, PrefDumpActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.action_debug_twitter_auth: {
                Intent intent = new Intent(this, TwitterAuthActivity.class);
                startActivityForResult(intent, 0);
                return true;
            }
            case R.id.action_debug_linkedinAuth: {
                Intent intent = new Intent(this, AuthWithLinkedinActivity.class);
                startActivityForResult(intent, 0);
                return true;
            }
            case R.id.action_debug_keywords_lookup_with_linkedin_id: {
                final EditText input = new EditText(this);
                new AlertDialog.Builder(this)
                        .setTitle("Input linkedin Id (like '1x8UyU9s7Y')")
                        .setView(input)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                String value = input.getText().toString();
                                if (value.length() > 0) {
                                    new KeywordHelper(MainActivity.this).fetchKeywordWithLinkedInId(value, new KeywordHelper.OnKeywordObtainedListener() {
                                        @Override
                                        public void keywordObtained(Set<String> keywords) {
                                            new AlertDialog.Builder(MainActivity.this)
                                                    .setMessage(keywords.toString())
                                                    .show();
                                        }
                                    });
                                }
                            }
                        }).show();
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void fetchIncomingEvent() {
        RestRequest restRequest
                = getRequestForQuery("SELECT StartDateTime, WhoId FROM Event Where StartDateTime >= TODAY");
        mClient.sendAsync(restRequest, new AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, RestResponse result) {
                try {
                    mProgressDialog.setMessage("Loading Your Schedule.........");
                    JSONArray records = result.asJSONObject().getJSONArray("records");
                    List<String> contactIds = new ArrayList<String>();
                    DateFormat dateFormat = new SimpleDateFormat(ISO_8601_FORMAT, Locale.getDefault());
                    mDateToContactId = new HashMap<Date, List<String>>();
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject json = records.getJSONObject(i);
                        String contactId = json.getString("WhoId");
                        contactIds.add(contactId);
                        try {
                            Date date = dateFormat.parse(json.getString("StartDateTime"));
                            if (!mDateToContactId.containsKey(date)) {
                                List<String> idList = new ArrayList<String>();
                                mDateToContactId.put(date, idList);
                            }
                            mDateToContactId.get(date).add(contactId);
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    fetchContact(contactIds);
                } catch (JSONException e) {
                    onError(e);
                } catch (IOException e) {
                    onError(e);
                }
            }

            @Override
            public void onError(Exception exception) {
                showErrorToast(exception);
            }
        });
    }

    private void fetchContact(final List<String> contactIds) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT Id,Name,Title,Department,PhotoUrl,Description FROM Contact Where Id IN (");
        for (int i = 0; i < contactIds.size(); i++) {
            queryBuilder.append("'").append(contactIds.get(i)).append("'");
            if (i != contactIds.size() - 1) {
                queryBuilder.append(",");
            } else {
                queryBuilder.append(")");
            }
        }
        RestRequest restRequest = getRequestForQuery(queryBuilder.toString());
        mClient.sendAsync(restRequest, new AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, RestResponse result) {
                try {
                    JSONArray records = result.asJSONObject().getJSONArray("records");
                    mIdToContact = new HashMap<String, Contact>();
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject contact = records.getJSONObject(i);
                        String description = contact.getString("Description");
                        String linkedinUrl = "";
                        String twitterScreenName = "";
                        if (description != null) {
                            String[] arr = description.split("\n");
                            if (arr.length > 1 && arr[0].split(":", 2).length == 2&& arr[1].split(":", 2).length == 2) {
                                linkedinUrl = arr[0].split(":", 2)[1].trim();
                                twitterScreenName = arr[1].split(":", 2)[1].trim();
                            }
                        }

                        mIdToContact.put(contact.getString("Id"),
                                new Contact(
                                        contact.getString("Id"),
                                        contact.getString("Name"),
                                        contact.get("Title") == null ? "" : contact.getString("Title"),
                                        contact.getString("PhotoUrl"),
                                        linkedinUrl,
                                        twitterScreenName));
                    }
                    updateContactTable();
                    mProgressDialog.dismiss();
                    ListView contactList = (ListView) findViewById(R.id.contacts_list);
                    final EventContactAdapter adapter = new EventContactAdapter(MainActivity.this, mDateToContactId);
                    contactList.setAdapter(adapter);
                    contactList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            Object item = adapter.getItem(position);
                            if (item instanceof Contact) {
                                Intent intent = new Intent(MainActivity.this, ContactDetailActivity.class);
                                intent.putExtra(ContactDetailActivity.EXTRA_KEY_USER_ID, ((Contact) item).getUserId());
                                startActivity(intent);
                            }
                        }
                    });
                    contactList.setDivider(null);
                } catch (JSONException e) {
                    onError(e);
                } catch (IOException e) {
                    onError(e);
                }
            }

            @Override
            public void onError(Exception exception) {
                showErrorToast(exception);
            }
        });
    }

    private void updateContactTable() {
        SQLiteDatabase db = DBHelper.getWritableDatabase(this);
        Set<String> userIds = new HashSet<String>(mIdToContact.keySet());
        String query = "forceUserId IN (?";
        for (int i = 1; i < userIds.size(); i++) {
            query += ",?";
        }
        query += ")";
        Cursor cursor = db.query("users", new String[]{"forceUserId"}, query, userIds.toArray(new String[0]), null, null, null);
        if (cursor.moveToFirst()) {
            do {
                userIds.remove(cursor.getString(cursor.getColumnIndex("forceUserId")));
            } while (cursor.moveToNext());
        }
        for (String userId: userIds) {
            ContentValues values = new ContentValues();
            values.put("forceUserId", userId);
            Contact contact = mIdToContact.get(userId);
            values.put("iconUrl", contact.getUrl());
            values.put("name", contact.getName());
            values.put("company", contact.getTitle());
            values.put("linkedInUrl", contact.getLinkedInUrl());
            values.put("twitterScreenName", contact.getTwitterScreenName());
            db.insert("users", "", values);
        }
    }

    private RestRequest getRequestForQuery(String query) {
        try {
            return RestRequest.getRequestForQuery(getString(R.string.api_version), query);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private void showErrorToast(Exception exception) {
        String errorString = getString(SalesforceSDKManager.getInstance().getSalesforceR().stringGenericError(), exception.toString());
        Toast.makeText(MainActivity.this,
                errorString,
                Toast.LENGTH_LONG).show();
        Log.e("NiceToMeetYou", errorString);
    }

    private class EventContactAdapter extends ArrayAdapter<Object> {
        private final int VIEW_TYPE_HEADER = 0;
        private final int VIEW_TYPE_CONTACT = 1;
        private List<Object> mDateAndContacts = new ArrayList<Object>();
        private List<Integer> mSectionHeader = new ArrayList<Integer>();

        public EventContactAdapter(Context context, Map<Date, List<String>> dateToContactId) {
            super(context, R.layout.contact_list_item);
            List<Date> dates = new ArrayList<Date>(dateToContactId.keySet());
            Collections.sort(dates);

            for (int i = 0; i < dates.size(); i++) {
                mSectionHeader.add(mDateAndContacts.size());
                mDateAndContacts.add(dates.get(i));
                for (String contactId: dateToContactId.get(dates.get(i))) {
                    mDateAndContacts.add(mIdToContact.get(contactId));
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            return mSectionHeader.contains(position) ? VIEW_TYPE_HEADER : VIEW_TYPE_CONTACT;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getCount() {
            return mDateAndContacts.size();
        }

        @Override
        public Object getItem(int position) {
            return mDateAndContacts.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position,  View convertView, ViewGroup parent) {
            Object item = getItem(position);
            if (mSectionHeader.contains(position)) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.contact_date_item, null);
                Date date = (Date) item;
                int month = date.getMonth();
                int day = date.getDay();
                String dateFormat;
                if (month == mMonth && day == mDay) {
                    dateFormat = "'Today, 'HH:mm";
                } else if (month == mMonth && day == mDay + 1) { // TODO: I know, I need to deal with corner cases...
                    dateFormat = "'Tomorrow, 'HH:mm";
                } else {
                    dateFormat = "MMM dd, HH:mm";
                }
                ((TextView) convertView).setText(new SimpleDateFormat(dateFormat).format(date));
            } else {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.contact_list_item, null);
                Contact contactItem = (Contact) item;
                ((TextView) convertView.findViewById(R.id.nameText)).setText(contactItem.getName());
                ((TextView) convertView.findViewById(R.id.titleText)).setText(contactItem.getTitle());
                if (contactItem.getUrl() != null) {
                    String url = mBaseUrl + contactItem.getUrl() + "?oauth_token=" + mClient.getAuthToken();
                    if (uiDebug) {
                        url = contactItem.getUrl();
                    }
                    final ImageView avatarImageView = (ImageView) convertView.findViewById(R.id.avatarImage);
                    Picasso.with(MainActivity.this).load(url).placeholder(R.drawable.hoge)
                            .transform(new RoundTransformation())
                            .error(R.drawable.g2013).into(avatarImageView);
                }
            }
            return convertView;
        }
    }

    private class Contact {
        private final String mUserId;
        private final String mName;
        private final String mTitle;
        private final String mUrl;
        private final String mLinkedInUrl;
        private final String mTwitterScreenName;

        public Contact(String userId, String name, String title, String url, String linkedInUrl, String twitterScreenName) {
            mUserId = userId;
            mName = name;
            mTitle = title.equals("null") ? "" : title;
            mUrl = (url != null && url.equals("null")) ? null : url;
            mLinkedInUrl = linkedInUrl;
            mTwitterScreenName = twitterScreenName;
        }

        public String getUserId() {
            return mUserId;
        }

        public String getName() {
            return mName;
        }

        public String getTitle() {
            return mTitle;
        }

        public String getUrl() {
            return mUrl;
        }

        public String getLinkedInUrl() {
            return mLinkedInUrl;
        }

        public String getTwitterScreenName() {
            return mTwitterScreenName;
        }
    }
}

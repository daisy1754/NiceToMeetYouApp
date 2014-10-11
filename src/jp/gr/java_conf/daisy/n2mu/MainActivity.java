/*
 * Copyright (c) 2012, salesforce.com, inc.
 * All rights reserved.
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * - Neither the name of salesforce.com, inc. nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission of salesforce.com, inc.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package jp.gr.java_conf.daisy.n2mu;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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

/**
 * Main activity
 */
public class MainActivity extends SalesforceActivity {
    private static final String ISO_8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private RestClient mClient;
    private ProgressDialog mProgressDialog;
    private Map<Date, List<String>> mDateToContactId;
    private Map<String, Contact> mIdToContact;
    private boolean mLoadingFired;
    private String mBaseUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setTitle("Loading Your Schedule...");
        mProgressDialog.show();
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

    private void fetchIncomingEvent() {
        RestRequest restRequest
                = getRequestForQuery("SELECT StartDateTime, WhoId FROM Event Where StartDateTime >= TODAY");
        mClient.sendAsync(restRequest, new AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, RestResponse result) {
                try {
                    mProgressDialog.setTitle("Loading Your Schedule.........");
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
        Log.d("TokenToken", mClient.getAuthToken());
    }

    private void fetchContact(List<String> contactIds) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT Id,Name,Title,Department,PhotoUrl FROM Contact Where Id IN (");
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
                        mIdToContact.put(contact.getString("Id"),
                                new Contact(
                                        contact.getString("Name"),
                                        contact.get("Title") == null ? "" : contact.getString("Title"),
                                        contact.getString("PhotoUrl")));
                    }
                    mProgressDialog.dismiss();
                    ((ListView) findViewById(R.id.contacts_list)).setAdapter(
                            new EventContactAdapter(MainActivity.this, mDateToContactId));
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
            for (Map.Entry<Date, List<String>> entry: dateToContactId.entrySet()) {
                mSectionHeader.add(mDateAndContacts.size());
                mDateAndContacts.add(entry.getKey());
                for (String contactId: entry.getValue()) {
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
                convertView = new TextView(MainActivity.this);
                ((TextView) convertView).setText(item.toString());
            } else {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.contact_list_item, null);
                Contact contactItem = (Contact) item;
                ((TextView) convertView.findViewById(R.id.nameText)).setText(contactItem.getName());
                ((TextView) convertView.findViewById(R.id.titleText)).setText(contactItem.getTitle());
                if (contactItem.getUrl() != null) {
                    final String url = mBaseUrl + contactItem.getUrl() + "?oauth_token=" + mClient.getAuthToken();
                    final ImageView avatarImageView = (ImageView) convertView.findViewById(R.id.avatarImage);
                    Picasso.with(MainActivity.this).load(url).placeholder(R.drawable.hoge)
                            .error(R.drawable.g2013).into(avatarImageView);

                }
            }
            return convertView;
        }
    }

    private class Contact {
        private final String mName;
        private final String mTitle;
        private final String mUrl;

        public Contact(String name, String title, String url) {
            mName = name;
            mTitle = title.equals("null") ? "" : title;
            mUrl = (url != null && url.equals("null")) ? null : url;
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
    }
}

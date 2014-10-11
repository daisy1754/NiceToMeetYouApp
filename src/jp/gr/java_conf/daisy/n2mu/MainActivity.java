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

import java.io.IOError;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.salesforce.androidsdk.app.SalesforceSDKManager;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestClient.AsyncRequestCallback;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;
import com.salesforce.androidsdk.ui.sfnative.SalesforceActivity;

/**
 * Main activity
 */
public class MainActivity extends SalesforceActivity {

    private RestClient mClient;
    private ArrayAdapter<String> mListAdapter;
    private ProgressDialog mProgressDialog;
    private Map<String, Object> mIdToPeople;
    private boolean loadingFired;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setTitle("Loading Your Schedule...");

        mListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<String>());
        ((ListView) findViewById(R.id.contacts_list)).setAdapter(mListAdapter);
    }

    @Override
    public void onResume(RestClient client) {
        mClient = client;
        if (loadingFired) {
            return;
        }
        loadingFired = true;
        fetchIncomingEvent();
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
                    for (int i = 0; i < records.length(); i++) {
                        JSONObject json = records.getJSONObject(i);
                        contactIds.add(json.getString("WhoId"));
                        mListAdapter.add(json.getString("StartDateTime") + json.getString("WhoId"));
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

    private void fetchContact(List<String> contactIds) {
        // TODO
        mProgressDialog.dismiss();
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
}

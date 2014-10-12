package jp.gr.java_conf.daisy.n2mu;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;

import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.ui.sfnative.SalesforceActivity;

public class NewReportActivity extends SalesforceActivity {
    private RestClient mClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_report);

        getActionBar().setTitle("Add note");
        getActionBar().setDisplayShowHomeEnabled(false);
        ((EditText) findViewById(R.id.title)).setText("Meeting report with Joey");
    }

    @Override
    public void onResume(RestClient client) {
        mClient = client;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.new_report_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_submit:
                ProgressDialog dialog = new ProgressDialog(NewReportActivity.this);
                dialog.setMessage("Not implemented");
                dialog.setIndeterminate(true);
                dialog.show();

                // TODO: implement

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

package jp.gr.java_conf.daisy.n2mu.debug;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;

import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jp.gr.java_conf.daisy.n2mu.Preferences;
import jp.gr.java_conf.daisy.n2mu.R;

public class PrefDumpActivity extends Activity {
    private static final String[] PREF_NAMES = new String[] {Preferences.PREFERENCES_NAME};
    private TextView titleView;
    private BaseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.debug_dump_content_activity);
        Spinner table = (Spinner) findViewById(R.id.debug_item_names);
        titleView = (TextView) findViewById(R.id.debug_item_title);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, getDumpTargetNames());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        final ListView listView =(ListView) findViewById(R.id.debug_item_content);
        table.setAdapter(adapter);
        table.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Spinner spinner = (Spinner) parent;
                String item = spinner.getSelectedItem().toString();
                final BaseAdapter adapter = getAdapter(item);
                listView.setAdapter(adapter);
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        PrefDumpActivity.this.onItemClick(adapter.getItem(position));
                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    protected void setTitle(String title) {
        titleView.setText(title);
    }

    private String[] getDumpTargetNames() {
        return PREF_NAMES;
    }

    private BaseAdapter getAdapter(String targetName) {
        SharedPreferences prefs;
        if (targetName == null || targetName.length() == 0) {
            prefs = getPreferences(Context.MODE_PRIVATE);
        } else {
            prefs = getSharedPreferences(targetName, Context.MODE_PRIVATE);
        }
        List<KeyValue> keyValues = new ArrayList<KeyValue>();
        Map<String,?> keys = prefs.getAll();
        for(Map.Entry<String,?> entry : keys.entrySet()){
            keyValues.add(new KeyValue(entry.getKey(), entry.getValue()));
        }
        adapter = new ArrayAdapter<KeyValue>(this, android.R.layout.simple_list_item_1, keyValues);
        return adapter;
    }

    private void onItemClick(Object selectedItem) {
        final KeyValue selected = (KeyValue) selectedItem;
        AlertDialog.Builder builder =  new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle(selected.key)
                .setNeutralButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getSharedPreferences("UserData", Context.MODE_PRIVATE).edit()
                                .remove(selected.key)
                                .commit();
                        Toast.makeText(
                                PrefDumpActivity.this,
                                "Try to remove preference with key '" + selected.key + "'",
                                Toast.LENGTH_SHORT).show();
                        adapter.notifyDataSetChanged();
                    }
                });
        if (selected.value instanceof String) {
            final EditText editView = new EditText(this);
            editView.setText(selected.value.toString());
            builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    getSharedPreferences("UserData", Context.MODE_PRIVATE).edit()
                            .putString(selected.key, editView.getText().toString())
                            .commit();
                    Toast.makeText(
                            PrefDumpActivity.this,
                            "Try to rewrite " + selected.key + " to " + editView.getText(),
                            Toast.LENGTH_SHORT).show();
                    adapter.notifyDataSetChanged();
                }
            });
            builder.setView(editView);
        } else {
            TextView textView = new TextView(this);
            textView.setGravity(Gravity.CENTER);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24.f);
            textView.setText(selected.value.toString());
            builder.setView(textView);
            if (selected.value instanceof Boolean) {
                builder.setPositiveButton("Negate", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        getSharedPreferences("UserData", Context.MODE_PRIVATE).edit()
                                .putBoolean(selected.key, !(Boolean)selected.value)
                                .commit();
                        Toast.makeText(
                                PrefDumpActivity.this,
                                "Negate",
                                Toast.LENGTH_SHORT).show();
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        }
        builder.show();
    }

    public class KeyValue {
        private final String key;
        private final Object value;

        public KeyValue(String key, Object value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
            return key + ": " + value;
        }
    }
}

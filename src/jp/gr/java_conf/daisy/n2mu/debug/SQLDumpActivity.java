package jp.gr.java_conf.daisy.n2mu.debug;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import jp.gr.java_conf.daisy.n2mu.DBHelper;
import jp.gr.java_conf.daisy.n2mu.R;

public class SQLDumpActivity extends Activity {

    private static final String[] TABLE_NAMES = new String[] {"users", "keywords"};
    private TextView titleView;
    private String table;

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
                        SQLDumpActivity.this.onItemClick(adapter.getItem(position));
                    }
                });

                dumpAdapterContent(adapter);
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
        return TABLE_NAMES;
    }

    private BaseAdapter getAdapter(String targetName) {
        DBHelper dbHelper = new DBHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        table = targetName;
        Cursor cursor = db.query(table, null, null, null, null, null, null);
        setTitle(cursor);
        return new CursorAdapter(this, cursor, true) {
            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent) {
                return new TextView(context);
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                ((TextView) view).setText(columnContent(cursor));
            }
        };
    }

    protected void onItemClick(Object selectedItem) {
        final Cursor cursor = (Cursor) selectedItem;
        final String whereClause = whereClauseForCurrentCursor(cursor);

        LinearLayout layout = new LinearLayout(this);
        final Spinner spinner = new Spinner(this);
        final EditText editView = new EditText(this);
        layout.addView(spinner);
        layout.addView(editView);

        final String[] columnNames = cursor.getColumnNames();
        ArrayAdapter adapter =new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, columnNames);
        spinner.setAdapter(adapter);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                editView.setText(cursor.getString(cursor.getColumnIndex(columnNames[position])));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle("Modify data")
                .setView(layout)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        DBHelper dbHelper = new DBHelper(SQLDumpActivity.this);
                        SQLiteDatabase db = dbHelper.getWritableDatabase();
                        ContentValues values = new ContentValues();
                        values.put(spinner.getSelectedItem().toString(), editView.getText().toString());
                        int numUpdated = db.update(table, values, whereClause, null);
                        Toast.makeText(SQLDumpActivity.this, numUpdated + " entries updated", Toast.LENGTH_SHORT).show();
                        Log.d("photo update table", whereClause);
                    }
                })
                .show();
    }

    private void setTitle(Cursor cursor) {
        int numOfColumns = cursor.getColumnCount();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < numOfColumns; i++) {
            builder.append(cursor.getColumnName(i)).append("| ");
        }
        setTitle(builder.toString());
    }

    private String columnContent(Cursor c) {
        int numOfColumns = c.getColumnCount();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < numOfColumns; i++) {
            builder.append(c.getString(i)).append("| ");
        }
        return builder.toString();
    }

    private String whereClauseForCurrentCursor(Cursor cursor) {
        int numOfColumns = cursor.getColumnCount();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < numOfColumns; i++) {
            if (cursor.getString(i) == null) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(" AND ");
            }
            builder.append(cursor.getColumnName(i)).append("=='").append(cursor.getString(i)).append("'");
        }
        return builder.toString();
    }

    private void dumpAdapterContent(BaseAdapter adapter) {
        StringBuilder builder = new StringBuilder();
        int count = adapter.getCount();
        for (int i = 0; i < count; i++) {
            builder.append(columnContent((Cursor) adapter.getItem(i))).append("\n");
            if (builder.length() > 1024) {
                Log.d("DBAdapterDump", builder.toString());
                builder.setLength(0);
            }
        }
        Log.d("DUMP", builder.toString());
    }
}


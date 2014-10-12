package jp.gr.java_conf.daisy.n2mu;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class ContactSummaryFragment extends Fragment {
    private String mUserId;
    private String mKeywordText;

    public void setUserId(String userId) {
        mUserId = userId;
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
        return view;
    }
}

package jp.gr.java_conf.daisy.n2mu;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ContactSummaryFragment extends Fragment {
    private String mUserId;

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
            ((TextView) view.findViewById(R.id.keywordTexts)).setText(builder.toString());
        }
        db.close();
        return view;
    }
}

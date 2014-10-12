package jp.gr.java_conf.daisy.n2mu;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    private static final int VERSION = 1;
    public DBHelper(Context context) {
        super(context, "n2mu.db", null, VERSION);
    }

    public static SQLiteDatabase getWritableDatabase(Context context) {
        return new DBHelper(context).getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createUserTable(db);
        createKeywordsTable(db);
    }

    private void createUserTable(SQLiteDatabase db) {
        StringBuffer sql = new StringBuffer();
        sql.append("create table users(");
        sql.append("_id integer primary key autoincrement not null,");
        sql.append("forceUserId text not null unique,");
        sql.append("iconUrl text,");
        sql.append("linkedInId text,");
        sql.append("twitterId text,");
        sql.append("twitterScreenName text");
        sql.append(")");
        db.execSQL(sql.toString());
    }

    private void createKeywordsTable(SQLiteDatabase db) {
        StringBuffer sql = new StringBuffer();
        sql.append("create table keywords(");
        sql.append("_id integer primary key autoincrement not null,");
        sql.append("userId text not null,");
        sql.append("keyword text not null");
        sql.append(")");
        db.execSQL(sql.toString());
    }

    private void createPhotoTable(SQLiteDatabase db){
        StringBuffer sql = new StringBuffer();
        sql.append("create table photo_table(");
        sql.append("_id integer primary key autoincrement not null,");
        sql.append("photoId text not null unique,");
        sql.append("albumId text not null,");
        sql.append("uploadedStatus numerical,"); // 0 for not uploaded, 1 for succeed, and -1 for failed (need retry).
        sql.append("isDeleted numerical,");
        sql.append("createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,");
        sql.append("takenBy text,");
        sql.append("tickCountOfCreatedAt numerical");
        sql.append(")");
        db.execSQL(sql.toString());
    }

    private void createAlbumTable(SQLiteDatabase db) {
        StringBuffer sql = new StringBuffer();
        sql.append("create table album_table(");
        sql.append("_id integer primary key autoincrement not null,");
        sql.append("albumId text not null unique,");
        sql.append("color text,");
        sql.append("displayName text not null,");
        sql.append("updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,");
        sql.append("tickCountOfUpdatedAt numerical,");
        sql.append("enabledNotification numerical DEFAULT 1,");
        sql.append("code text,");
        sql.append("codeExpireDate TIMESTAMP");
        sql.append(")");
        db.execSQL(sql.toString());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}

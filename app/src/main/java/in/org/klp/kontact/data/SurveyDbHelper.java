package in.org.klp.kontact.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObservable;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.ListView;

import in.org.klp.kontact.data.SurveyContract.SurveyEntry;

/**
 * Created by haris on 5/16/16.
 */
public class SurveyDbHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;

    static final String DATABASE_NAME = "kontact.db";

    public SurveyDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        final String SQL_CREATE_SURVEY_TABLE = "CREATE TABLE " + SurveyEntry.TABLE_NAME + " (" +
                SurveyEntry._ID + " INTEGER PRIMARY KEY," +
                SurveyEntry.COLUMN_STATUS + " INTEGER, " +
                SurveyEntry.COLUMN_CREATED_AT + " INTEGER NOT NULL, " +
                SurveyEntry.COLUMN_PARTNER + " TEXT, " +
                SurveyEntry.COLUMN_NAME + " TEXT " +
                " );";
        sqLiteDatabase.execSQL(SQL_CREATE_SURVEY_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        onCreate(sqLiteDatabase);
    }

    public void insert_survey(int id, int status, int createdAt, String partner, String name) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(SurveyEntry._ID, id);
        contentValues.put(SurveyEntry.COLUMN_STATUS, status);
        contentValues.put(SurveyEntry.COLUMN_CREATED_AT, createdAt);
        contentValues.put(SurveyEntry.COLUMN_PARTNER, partner);
        contentValues.put(SurveyEntry.COLUMN_NAME, name);
        this.getWritableDatabase().insertOrThrow(SurveyEntry.TABLE_NAME,"",contentValues);
    }

    public void delete_survey(int id) {
        this.getWritableDatabase().delete(SurveyEntry.TABLE_NAME, SurveyEntry._ID + "=" + id, null);
    }

    public void update_survey(int id, int status) {
        this.getWritableDatabase().execSQL("UPDATE " +
                        SurveyEntry.TABLE_NAME + " SET " +
                        SurveyEntry.COLUMN_STATUS + " = " +
                        status + " WHERE " +
                        SurveyEntry._ID + " = " +
                        id
        );
    }

    public void list_surveys(ListView listView) {
        Cursor curosr = this.getReadableDatabase().rawQuery("SELECT * FROM " + SurveyEntry.TABLE_NAME, null);
    }
}

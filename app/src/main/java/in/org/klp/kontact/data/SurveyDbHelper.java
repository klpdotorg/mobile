package in.org.klp.kontact.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObservable;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.ListView;

import in.org.klp.kontact.data.SurveyContract.SurveyEntry;
import in.org.klp.kontact.data.SurveyContract.QuestionEntry;
import in.org.klp.kontact.data.SurveyContract.QuestiongroupEntry;
import in.org.klp.kontact.data.SurveyContract.QuestiongroupQuestionEntry;

import in.org.klp.kontact.data.SchoolContract.SchoolEntry;


public class SurveyDbHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;

    static final String DATABASE_NAME = "kontact.db";

    public SurveyDbHelper(Context context, String name, SQLiteDatabase.CursorFactory factory,
                          int version) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        final String SQL_CREATE_SURVEY_TABLE = "CREATE TABLE " + SurveyEntry.TABLE_NAME + " (" +
                SurveyEntry._ID + " INTEGER PRIMARY KEY," +
                SurveyEntry.COLUMN_PARTNER + " TEXT, " +
                SurveyEntry.COLUMN_NAME + " TEXT " +
                " );";

        final String SQL_CREATE_QUESTION_TABLE = "CREATE TABLE " + QuestionEntry.TABLE_NAME + " (" +
                QuestionEntry._ID + " INTEGER PRIMARY KEY," +
                QuestionEntry.COLUMN_TEXT + " TEXT, " +
                QuestionEntry.COLUMN_KEY + " TEXT, " +
                QuestionEntry.COLUMN_OPTIONS + " TEXT, " +
                QuestionEntry.COLUMN_TYPE + " TEXT, " +
                QuestionEntry.COLUMN_SCHOOL_TYPE + " INTEGER, " +
                " FOREIGN KEY (" + QuestionEntry.COLUMN_SCHOOL_TYPE + ") REFERENCES " +
                SchoolEntry.TABLE_NAME + " (" + SchoolEntry._ID + "));";

        final String SQL_CREATE_QUESTIONGROUP_TABLE = "CREATE TABLE " + QuestiongroupEntry.TABLE_NAME + " (" +
                QuestiongroupEntry._ID + " INTEGER PRIMARY KEY," +
                QuestiongroupEntry.COLUMN_STATUS + " INTEGER, " +
                QuestiongroupEntry.COLUMN_START_DATE + " INTEGER, " +
                QuestiongroupEntry.COLUMN_END_DATE + " INTEGER, " +
                QuestiongroupEntry.COLUMN_VERSION + " INTEGER, " +
                QuestiongroupEntry.COLUMN_SOURCE + " TEXT, " +
                QuestiongroupEntry.COLUMN_SURVEY + " INTEGER, " +
                " FOREIGN KEY (" + QuestiongroupEntry.COLUMN_SURVEY + ") REFERENCES " +
                SurveyEntry.TABLE_NAME + " (" + SurveyEntry._ID + "));";

        final String SQL_CREATE_QUESTIONGROUPQUESTION_TABLE = "CREATE TABLE " + QuestiongroupQuestionEntry.TABLE_NAME + " (" +
                QuestiongroupQuestionEntry._ID + " INTEGER PRIMARY KEY," +
                QuestiongroupQuestionEntry.COLUMN_SEQUENCE + " INTEGER, " +
                QuestiongroupQuestionEntry.COLUMN_QUESTION + " INTEGER, " +
                QuestiongroupQuestionEntry.COLUMN_QUESTIONGROUP + " INTEGER, " +
                " FOREIGN KEY (" + QuestiongroupQuestionEntry.COLUMN_QUESTION + ") REFERENCES " +
                QuestionEntry.TABLE_NAME + " (" + QuestionEntry._ID + "), " +
                " FOREIGN KEY (" + QuestiongroupQuestionEntry.COLUMN_QUESTIONGROUP + ") REFERENCES " +
                QuestiongroupEntry.TABLE_NAME + " (" + QuestiongroupEntry._ID + "));";

        sqLiteDatabase.execSQL(SQL_CREATE_SURVEY_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_QUESTION_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_QUESTIONGROUP_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_QUESTIONGROUPQUESTION_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + SurveyEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + QuestionEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }

    public void insert_survey(int id, String partner, String name) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(SurveyEntry._ID, id);
        contentValues.put(SurveyEntry.COLUMN_PARTNER, partner);
        contentValues.put(SurveyEntry.COLUMN_NAME, name);
        this.getWritableDatabase().insertOrThrow(SurveyEntry.TABLE_NAME,"",contentValues);
    }

    public void delete_survey(int id) {
        this.getWritableDatabase().delete(SurveyEntry.TABLE_NAME, SurveyEntry._ID + "=" + id, null);
    }


    public Cursor list_surveys() {
        Cursor cursor = this.getReadableDatabase().rawQuery("SELECT * FROM " + SurveyEntry.TABLE_NAME, null);
        return cursor;
    }
}

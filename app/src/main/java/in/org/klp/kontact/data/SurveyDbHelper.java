package in.org.klp.kontact.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObservable;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.BadParcelableException;
import android.util.Log;
import android.widget.ListView;

import in.org.klp.kontact.data.SurveyContract.SurveyEntry;
import in.org.klp.kontact.data.SurveyContract.QuestionEntry;
import in.org.klp.kontact.data.SurveyContract.QuestiongroupEntry;
import in.org.klp.kontact.data.SurveyContract.QuestiongroupQuestionEntry;

import in.org.klp.kontact.data.SchoolContract.SchoolEntry;
import in.org.klp.kontact.data.SchoolContract.BoundaryEntry;


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
                SurveyEntry.COLUMN_PARTNER + " TEXT, " +
                SurveyEntry.COLUMN_NAME + " TEXT " +
                " );";

        final String SQL_CREATE_QUESTION_TABLE = "CREATE TABLE " + QuestionEntry.TABLE_NAME + " (" +
                QuestionEntry._ID + " INTEGER PRIMARY KEY," +
                QuestionEntry.COLUMN_TEXT + " TEXT, " +
                QuestionEntry.COLUMN_KEY + " TEXT, " +
                QuestionEntry.COLUMN_OPTIONS + " TEXT, " +
                QuestionEntry.COLUMN_TYPE + " TEXT, " +
                QuestionEntry.COLUMN_SCHOOL_TYPE + " TEXT" +
                " );";

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

        final String SQL_CREATE_BOUNDARY_TABLE = "CREATE TABLE " + BoundaryEntry.TABLE_NAME + " (" +
                BoundaryEntry._ID + " INTEGER PRIMARY KEY," +
                BoundaryEntry.COLUMN_PARENT + " INTEGER, " +
                BoundaryEntry.COLUMN_NAME + " TEXT, " +
                BoundaryEntry.COLUMN_HIERARCHY + " TEXT, " +
                BoundaryEntry.COLUMN_TYPE + " TEXT, " +
                " FOREIGN KEY (" + BoundaryEntry.COLUMN_PARENT + ") REFERENCES " +
                BoundaryEntry.TABLE_NAME + " (" + BoundaryEntry._ID + "));";

        final String SQL_CREATE_SCHOOL_TABLE = "CREATE TABLE " + SchoolEntry.TABLE_NAME + " (" +
                SchoolEntry._ID + " INTEGER PRIMARY KEY," +
                SchoolEntry.COLUMN_BOUNDARY + " INTEGER, " +
                SchoolEntry.COLUMN_NAME + " TEXT, " +
                " FOREIGN KEY (" + SchoolEntry.COLUMN_BOUNDARY + ") REFERENCES " +
                BoundaryEntry.TABLE_NAME + " (" + BoundaryEntry._ID + "));";


        sqLiteDatabase.execSQL(SQL_CREATE_SURVEY_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_QUESTION_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_QUESTIONGROUP_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_QUESTIONGROUPQUESTION_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_BOUNDARY_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_SCHOOL_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // FIXME: Should handle graceful versioning process instead of, duh, blindly
        // dropping the tables.
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + SurveyEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + QuestionEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }

    // Survey table helper functions.
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

    // Questiongroup table helper functions.
    public void insert_questiongroup(int id, int status, int start_date, int end_date, int version, String source, int survey_id) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(QuestiongroupEntry._ID, id);
        contentValues.put(QuestiongroupEntry.COLUMN_STATUS, status);
        contentValues.put(QuestiongroupEntry.COLUMN_START_DATE, start_date);
        contentValues.put(QuestiongroupEntry.COLUMN_END_DATE, end_date);
        contentValues.put(QuestiongroupEntry.COLUMN_VERSION, version);
        contentValues.put(QuestiongroupEntry.COLUMN_SOURCE, source);
        contentValues.put(QuestiongroupEntry.COLUMN_SURVEY, survey_id);
        this.getWritableDatabase().insertOrThrow(QuestiongroupEntry.TABLE_NAME,"",contentValues);
    }

    public void delete_questiongroup(int id) {
        this.getWritableDatabase().delete(QuestiongroupEntry.TABLE_NAME, QuestiongroupEntry._ID + "=" + id, null);
    }

    public Cursor list_questiongroups(String survey_id) {
        Cursor cursor = this.getReadableDatabase().rawQuery(
                "SELECT * FROM " + QuestiongroupEntry.TABLE_NAME + " where "
                + QuestiongroupEntry.COLUMN_SURVEY + "=" + survey_id,
                null
        );
        return cursor;
    }


    // Question table helper functions.
    public void insert_question(int id, String text, String key, String options, String type , String school_type) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(QuestionEntry._ID, id);
        contentValues.put(QuestionEntry.COLUMN_TEXT, text);
        contentValues.put(QuestionEntry.COLUMN_KEY, key);
        contentValues.put(QuestionEntry.COLUMN_OPTIONS, options);
        contentValues.put(QuestionEntry.COLUMN_TYPE, type);
        contentValues.put(QuestionEntry.COLUMN_SCHOOL_TYPE, school_type);

        this.getWritableDatabase().insertOrThrow(QuestionEntry.TABLE_NAME,"",contentValues);
    }

    public void delete_question(int id) {
        this.getWritableDatabase().delete(QuestionEntry.TABLE_NAME, QuestionEntry._ID + "=" + id, null);
    }

    public Cursor list_questions(String questiongroup_id) {
        Cursor cursor = this.getReadableDatabase().rawQuery(
                "SELECT * FROM " + QuestionEntry.TABLE_NAME + " q " +
                        " LEFT JOIN " + QuestiongroupQuestionEntry.TABLE_NAME + " qgq " +
                        " ON qgq."+ QuestiongroupQuestionEntry.COLUMN_QUESTION + "=q._id " +
                        " WHERE qgq." + QuestiongroupQuestionEntry.COLUMN_QUESTIONGROUP + "=" +
                        questiongroup_id
                ,
                null
        );
        return cursor;
    }

    //Questiongroupquestion table helper functions.
    public void insert_questiongroupquestion(int id, int question_id, int questiongroup_id, int sequence) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(QuestiongroupQuestionEntry._ID, id);
        contentValues.put(QuestiongroupQuestionEntry.COLUMN_QUESTION, question_id);
        contentValues.put(QuestiongroupQuestionEntry.COLUMN_QUESTIONGROUP, questiongroup_id);
        contentValues.put(QuestiongroupQuestionEntry.COLUMN_SEQUENCE, sequence);

        this.getWritableDatabase().insertOrThrow(QuestiongroupQuestionEntry.TABLE_NAME,"",contentValues);
    }

    public void delete_questiongroupquestion(int id) {
        this.getWritableDatabase().delete(QuestiongroupQuestionEntry.TABLE_NAME, QuestiongroupQuestionEntry._ID + "=" + id, null);
    }

    public Cursor list_questiongroupquestions() {
        Cursor cursor = this.getReadableDatabase().rawQuery("SELECT * FROM " + QuestiongroupQuestionEntry.TABLE_NAME, null);
        return cursor;
    }

    // School table helper functions
    public void insert_school(int id, int bounday_id, String name) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(SchoolEntry._ID, id);
        contentValues.put(SchoolEntry.COLUMN_BOUNDARY, bounday_id);
        contentValues.put(SchoolEntry.COLUMN_NAME, name);
        this.getWritableDatabase().insertOrThrow(SchoolEntry.TABLE_NAME,"",contentValues);
    }

    public void delete_school(int id) {
        this.getWritableDatabase().delete(SchoolEntry.TABLE_NAME, SchoolEntry._ID + "=" + id, null);
    }

    public Cursor list_schools() {
        Cursor cursor = this.getReadableDatabase().rawQuery("SELECT * FROM " + SchoolEntry.TABLE_NAME, null);
        return cursor;
    }

    // Boundary table helper functions
    public void insert_boundary(int id, int parent_id, String name, String hierarchy, String type) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(BoundaryEntry._ID, id);
        if (parent_id == -1){
            contentValues.putNull(BoundaryEntry.COLUMN_PARENT);
        }
        else {
            contentValues.put(BoundaryEntry.COLUMN_PARENT, parent_id);
        }
        contentValues.put(BoundaryEntry.COLUMN_NAME, name);
        contentValues.put(BoundaryEntry.COLUMN_HIERARCHY, hierarchy);
        contentValues.put(BoundaryEntry.COLUMN_TYPE, type);
        this.getWritableDatabase().insertOrThrow(BoundaryEntry.TABLE_NAME,"",contentValues);
    }

    public void delete_boundary(int id) {
        this.getWritableDatabase().delete(BoundaryEntry.TABLE_NAME, BoundaryEntry._ID + "=" + id, null);
    }

    public Cursor list_boundaries() {
        Cursor cursor = this.getReadableDatabase().rawQuery("SELECT * FROM " + BoundaryEntry.TABLE_NAME, null);
        return cursor;
    }

    public Cursor list_child_boundaries(int parent){
        String query="";
        if (parent==-1)
            query="SELECT " + BoundaryEntry._ID + ", '1' as bpundary_id, name   FROM " + BoundaryEntry.TABLE_NAME + " WHERE " + BoundaryEntry.COLUMN_PARENT + " is null and " + BoundaryEntry.COLUMN_TYPE + "='primaryschool' order by name";
        else
            query="SELECT * FROM " + BoundaryEntry.TABLE_NAME + " WHERE " + BoundaryEntry.COLUMN_PARENT + "=" + parent + " and " + BoundaryEntry.COLUMN_TYPE + "='primaryschool' order by name";
        Cursor cursor = this.getReadableDatabase().rawQuery(query, null);
        return cursor;
    }

    public Cursor list_schools_for_boundary(int boundary){
        Cursor cursor = this.getReadableDatabase().rawQuery("SELECT * FROM " + SchoolEntry.TABLE_NAME + " WHERE " + SchoolEntry.COLUMN_BOUNDARY + "=" + boundary, null);
        return cursor;
    }

}

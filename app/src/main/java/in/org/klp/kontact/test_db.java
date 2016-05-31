package in.org.klp.kontact;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by deviprasad on 28/5/16.
 */
public class test_db extends SQLiteOpenHelper {

    // Database Version
    private static final int DATABASE_VERSION = 1;
    // Database Name
    private static final String DATABASE_NAME = "klp";
    // Boundary table name
    private static final String Boundaries = "Boundary";
    // Boundary Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_PARENT = "parent";

    // School table name
    private static final String Schools = "Schools";
    // School table Column names
    private static final String SCHOOL_ID = "id";
    private static final String SCHOOL_NAME = "name";
    private static final String SCHOOL_BOUNDARY = "boundary";

    // School table name
    private static final String Questions = "Question";
    // School table Column names
    private static final String QUESTION_ID = "id";
    private static final String QUESTION_NAME = "name";
    private static final String QUESTION_SURVEY = "survey_id";

    public test_db(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_BOUNDARY_TABLE = "CREATE TABLE " + Boundaries + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_NAME + " TEXT,"
                + KEY_PARENT + " INTEGER" + ")";
        db.execSQL(CREATE_BOUNDARY_TABLE);
        String CREATE_SCHOOL_TABLE = "CREATE TABLE " + Schools + "("
                + SCHOOL_ID + " INTEGER PRIMARY KEY," + SCHOOL_NAME + " TEXT,"
                + SCHOOL_BOUNDARY + " INTEGER" + ")";
        db.execSQL(CREATE_SCHOOL_TABLE);
        db.execSQL(CREATE_BOUNDARY_TABLE);
        String CREATE_QUESTION_TABLE = "CREATE TABLE " + Questions + "("
                + QUESTION_ID + " INTEGER PRIMARY KEY," + QUESTION_NAME + " TEXT,"
                + QUESTION_SURVEY + " INTEGER" + ")";
        db.execSQL(CREATE_QUESTION_TABLE);
        add_boundary(new Boundary(1234, "District", 1));
        add_boundary(new Boundary(1235, "block1", 1234));
        add_boundary(new Boundary(1236, "Block2", 1234));
        add_boundary(new Boundary(1237, "Cluster11", 1235));
        add_boundary(new Boundary(1238, "Cluster12", 1235));
        add_boundary(new Boundary(1239, "Cluster21", 1236));
        add_boundary(new Boundary(8773, "cluster22", 1236));
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
// Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + Boundaries);
// Creating tables again
        onCreate(db);
    }

    public void add_boundary(Boundary boundary){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_ID, boundary.getId()); // Boundary Id
        values.put(KEY_NAME, boundary.getName()); // Bounday Name
        values.put(KEY_PARENT, boundary.getParent());
// Inserting Row
        db.insert(Boundaries, null, values);
        db.close(); // Closing database connection
    }

    public void add_school(School school){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(SCHOOL_ID, school.getId()); // Boundary Id
        values.put(SCHOOL_NAME, school.getName()); // Boundary Name
        values.put(SCHOOL_BOUNDARY, school.getBoundary());
// Inserting Row
        db.insert(Schools, null, values);
        db.close(); // Closing database connection
    }

    public void add_question(School school){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(QUESTION_ID, school.getId()); // Boundary Id
        values.put(QUESTION_NAME, school.getName()); // Boundary Name
        values.put(QUESTION_SURVEY, school.getBoundary());
// Inserting Row
        db.insert(Schools, null, values);
        db.close(); // Closing database connection
    }

    public Boundary get_boundary(int id){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(Boundaries, new String[] { KEY_ID,
                        KEY_NAME, KEY_PARENT }, KEY_ID + "=?",
                new String[] { String.valueOf(id) }, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();
        Boundary boundary = new Boundary(Integer.parseInt(cursor.getString(0)),
                cursor.getString(1), Integer.parseInt(cursor.getString(2)));
// return boundary
        return boundary;
    }

    public School get_school(int id){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(Schools, new String[] { SCHOOL_ID,
                        SCHOOL_NAME, SCHOOL_BOUNDARY }, KEY_ID + "=?",
                new String[] { String.valueOf(id) }, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();
        School school = new School(Integer.parseInt(cursor.getString(0)),
                cursor.getString(1), Integer.parseInt(cursor.getString(2)));
// return boundary
        return school;
    }

    public List<StringWithTags> get_all_boundaries(int parent){
        List<StringWithTags> boundaryList = new ArrayList<StringWithTags>();
        String selectQuery = "SELECT * FROM " + Boundaries + " where parent=" + parent;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
// looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                StringWithTags boundary = new StringWithTags(cursor.getString(1), Integer.parseInt(cursor.getString(0)), Integer.parseInt(cursor.getString(2)));
                //boundary.setId(Integer.parseInt(cursor.getString(0)));
                //boundary.setName(cursor.getString(1));
                //boundary.setParent(Integer.parseInt(cursor.getString(2)));
                boundaryList.add(boundary);
            } while (cursor.moveToNext());
        }
        return  boundaryList;
    }

    public List<StringWithTags> get_all_schools(int boundary){
        List<StringWithTags> schoolList = new ArrayList<StringWithTags>();
        String selectQuery = "SELECT * FROM " + Schools + " where boundary=" + boundary;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                StringWithTags school = new StringWithTags(cursor.getString(1), Integer.parseInt(cursor.getString(0)), Integer.parseInt(cursor.getString(2)));
                schoolList.add(school);
            } while (cursor.moveToNext());
        }
        return  schoolList;
    }
}

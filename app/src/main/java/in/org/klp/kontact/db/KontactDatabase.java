package in.org.klp.kontact.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.yahoo.squidb.data.SquidDatabase;
import com.yahoo.squidb.data.adapter.SQLiteDatabaseWrapper;
import com.yahoo.squidb.sql.Table;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by bibhas on 6/17/16.
 */
// This is how you'd set up a database instance
public class KontactDatabase extends SquidDatabase {

    private static String DB_NAME ="kontact.db";
    private static final int VERSION = 2;
    private static String DB_PATH = "";
    private static Context myContext;

    public KontactDatabase(Context context) {
        super(context);
        myContext = context;

        if (android.os.Build.VERSION.SDK_INT >= 17) {
            DB_PATH = context.getApplicationInfo().dataDir + "/databases/";
        } else {
            DB_PATH = "/data/data/" + context.getPackageName() + "/databases/";
        }
    }

    /*
     * Create the database
     * check if db already exists, if not copy from assets directory
     * if copying fails, create new db
     */
    public void initDatabase() {
        if(!deviceDatabaseExists()){
            try {
                Log.d("db", "copying db to the device");
                copyDatabase();
            } catch (IOException e) {
                // copying failed, create new db as a fallback measure
                // empty db is better than no db
                Log.e("db", "Error copying database, creating new empty db.");
                e.printStackTrace();

                acquireNonExclusiveLock();
                try {
                    getDatabase();
                } finally {
                    releaseNonExclusiveLock();
                }
            }
        }

    }

    /*
     * copy the database from assets directory
     */
    private void copyDatabase() throws IOException {
        //Open your local db as the input stream
        InputStream myInput = myContext.getAssets().open(DB_NAME);

        // Path to the just created empty db
        String outFileName = DB_PATH + getName();

        //Open the empty db as the output stream
        OutputStream myOutput = new FileOutputStream(outFileName);

        //transfer bytes from the inputfile to the outputfile
        byte[] buffer = new byte[1024];
        int length;
        while ((length = myInput.read(buffer))>0){
            myOutput.write(buffer, 0, length);
        }

        //Close the streams
        myOutput.flush();
        myOutput.close();
        myInput.close();
    }

    /**
     * Checks if the database already exists in the device
     * */
    public boolean deviceDatabaseExists() {
        SQLiteDatabase checkDB = null;
        Boolean dbExists = false;

        Log.d("db", "Checking if database already exists in the device.");
        try{
            String dbPath = DB_PATH + getName();
            checkDB = SQLiteDatabase.openDatabase(getDatabasePath(), null, SQLiteDatabase.OPEN_READONLY);
            dbExists = true;
        } catch (SQLiteException e){
            //database does't exist yet.
            dbExists = false;
        } finally {
            if(checkDB != null){
                checkDB.close();
            }
        }
        Log.d("db", "database exists in device: " + dbExists.toString());
        return dbExists;
    }

    @Override
    public String getName() {
        return DB_NAME;
    }

    public String getDbPath() {
        return DB_PATH;
    }

    @Override
    protected Table[] getTables() {
        return new Table[]{
            // List all tables here
            School.TABLE,
            Boundary.TABLE,
            Survey.TABLE,
            Story.TABLE,
            Question.TABLE,
            QuestionGroup.TABLE,
            QuestionGroupQuestion.TABLE,
            Answer.TABLE
        };
    }

    @Override
    protected int getVersion() {
        return VERSION;
    }

    @Override
    protected boolean onUpgrade(SQLiteDatabaseWrapper db, int oldVersion, int newVersion) {
        // nothing happens
        // to create tables, try like this -> tryCreateTable(School.TABLE)
        // https://github.com/yahoo/squidb/wiki/Implementing-database-upgrades
        switch(oldVersion) {
            case 1:
                // These tables were added in v2
                tryCreateTable(Story.TABLE);
                tryCreateTable(Answer.TABLE);
        }
        // https://github.com/yahoo/squidb/wiki/Implementing-database-upgrades#some-people-just-want-to-watch-the-world-burn
        return true;
    }

    // Other overridable methods exist for migrations and initialization;
    // omitted for brevity
}
package in.org.klp.kontact.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.yahoo.squidb.data.SquidDatabase;
import com.yahoo.squidb.data.TableModel;
import com.yahoo.squidb.data.adapter.SQLiteDatabaseWrapper;
import com.yahoo.squidb.sql.Table;
import com.yahoo.squidb.sql.TableStatement;

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
    private static final int VERSION = 3;
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
//        switch(oldVersion) {
//            case 1:
//                // These tables were added in v2
//                tryCreateTable(Story.TABLE);
//                tryCreateTable(Answer.TABLE);
//        }

        /*
         * Commented everything because we're letting SQLite Asset Helper handle the migrations.
         * because that library handles copying over the database.
         * If we had a normal application that didn't come with prepopulated database,
         * we could have used this method to manage migrations.
         */

        // https://github.com/yahoo/squidb/wiki/Implementing-database-upgrades#some-people-just-want-to-watch-the-world-burn
        return true;
    }

    // by default squidb wont let you override the id field
    // this is the way to do it
    // https://github.com/yahoo/squidb/issues/186
    public boolean insertWithId(TableModel item) {
        return insertRow(item, TableStatement.ConflictAlgorithm.REPLACE);
    }
}
package in.org.klp.kontact.db;

import android.content.Context;

import com.yahoo.squidb.data.SquidDatabase;
import com.yahoo.squidb.data.adapter.SQLiteDatabaseWrapper;
import com.yahoo.squidb.sql.Table;

/**
 * Created by bibhas on 6/17/16.
 */
// This is how you'd set up a database instance
public class KontactDatabase extends SquidDatabase {

    private static final int VERSION = 2;

    public KontactDatabase(Context context) {
        super(context);
    }

    @Override
    public String getName() {
        return "kontact.db";
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
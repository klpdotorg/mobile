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

    private static final int VERSION = 1;

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
                Question.TABLE,
        };
    }

    @Override
    protected int getVersion() {
        return VERSION;
    }

    @Override
    protected boolean onUpgrade(SQLiteDatabaseWrapper db, int oldVersion, int newVersion) {
        return false;
    }

    // Other overridable methods exist for migrations and initialization;
    // omitted for brevity
}
package in.org.klp.kontact.db;

import android.content.Context;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

/**
 * Created by bibhas on 6/19/16.
 */
public class DatabaseCopyHelper extends SQLiteAssetHelper {

    private static final String DATABASE_NAME = "kontact.db";
    private static final int DATABASE_VERSION = 2;

    public DatabaseCopyHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
}
package in.org.klp.kontact.db;

import android.content.Context;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

/**
 * Created by bibhas on 6/19/16.
 *
 * This is only for handling the copying process of the database.
 * The SQLiteAssetHelper is a plugin that takes the database from
 * /main/assets/databases/kontact.db to the device location where
 * the app can use it. And this plugin only works with the inbuilt
 * SqliteOpenHelper. But we're using SquiDB to deal with database
 * queries right now. And SquiDB and SQLiteAssetHelper cannot work
 * together. Hence this class is here only so that SQLiteAssetHelper
 * can use it to copy the database.
 *
 */
public class DatabaseCopyHelper extends SQLiteAssetHelper {

    private static final String DATABASE_NAME = "kontact.db";
    private static final int DATABASE_VERSION = 2;

    public DatabaseCopyHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
}
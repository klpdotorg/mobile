package in.org.klp.kontact;

import android.app.Application;
import android.util.Log;

import java.io.IOException;

import in.org.klp.kontact.db.KontactDatabase;

/**
 * Created by bibhas on 6/19/16.
 */
public class KLPApplication extends Application {
    @Override
    public void onCreate() {
        // TODO: Copy database and set shared pref
        KontactDatabase db = new KontactDatabase(this);
        db.initDatabase();

        super.onCreate();
    }
}

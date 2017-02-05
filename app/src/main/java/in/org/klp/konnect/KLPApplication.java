package in.org.klp.konnect;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.crashlytics.android.Crashlytics;

import java.util.Locale;

import in.org.klp.konnect.db.KontactDatabase;
import io.fabric.sdk.android.Fabric;

/**
 * Created by bibhas on 7/5/16.
 */
public class KLPApplication extends Application {
    KontactDatabase db;

    @Override
    public void onCreate() {
        super.onCreate();

        initSingletons();
        updateLanguage(this);
        Fabric.with(this, new Crashlytics());
    }

    private void initSingletons() {
        db = new KontactDatabase(this);
    }

    public KontactDatabase getDb() {
        return db;
    }

    public static void updateLanguage(Context ctx)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        String lang = prefs.getString("user_locale", "kn");
        updateLanguage(ctx, lang);
    }

    public static void updateLanguage(Context ctx, String lang)
    {
        Configuration cfg = new Configuration();
        if (!TextUtils.isEmpty(lang))
            cfg.locale = new Locale(lang);
        else
            cfg.locale = Locale.getDefault();

        ctx.getResources().updateConfiguration(cfg, null);
    }
}

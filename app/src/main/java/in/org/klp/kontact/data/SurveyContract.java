package in.org.klp.kontact.data;

import android.provider.BaseColumns;


public class SurveyContract {
    public static final class SurveyEntry implements BaseColumns {
        public static final String TABLE_NAME = "survey";
        public static final String COLUMN_PARTNER = "partner";
        public static final String COLUMN_NAME = "name";
    }
}

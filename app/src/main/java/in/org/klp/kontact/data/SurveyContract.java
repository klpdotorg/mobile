package in.org.klp.kontact.data;

import android.provider.BaseColumns;

/**
 * Created by haris on 5/16/16.
 */
public class SurveyContract {
    public static final class SurveyEntry implements BaseColumns {
        public static final String TABLE_NAME = "survey";
        public static final String COLUMN_STATUS = "status";
        public static final String COLUMN_CREATED_AT = "created_at";
        public static final String COLUMN_START_DATE = "start_date";
        public static final String COLUMN_END_DATE = "end_date";
        public static final String COLUMN_PARTNER = "partner";
        public static final String COLUMN_NAME = "name";
    }
}

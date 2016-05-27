package in.org.klp.kontact.data;

import android.provider.BaseColumns;


public class SurveyContract {

    public static final class SurveyEntry implements BaseColumns {
        public static final String TABLE_NAME = "survey";
        public static final String COLUMN_PARTNER = "partner";
        public static final String COLUMN_NAME = "name";
    }

    public static final class QuestionEntry implements BaseColumns {
        public static final String TABLE_NAME = "question";
        public static final String COLUMN_TEXT = "text";
        public static final String COLUMN_TYPE = "type";
        public static final String COLUMN_OPTIONS = "options";
        public static final String COLUMN_KEY = "key";
        public static final String COLUMN_SCHOOL_TYPE = "school_type_id";
    }
}

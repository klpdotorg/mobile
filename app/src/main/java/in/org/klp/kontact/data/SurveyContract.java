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
        public static final String COLUMN_SCHOOL_TYPE = "school_type";
    }

    public static final class QuestiongroupEntry implements BaseColumns {
        public static final String TABLE_NAME = "questiongroup";
        public static final String COLUMN_STATUS = "status";
        public static final String COLUMN_START_DATE = "start_date";
        public static final String COLUMN_END_DATE = "end_date";
        public static final String COLUMN_VERSION = "version";
        public static final String COLUMN_SOURCE = "source";
        public static final String COLUMN_SURVEY = "survey_id";
    }

    public static final class QuestiongroupQuestionEntry implements BaseColumns {
        public static final String TABLE_NAME = "questiongroupquestion";
        public static final String COLUMN_QUESTIONGROUP = "questiongroup_id";
        public static final String COLUMN_QUESTION = "question_id";
        public static final String COLUMN_SEQUENCE = "sequence";
    }
}
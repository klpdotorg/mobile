package in.org.klp.kontact.data;

import android.provider.BaseColumns;

public class SchoolContract {
    public static final class BoundaryEntry implements BaseColumns {
        public static final String TABLE_NAME = "boundary";
        public static final String COLUMN_PARENT = "boundary_id";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_HIERARCHY = "hierarchy";
        public static final String COLUMN_TYPE = "type";
    }

    public static final class SchoolEntry implements BaseColumns {
        public static final String TABLE_NAME = "school";
        public static final String COLUMN_BOUNDARY = "boundary_id";
        public static final String COLUMN_DISE = "dise_code";
        public static final String COLUMN_NAME = "name";

    }
}

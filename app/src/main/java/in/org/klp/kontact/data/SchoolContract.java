package in.org.klp.kontact.data;


import android.provider.BaseColumns;

public class SchoolContract {
    public static final class BoundaryHierarchyEntry implements BaseColumns {
        public static final String TABLE_NAME = "boundary_hierarchy";
        public static final String COLUMN_NAME = "name";
    }

    public static final class BoundaryEntry implements BaseColumns {
        public static final String TABLE_NAME = "boundary";
        public static final String COLUMN_PARENT = "boundary_id";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_HIERARCHY = "hierarchy";
        public static final String COLUMN_TYPE = "type";
    }

    
}

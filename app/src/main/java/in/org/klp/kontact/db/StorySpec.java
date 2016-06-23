package in.org.klp.kontact.db;

import com.yahoo.squidb.annotations.ColumnSpec;
import com.yahoo.squidb.annotations.PrimaryKey;
import com.yahoo.squidb.annotations.TableModelSpec;

/**
 * Created by bibhas on 6/17/16.
 */
@TableModelSpec(className = "Story", tableName = "story")
public class StorySpec {
    @PrimaryKey
    @ColumnSpec(name="_id")
    long Id;

    @ColumnSpec(name="user_id", constraints = "NOT NULL")
    public long user_id;

    @ColumnSpec(name="school_id", constraints = "NOT NULL")
    public long school_id;

    @ColumnSpec(name="group_id", constraints = "NOT NULL")
    public long group_id;

    @ColumnSpec(name="created_at", constraints = "NOT NULL")
    public long created_at;

    @ColumnSpec(defaultValue="0")
    public int synced;

    public String sysid;

}
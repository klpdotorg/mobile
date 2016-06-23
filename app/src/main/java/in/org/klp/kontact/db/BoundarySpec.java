package in.org.klp.kontact.db;

import com.yahoo.squidb.annotations.ColumnSpec;
import com.yahoo.squidb.annotations.PrimaryKey;
import com.yahoo.squidb.annotations.TableModelSpec;

/**
 * Created by bibhas on 6/17/16.
 */
@TableModelSpec(className = "Boundary", tableName = "boundary")
public class BoundarySpec {
    @PrimaryKey
    @ColumnSpec(name="_id")
    long Id;

    @ColumnSpec(name="boundary_id")
    public long boundary_id;

    @ColumnSpec(name="name")
    public String name;

    public String hierarchy;
    public String type;
}
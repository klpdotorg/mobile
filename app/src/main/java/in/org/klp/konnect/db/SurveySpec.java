package in.org.klp.konnect.db;

import com.yahoo.squidb.annotations.ColumnSpec;
import com.yahoo.squidb.annotations.PrimaryKey;
import com.yahoo.squidb.annotations.TableModelSpec;

/**
 * Created by bibhas on 6/17/16.
 */
@TableModelSpec(className = "Survey", tableName = "survey")
public class SurveySpec {
    @PrimaryKey
    @ColumnSpec(name="_id")
    long Id;

    public String partner;
    public String name;
}
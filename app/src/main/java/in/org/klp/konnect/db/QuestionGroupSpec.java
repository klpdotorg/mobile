package in.org.klp.konnect.db;

import com.yahoo.squidb.annotations.ColumnSpec;
import com.yahoo.squidb.annotations.PrimaryKey;
import com.yahoo.squidb.annotations.TableModelSpec;

/**
 * Created by bibhas on 6/17/16.
 */
@TableModelSpec(className = "QuestionGroup", tableName = "questiongroup")
public class QuestionGroupSpec {
    @PrimaryKey
    @ColumnSpec(name="_id")
    long Id;

    public int status;

    // A text column named "firstName"
    @ColumnSpec(name="start_date")
    public long start_date;

    @ColumnSpec(name="end_date")
    public long end_date;

    public int version;

    public String source;

    @ColumnSpec(name="survey_id")
    public long survey_id;
}
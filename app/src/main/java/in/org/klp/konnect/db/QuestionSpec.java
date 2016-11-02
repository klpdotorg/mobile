package in.org.klp.konnect.db;

import com.yahoo.squidb.annotations.ColumnSpec;
import com.yahoo.squidb.annotations.PrimaryKey;
import com.yahoo.squidb.annotations.TableModelSpec;

/**
 * Created by bibhas on 6/17/16.
 */
@TableModelSpec(className = "Question", tableName = "question")
public class QuestionSpec {
    @PrimaryKey
    @ColumnSpec(name="_id")
    long Id;

    public String text;

    @ColumnSpec(name="display_text")
    public String display_text;

    public String key;
    public String options;
    public String type;

    @ColumnSpec(name="school_type")
    public String school_type;

    @ColumnSpec(name="text_kn")
    public String text_kn;
}
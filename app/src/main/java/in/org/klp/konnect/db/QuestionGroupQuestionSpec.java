package in.org.klp.konnect.db;

import com.yahoo.squidb.annotations.ColumnSpec;
import com.yahoo.squidb.annotations.PrimaryKey;
import com.yahoo.squidb.annotations.TableModelSpec;

/**
 * Created by bibhas on 6/17/16.
 */
@TableModelSpec(className = "QuestionGroupQuestion", tableName = "questiongroupquestion")
public class QuestionGroupQuestionSpec {
    @PrimaryKey
    @ColumnSpec(name="_id")
    long Id;

    public int sequence;

    @ColumnSpec(name="question_id")
    public long question_id;

    @ColumnSpec(name="questiongroup_id")
    public long questiongroup_id;
}
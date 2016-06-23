package in.org.klp.kontact.db;

import com.yahoo.squidb.annotations.ColumnSpec;
import com.yahoo.squidb.annotations.PrimaryKey;
import com.yahoo.squidb.annotations.TableModelSpec;

/**
 * Created by bibhas on 6/17/16.
 */
@TableModelSpec(className = "Answer", tableName = "answer",
        tableConstraint = "FOREIGN KEY(story_id) references story(_id) ON DELETE CASCADE")
public class AnswerSpec {
    @PrimaryKey
    @ColumnSpec(name="_id")
    long Id;

    @ColumnSpec(name="text", constraints = "NOT NULL")
    public String text;

    @ColumnSpec(name="story_id", constraints = "NOT NULL")
    public long story_id;

    @ColumnSpec(name="question_id", constraints = "NOT NULL")
    public long question_id;

    @ColumnSpec(name="created_at", constraints = "NOT NULL")
    public long created_at;
}
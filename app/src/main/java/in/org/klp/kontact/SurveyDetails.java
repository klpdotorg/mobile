package in.org.klp.kontact;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import in.org.klp.kontact.db.KontactDatabase;
import in.org.klp.kontact.db.QuestionGroup;

public class SurveyDetails extends AppCompatActivity {
    Long surveyId;
    String surveyName;
    KontactDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey_details);

        db = new KontactDatabase(this);

        surveyId = getIntent().getLongExtra("surveyId", 0);
        surveyName = getIntent().getStringExtra("surveyName");

        if (surveyId == 0 || surveyName.isEmpty()) {
            Intent intent = new Intent(SurveyDetails.this, MainActivity.class);
            startActivity(intent);
        }

        TextView textView = (TextView) findViewById(R.id.survey_details);
        textView.setText(surveyName);

        Button button = (Button) findViewById(R.id.generate_report);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(SurveyDetails.this, BoundarySelectionActivity.class);
                intent.putExtra("surveyId", surveyId);
                intent.putExtra("surveyName", surveyName);
                intent.putExtra("type","report");
                startActivity(intent);
            }
        });

        button = (Button) findViewById(R.id.new_response);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(SurveyDetails.this, BoundarySelectionActivity.class);
                intent.putExtra("surveyId", surveyId);
                intent.putExtra("surveyName", surveyName);
                intent.putExtra("type","response");
                startActivity(intent);
            }
        });



        // check criterion - https://github.com/yahoo/squidb/wiki/SquiDB's-query-builder#criterion
        int qgCount = db.count(QuestionGroup.class, QuestionGroup.SURVEY_ID.eq(surveyId));
        // disabling new response button if there is no QG for the given survey
        if (qgCount == 0) button.setEnabled(false);
    }
}

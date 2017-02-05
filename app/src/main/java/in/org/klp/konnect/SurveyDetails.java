package in.org.klp.konnect;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import in.org.klp.konnect.db.KontactDatabase;
import in.org.klp.konnect.db.QuestionGroup;
import in.org.klp.konnect.db.Survey;

public class SurveyDetails extends AppCompatActivity {
    Long surveyId;
    String surveyName;
    KontactDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey_details);

        db = ((KLPApplication) getApplicationContext()).getDb();

        surveyId = getIntent().getLongExtra("surveyId", 0);
        surveyName = getIntent().getStringExtra("surveyName");

        if (surveyId == 0 || surveyName.isEmpty()) {
            Toast.makeText(this, "Invalid Survey ID/Name", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(SurveyDetails.this, MainActivity.class);
            startActivity(intent);
        }

        Survey survey = db.fetch(Survey.class, surveyId);

        TextView textViewName = (TextView) findViewById(R.id.textViewSurveyName);
        textViewName.setText(survey.getName());

        TextView textViewPartner = (TextView) findViewById(R.id.textViewSurveyPartner);
        textViewPartner.setText(survey.getPartner());

        Button button = (Button) findViewById(R.id.generate_report);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SurveyDetails.this, BoundarySelectionActivity.class);
                intent.putExtra("surveyId", surveyId);
                intent.putExtra("surveyName", surveyName);
                intent.putExtra("type", "report");
                startActivity(intent);
            }
        });

        button = (Button) findViewById(R.id.new_response);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SurveyDetails.this, BoundarySelectionActivity.class);
                intent.putExtra("surveyId", surveyId);
                intent.putExtra("surveyName", surveyName);
                intent.putExtra("type", "response");
                startActivity(intent);
            }
        });

        button = (Button) findViewById(R.id.list_stories);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SurveyDetails.this, StoriesActivity.class);
                intent.putExtra("surveyId", surveyId);
                intent.putExtra("surveyName", surveyName);
                intent.putExtra("type", "stories");
                startActivity(intent);
            }
        });


        // check criterion - https://github.com/yahoo/squidb/wiki/SquiDB's-query-builder#criterion
        int qgCount = db.count(QuestionGroup.class, QuestionGroup.SURVEY_ID.eq(surveyId));
        // disabling new response button if there is no QG for the given survey
        if (qgCount == 0) button.setEnabled(false);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(SurveyDetails.this, SurveyActivity.class);
        startActivity(intent);
    }
}

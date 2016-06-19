package in.org.klp.kontact;


import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import in.org.klp.kontact.db.KontactDatabase;
import in.org.klp.kontact.db.School;

public class BoundarySelectionActivity extends AppCompatActivity {
    Long surveyId;
    String surveyName;
    Long schoolId;
    KontactDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boundary_selection);
        db = new KontactDatabase(this);

        surveyId = getIntent().getLongExtra("surveyId", 0);
        surveyName = getIntent().getStringExtra("surveyName");
        // presettings school id for demo
        schoolId = new Long(33313);

        if (surveyId == 0 | TextUtils.isEmpty(surveyName)) {
            Intent intent = new Intent(BoundarySelectionActivity.this, MainActivity.class);
            startActivity(intent);
        }

        School school = db.fetch(School.class, schoolId);
        TextView textViewChosenSchool = (TextView) findViewById(R.id.textViewChosenSchool);
        textViewChosenSchool.setText("Chosen school for demo: " + school.getName() + " (" + schoolId.toString() + ")");

        TextView textView = (TextView) findViewById(R.id.survey_details);
        textView.setText(surveyName);

        Button button = (Button) findViewById(R.id.questions_activity);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(BoundarySelectionActivity.this, QuestionActivity.class);
                intent.putExtra("surveyId", surveyId);
                intent.putExtra("surveyName", surveyName);

                // after filtering by location and finding schools, put the school KLP id here
                intent.putExtra("schoolId", schoolId);

                startActivity(intent);
            }
        });
    }
}

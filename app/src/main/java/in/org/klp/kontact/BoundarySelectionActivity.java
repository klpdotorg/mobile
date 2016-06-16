package in.org.klp.kontact;


import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class BoundarySelectionActivity extends AppCompatActivity {
    String surveyId;
    String surveyName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boundary_selection);
        surveyId = getIntent().getStringExtra("surveyId");
        surveyName = getIntent().getStringExtra("surveyName");
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
                intent.putExtra("schoolId", 33313);

                startActivity(intent);
            }
        });
    }
}

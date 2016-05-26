package in.org.klp.kontact;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class SurveyDetails extends AppCompatActivity {
    String surveyId;
    String surveyName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey_details);
        surveyId = getIntent().getStringExtra("surveyId");
        surveyName = getIntent().getStringExtra("surveyName");
        TextView textView = (TextView) findViewById(R.id.survey_details);
        textView.setText(surveyName);

        Button button = (Button) findViewById(R.id.new_response);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(SurveyDetails.this, BoundarySelectionActivity.class);
                intent.putExtra("surveyId", surveyId);
                intent.putExtra("surveyName", surveyName);
                startActivity(intent);
            }
        });
    }
}

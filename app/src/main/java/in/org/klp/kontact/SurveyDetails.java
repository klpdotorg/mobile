package in.org.klp.kontact;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class SurveyDetails extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey_details);
        String surveyId = getIntent().getStringExtra("surveyId");
        String surveyName = getIntent().getStringExtra("surveyName");
        TextView textView=(TextView) findViewById(R.id.survey_details);
        textView.setText(surveyName);
    }
}

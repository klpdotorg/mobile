package in.org.klp.kontact;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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

    }
}

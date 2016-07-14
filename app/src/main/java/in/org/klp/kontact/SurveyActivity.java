package in.org.klp.kontact;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

public class SurveyActivity extends AppCompatActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(SurveyActivity.this, MainActivity.class);
        startActivity(intent);
    }
}

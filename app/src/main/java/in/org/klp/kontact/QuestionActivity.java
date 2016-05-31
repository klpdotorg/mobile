package in.org.klp.kontact;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.Toast;

import in.org.klp.kontact.data.SurveyContract;
import in.org.klp.kontact.data.SurveyDbHelper;

public class QuestionActivity extends AppCompatActivity {
    private SurveyDbHelper mDbHelper;
    private SQLiteDatabase db;
    private String surveyId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDbHelper = new SurveyDbHelper(getApplicationContext());
        db = mDbHelper.getReadableDatabase();

        surveyId = getIntent().getStringExtra("surveyId");

        Cursor c = db.query(
                SurveyContract.QuestiongroupEntry.TABLE_NAME,
                new String[] {
                        SurveyContract.QuestiongroupEntry._ID
                },
                "survey_id = ? AND start_date < date('now') AND end_date > date('now')",
                new String[] {
                        surveyId
                },
                null,
                null,
                null
        );

        Toast.makeText(this, "Total " + String.valueOf(c.getColumnCount()) + " columns found", Toast.LENGTH_SHORT).show();
        c.moveToFirst();

    }
}

package in.org.klp.kontact;


import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ListPopupWindow;
import android.view.View;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import in.org.klp.kontact.data.SqlHandler;

public class BoundarySelectionActivity extends AppCompatActivity {
    String surveyId, surveyName, next;
    SqlHandler sqlhandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boundary_selection);

        sqlhandler = new SqlHandler(this);
        surveyId = getIntent().getStringExtra("surveyId");
        surveyName = getIntent().getStringExtra("surveyName");
        next = getIntent().getStringExtra("next");

        TextView textView = (TextView) findViewById(R.id.survey_details);
        textView.setText(surveyName);

        Button select_district = (Button) findViewById(R.id.select_district);
        select_district.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ListPopupWindow listDistrictsPopup = new ListPopupWindow(getApplicationContext());
                listDistrictsPopup.setAnchorView(view);

                HashMap<Integer, String> distMap = getDistricts(sqlhandler);
            }
        });

        Button button = (Button) findViewById(R.id.questions_activity);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(BoundarySelectionActivity.this, QuestionActivity.class);
                intent.putExtra("surveyId", surveyId);
                intent.putExtra("surveyName", surveyName);
                startActivity(intent);
            }
        });
    }

    private HashMap<Integer, String> getDistricts(SqlHandler sqlHandler) {
        HashMap<Integer, String> map = new HashMap<Integer, String>();

        String query = "SELECT id, name FROM boundary WHERE type=1 AND hid=9";
        Cursor distCursor = sqlHandler.selectQuery(query);

        if (distCursor != null && distCursor.getCount() != 0) {
            if (distCursor.moveToFirst()) {
                do {
                    map.put(
                            Integer.valueOf(distCursor.getString(distCursor.getColumnIndex("id"))),
                            distCursor.getString(distCursor.getColumnIndex("name"))
                    );
                } while (distCursor.moveToNext());
            }
        }
        distCursor.close();
        return map;
    }
}

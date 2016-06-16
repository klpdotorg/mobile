package in.org.klp.kontact;


import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import in.org.klp.kontact.data.SurveyDbHelper;

public class BoundarySelectionActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    String surveyId, schoolID;
    String surveyName, district, block, cluster, school;

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
                intent.putExtra("boundary",district+", "+block+", "+cluster+", \n " +schoolID.toString() +  " : "+ school);
                intent.putExtra("surveyId", surveyId);
                intent.putExtra("surveyName", surveyName);
                intent.putExtra("schoolID", schoolID);
                startActivity(intent);
            }
        });

        fill_dropdown(1,R.id.select_district_report, -1);
    }

    private void fill_dropdown(int type, int id, int parent){
        List<StringWithTags> stringWithTags;
        if (type==1)
            stringWithTags = get_boundary_data(parent);
        else
            stringWithTags = get_school_data(parent);
        Spinner spinner=(Spinner) findViewById(id);
        spinner.setOnItemSelectedListener(this);
        ArrayAdapter<StringWithTags> boundaryArrayAdapter=new ArrayAdapter<StringWithTags>(this, android.R.layout.simple_spinner_item, stringWithTags);
        spinner.setAdapter(boundaryArrayAdapter);
        boundaryArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
        StringWithTags boundaryForSelector = (StringWithTags) parent.getItemAtPosition(pos);
        switch (parent.getId()){
            case R.id.select_district_report:
                fill_dropdown(1,R.id.select_block_report, Integer.parseInt(boundaryForSelector.id.toString()));
                district=((StringWithTags) parent.getItemAtPosition(pos)).string;
                break;
            case R.id.select_block_report:
                fill_dropdown(1,R.id.select_cluster_report, Integer.parseInt(boundaryForSelector.id.toString()));
                block=((StringWithTags) parent.getItemAtPosition(pos)).string;
                break;
            case R.id.select_cluster_report:
                fill_dropdown(2,R.id.select_school_report, Integer.parseInt(boundaryForSelector.id.toString()));
                cluster=((StringWithTags) parent.getItemAtPosition(pos)).string;
                break;
            case R.id.select_school_report:
                schoolID=boundaryForSelector.id.toString();
                school=((StringWithTags) parent.getItemAtPosition(pos)).string;
        }
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

    private List<StringWithTags> get_boundary_data(int parent) {
        SurveyDbHelper dbHelper = new SurveyDbHelper(this);
        Cursor cursor = dbHelper.list_child_boundaries(parent);
        List<StringWithTags> boundaryList = new ArrayList<StringWithTags>();
        if (cursor.moveToFirst()) {
            do {
                StringWithTags school = new StringWithTags(cursor.getString(2), Integer.parseInt(cursor.getString(0)), Integer.parseInt(cursor.getString(1)));
                boundaryList.add(school);
            } while (cursor.moveToNext());
        }
        return  boundaryList;
    }

    private void select_schools(int boundary){
        ListView listView=(ListView) findViewById(R.id.school_list); //nothing
        List<StringWithTags> schoolList=get_school_data(boundary);
        ArrayAdapter<StringWithTags> schoolArrayAdapter=new ArrayAdapter<StringWithTags>(this, android.R.layout.simple_list_item_1, schoolList);
        listView.setAdapter(schoolArrayAdapter);
        ViewGroup.LayoutParams listViewParams = (ViewGroup.LayoutParams)listView.getLayoutParams();
        listViewParams.height = 400;
        listView.requestLayout();
    }

    private List<StringWithTags> get_school_data(int boundary){
        SurveyDbHelper dbHelper = new SurveyDbHelper(this);
        Cursor cursor = dbHelper.list_schools_for_boundary(boundary);
        List<StringWithTags> schoolList = new ArrayList<StringWithTags>();
        if (cursor.moveToFirst()) {
            do {
                StringWithTags school = new StringWithTags(cursor.getString(2), Integer.parseInt(cursor.getString(0)),1/* Integer.parseInt(cursor.getString(1))*/);
                schoolList.add(school);
            } while (cursor.moveToNext());
        }
        return  schoolList;
    }
}

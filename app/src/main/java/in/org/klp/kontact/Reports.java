package in.org.klp.kontact;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import in.org.klp.kontact.data.SurveyDbHelper;

public class Reports extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    String district, block, cluster = null, ret_str=null;
    int cyear,cdate,cmonth,chour,cminute;
    EditText editText;
    SimpleDateFormat dateFormat=new SimpleDateFormat();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        final String surveyId = getIntent().getStringExtra("surveyId");

        Button bt=(Button) findViewById(R.id.bt_display_report);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Intent myIntent = new Intent(Reports.this, display_reports.class);
                myIntent.putExtra("boundary",district+", "+block+", "+cluster);
                myIntent.putExtra("surveyId", surveyId);
                startActivity(myIntent);
            }
        });

        Calendar c=Calendar.getInstance();
        cyear=c.get(Calendar.YEAR);
        cdate=c.get(Calendar.DAY_OF_MONTH);
        cmonth=c.get(Calendar.MONTH);
        chour=c.get(Calendar.HOUR_OF_DAY);
        cminute=c.get(Calendar.MINUTE);

        EditText editText=(EditText) findViewById(R.id.report_start_date);
        editText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setdate(null, cyear, cmonth, cdate, R.id.report_start_date);
            }
        });
        editText=(EditText) findViewById(R.id.report_end_date);
        editText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setdate(null, cyear, cmonth, cdate, R.id.report_end_date);
            }
        });


        fill_dropdown(R.id.select_district_report, -1);

    }

    private void setdate(DatePicker view, int y, int m, int d, int id){
        DatePickerDialog dpd;
        editText = (EditText) findViewById(id);
        ret_str = "";
        dpd = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                editText.setText(String.format("%02d-%02d-%04d", dayOfMonth, monthOfYear + 1, year));
            }
        }, y, m, d);
        try {
            dpd.getDatePicker().setMaxDate(((Date) dateFormat.parse(String.format("%4d-%2d-%2d", y + 2, m + 1, d))).getTime() * 1);
            dpd.getDatePicker().setMinDate(((Date) dateFormat.parse(String.format("%4d-%2d-%2d", y, m + 1, d))).getTime() * 1);
        } catch (ParseException p) {
        }
        dpd.show();
    }

    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
        StringWithTags boundaryForSelector = (StringWithTags) parent.getItemAtPosition(pos);
        switch (parent.getId()){
            case R.id.select_district_report:
                fill_dropdown(R.id.select_block_report, Integer.parseInt(boundaryForSelector.id.toString()));
                district=((StringWithTags) parent.getItemAtPosition(pos)).string;
                break;
            case R.id.select_block_report:
                fill_dropdown(R.id.select_cluster_report, Integer.parseInt(boundaryForSelector.id.toString()));
                block=((StringWithTags) parent.getItemAtPosition(pos)).string;
                break;
            case R.id.select_cluster_report:
                select_schools(Integer.parseInt(boundaryForSelector.id.toString()));
                cluster=((StringWithTags) parent.getItemAtPosition(pos)).string;
                break;
        }
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

    private void fill_dropdown(int id, int parent){
        List<StringWithTags> stringWithTags = get_boundary_data(parent);
        Spinner spinner=(Spinner) findViewById(id);
        spinner.setOnItemSelectedListener(this);
        ArrayAdapter<StringWithTags> boundaryArrayAdapter=new ArrayAdapter<StringWithTags>(this, android.R.layout.simple_spinner_item, stringWithTags);
        spinner.setAdapter(boundaryArrayAdapter);
        boundaryArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
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

    private void select_schools(int boundary){
        ListView listView=(ListView) findViewById(R.id.school_list); //nothing
        List<StringWithTags> schoolList=get_school_data(boundary);
        ArrayAdapter<StringWithTags> schoolArrayAdapter=new ArrayAdapter<StringWithTags>(this, android.R.layout.simple_list_item_1, schoolList);
        listView.setAdapter(schoolArrayAdapter);
        ViewGroup.LayoutParams listViewParams = (ViewGroup.LayoutParams)listView.getLayoutParams();
        listViewParams.height = 400;
        listView.requestLayout();
    }
}

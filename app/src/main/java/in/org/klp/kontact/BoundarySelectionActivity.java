package in.org.klp.kontact;


import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import in.org.klp.kontact.data.StringWithTags;
import in.org.klp.kontact.db.Boundary;
import in.org.klp.kontact.db.KontactDatabase;
import in.org.klp.kontact.db.School;

public class BoundarySelectionActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    Long surveyId, bid;
    String surveyName, type="", district, block, cluster;
    SquidCursor<Boundary> boundary_cursor = null;
    SquidCursor<School> school_cursor = null;
    int cyear, cmonth, cdate, chour, cminute;
    EditText editText=null;
    SimpleDateFormat dateFormat;
    Context context = this;

    KontactDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boundary_selection);
        db = new KontactDatabase(this);

        SharedPreferences sharedPreferences=getSharedPreferences("boundary", MODE_PRIVATE);

        surveyId = getIntent().getLongExtra("surveyId", 0);
        surveyName = getIntent().getStringExtra("surveyName");
        type = getIntent().getStringExtra("type");
        Button bt_report = (Button) findViewById(R.id.report_button);
        EditText start_date=(EditText) findViewById(R.id.start_date);
        EditText end_date=(EditText) findViewById(R.id.end_date);
        ListView listView=(ListView) findViewById(R.id.school_list);
        if (type.equals("report")) {
            bt_report.setVisibility(View.VISIBLE);
            start_date.setVisibility(View.VISIBLE);
            end_date.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
        } else {
            bt_report.setVisibility(View.GONE);
            start_date.setVisibility(View.GONE);
            end_date.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
        }

        Calendar c=Calendar.getInstance();
        cyear=c.get(Calendar.YEAR);
        cdate=c.get(Calendar.DAY_OF_MONTH);
        cmonth=c.get(Calendar.MONTH);
        chour=c.get(Calendar.HOUR_OF_DAY);
        cminute=c.get(Calendar.MINUTE);

        EditText editText=(EditText) findViewById(R.id.start_date);
        editText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setdate(null, cyear, cmonth, cdate, R.id.start_date);
            }
        });
        editText=(EditText) findViewById(R.id.end_date);
        editText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setdate(null, cyear, cmonth, cdate, R.id.end_date);
            }
        });

        if (surveyId == 0 | TextUtils.isEmpty(surveyName)) {
            Intent intent = new Intent(BoundarySelectionActivity.this, MainActivity.class);
            startActivity(intent);
        }

        Spinner sp_district=(Spinner) findViewById(R.id.select_district);
        fill_dropdown(1, sp_district.getId(), 1);
        sp_district.setSelection(sharedPreferences.getInt("district",0));
        Spinner sp_block=(Spinner) findViewById(R.id.select_block);
        sp_block.setSelection(sharedPreferences.getInt("block",0));
        Spinner sp_cluster=(Spinner) findViewById(R.id.select_cluster);
        sp_cluster.setSelection(sharedPreferences.getInt("cluster",0));
        TextView textView = (TextView) findViewById(R.id.survey_details);
        textView.setText(surveyName);

        bt_report.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(BoundarySelectionActivity.this, Reports.class);
                intent.putExtra("surveyId", surveyId);
                intent.putExtra("surveyName", surveyName);
                intent.putExtra("bid", bid);
                intent.putExtra("boundary", district.toUpperCase() + ", " + block.toUpperCase() + ", " + cluster.toUpperCase());
                startActivity(intent);
            }
        });
    }

    private void setdate(DatePicker view, int y, int m, int d, int id){
        DatePickerDialog dpd;
        editText = (EditText) findViewById(id);
        dpd = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                editText.setText(String.format("%02d-%02d-%04d", dayOfMonth, monthOfYear + 1, year));
            }
        }, y, m, d);
        /*try {
           // dpd.getDatePicker().setMaxDate(((Date) dateFormat.parse(String.format("%4d-%2d-%2d", y + 2, m + 1, d))).getTime() * 1);
           // dpd.getDatePicker().setMinDate(((Date) dateFormat.parse(String.format("%4d-%2d-%2d", y, m + 1, d))).getTime() * 1);
        } catch (ParseException p) {
        }*/
        dpd.show();
    }

    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
        SharedPreferences sharedPreferences = getSharedPreferences("boundary", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        StringWithTags boundaryForSelector = (StringWithTags) parent.getItemAtPosition(pos);
        int viewid=parent.getId();
        switch (viewid){
            case R.id.select_district:
                fill_dropdown(1,R.id.select_block, Integer.parseInt(boundaryForSelector.id.toString()));
                editor.putInt("district", pos);
                district=((StringWithTags) parent.getItemAtPosition(pos)).string;
                break;
            case R.id.select_block:
                fill_dropdown(1,R.id.select_cluster, Integer.parseInt(boundaryForSelector.id.toString()));
                editor.putInt("block",pos);
                block=((StringWithTags) parent.getItemAtPosition(pos)).string;
                break;
            case R.id.select_cluster:
                fill_schools(R.id.school_list, Integer.parseInt(boundaryForSelector.id.toString()));
                cluster=((StringWithTags) parent.getItemAtPosition(pos)).string;
                editor.putInt("cluster", pos);
                bid=new Long(((StringWithTags) parent.getItemAtPosition(pos)).id.toString());
                break;
        }
        editor.commit();
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

    private void fill_dropdown(int type, int id, int parent){
        List<StringWithTags> stringWithTags=get_boundary_data(parent);
        Spinner spinner=(Spinner) findViewById(id);
        spinner.setOnItemSelectedListener(this);
        ArrayAdapter<StringWithTags> boundaryArrayAdapter=new ArrayAdapter<StringWithTags>(this, android.R.layout.simple_spinner_item, stringWithTags);
        spinner.setAdapter(boundaryArrayAdapter);
        boundaryArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    private void fill_schools(int id, int parent){
        ListView listView=(ListView) findViewById(id); //nothing
        List<StringWithTags> schoolList=get_school_data(parent);
        final ArrayAdapter<StringWithTags> schoolArrayAdapter=new ArrayAdapter<StringWithTags>(this, android.R.layout.simple_list_item_1, schoolList);
        listView.setAdapter(schoolArrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(BoundarySelectionActivity.this, QuestionActivity.class);
                intent.putExtra("surveyId", surveyId);
                intent.putExtra("surveyName", surveyName);
                intent.putExtra("schoolId", new Long(schoolArrayAdapter.getItem(i).id.toString()));
                startActivity(intent);
            }
        });
    }

    private List<StringWithTags> get_boundary_data(int parent) {
        Query listboundary = Query.select().from(Boundary.TABLE)
                    .where(Boundary.BOUNDARY_ID.eq(parent==1?null:parent));
        List<StringWithTags> boundaryList = new ArrayList<StringWithTags>();
        boundary_cursor = db.query(Boundary.class, listboundary);
        if (boundary_cursor.moveToFirst()) {
            do {
                StringWithTags boundary = new StringWithTags(boundary_cursor.getString(2), Integer.parseInt(boundary_cursor.getString(0)), Integer.parseInt(boundary_cursor.getString(3).equals("district") ? "1" : boundary_cursor.getString(1)));
                boundaryList.add(boundary);
            } while (boundary_cursor.moveToNext());
        }
        if (boundary_cursor !=null)
            boundary_cursor.close();
        return  boundaryList;
    }

    private List<StringWithTags> get_school_data(int parent){
        Query listschool = Query.select().from(School.TABLE)
                .where(School.BOUNDARY_ID.eq(parent));
        List<StringWithTags> schoolList = new ArrayList<StringWithTags>();
        school_cursor = db.query(School.class, listschool);
        if (school_cursor.moveToFirst()) {
            do {
                StringWithTags school = new StringWithTags(school_cursor.getString(2), Integer.parseInt(school_cursor.getString(0)),1);
                schoolList.add(school);
            } while (school_cursor.moveToNext());
        }
        if (school_cursor != null)
            school_cursor.close();
        return  schoolList;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.close();
    }
}

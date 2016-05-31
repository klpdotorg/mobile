package in.org.klp.kontact;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;

public class Reports extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    test_db db = new test_db(this);
    String district, block, cluster = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        Button bt=(Button) findViewById(R.id.bt_display_report);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Intent myIntent = new Intent(Reports.this, display_reports.class);
                myIntent.putExtra("boundary",district+", "+block+", "+cluster);
                startActivity(myIntent);
            }
        });

        /*
        db.add_school(new School(1, "School1", 1237));
        db.add_school(new School(2, "School2", 1237));
        db.add_school(new School(3, "School3", 1238));
        db.add_school(new School(4, "School4", 1238));
        db.add_school(new School(5, "School5", 1239));
        db.add_school(new School(6, "School6", 1239));
        db.add_school(new School(7, "School7", 8773));
        db.add_school(new School(8, "School8", 8773));*/

        fill_dropdown(R.id.select_district_report, 1);

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
        Spinner spinner=(Spinner) findViewById(id);
        spinner.setOnItemSelectedListener(this);
        ArrayAdapter<StringWithTags> boundaryArrayAdapter=new ArrayAdapter<StringWithTags>(this, android.R.layout.simple_spinner_item, db.get_all_boundaries(parent));
        spinner.setAdapter(boundaryArrayAdapter);
        boundaryArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    private void select_schools(int boundary){
        ListView listView=(ListView) findViewById(R.id.school_list); //nothing
        ArrayAdapter<StringWithTags> schoolArrayAdapter=new ArrayAdapter<StringWithTags>(this, android.R.layout.simple_list_item_1, db.get_all_schools(boundary));
        listView.setAdapter(schoolArrayAdapter);
        ViewGroup.LayoutParams listViewParams = (ViewGroup.LayoutParams)listView.getLayoutParams();
        listViewParams.height = 300;
        listView.requestLayout();
    }
}

package in.org.klp.kontact;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class display_reports extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_reports);

        String boundry_text= getIntent().getStringExtra("boundary");
        TextView tv=(TextView) findViewById(R.id.selected_boundaries);
        tv.setText(boundry_text);

        List<String> questions=new ArrayList<String>();
        questions.add("Drinking Water");
        questions.add("Seperate girls toilet");

        ListView listView=(ListView) findViewById(R.id.question_list);
        ArrayAdapter<String> questionArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, questions);
        listView.setAdapter(questionArrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                TextView textView=(TextView) findViewById(R.id.final_report);
                textView.setText(adapterView.getItemAtPosition(i).toString()+"\n60%");
            }});
    }
}

package in.org.klp.kontact;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import in.org.klp.kontact.data.SurveyDbHelper;
import in.org.klp.kontact.utils.SmartFragmentStatePagerAdapter;

public class display_reports extends AppCompatActivity implements display_report.OnFragmentInteractionListener {

    String surveyId;
    Context context=this;
    ViewPager vpPager;
    int qcount=0;
    private SmartFragmentStatePagerAdapter adapterViewPager;

    public void onFragmentInteraction(Uri uri) {
        //you can leave it empty
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_reports);

        String boundry_text= getIntent().getStringExtra("boundary");
        TextView tv=(TextView) findViewById(R.id.selected_boundaries);
        tv.setText(boundry_text);

        fetchQuestions();
        vpPager = (ViewPager) findViewById(R.id.vpPager);
        vpPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            // This method will be invoked when a new page becomes selected.
            @Override
            public void onPageSelected(int position) {
                //Toast.makeText(display_reports.this,
                        //"Selected page position: " + position, Toast.LENGTH_SHORT).show();
            }

            // This method will be invoked when the current page is scrolled
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                // Code goes here
            }

            // Called when the scroll state changes:
            // SCROLL_STATE_IDLE, SCROLL_STATE_DRAGGING, SCROLL_STATE_SETTLING
            @Override
            public void onPageScrollStateChanged(int state) {
                // Code goes here
            }
        });



        /*ListView listView=(ListView) findViewById(R.id.question_list);
        ArrayAdapter<String> questionArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, questions);
        listView.setAdapter(questionArrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                TextView textView=(TextView) findViewById(R.id.final_report);
                textView.setText(adapterView.getItemAtPosition(i).toString()+"\n60%");
            }});*/
    }

    public static class MyPagerAdapter extends SmartFragmentStatePagerAdapter {
        private static int NUM_ITEMS = 0;
        List<StringWithTags> list=null;

        public MyPagerAdapter(FragmentManager fragmentManager, int count, List<StringWithTags> result) {
            super(fragmentManager);
            NUM_ITEMS=count;
            list=result;
        }

        // Returns total number of pages
        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        // Returns the fragment to display for that page
        @Override
        public Fragment getItem(int position) {
            for (int i=0;i<NUM_ITEMS;i++)
                if (position==i)
                    return display_report.newInstance(String.valueOf(i), list.get(i).toString(), "60%(30/50)", "Block Average : 70%", "District Average : 30%" );
            return null;
            /*switch (position) {
                case 0: // Fragment # 0 - This will show FirstFragment
                    return display_report.newInstance("0", "Page # 1");
                case 1: // Fragment # 0 - This will show FirstFragment different title
                    return display_report.newInstance("1", "Page # 2");
                case 2: // Fragment # 1 - This will show SecondFragment
                    return display_report.newInstance("2", "Page # 3");
                default:
                    return null;
            }*/
        }

        // Returns the page title for the top indicator
        @Override
        public CharSequence getPageTitle(int position) {
            return "Page " + position;
        }

    }

    private void fetchQuestions() {
        FetchQuestionsTask questionsTask = new FetchQuestionsTask();
        questionsTask.execute();
    }

    public class FetchQuestionsTask extends AsyncTask<Void, Void, List<StringWithTags>>{

        private final String LOG_TAG = FetchQuestionsTask.class.getSimpleName();
        SurveyDbHelper dbHelper;

        @Override
        protected List<StringWithTags> doInBackground(Void... params) {

            dbHelper = new SurveyDbHelper(context);

            surveyId = getIntent().getStringExtra("surveyId");

            Cursor qg_cursor = dbHelper.list_questiongroups(surveyId);
            if (qg_cursor.getCount() >= 1) {

                String questionId;
                String questionText;
                String questiongroupId;

                qg_cursor.moveToNext();
                questiongroupId = qg_cursor.getString(0);
                qg_cursor.close();

                Cursor question_cursor = dbHelper.list_questions(questiongroupId);

                int count = 0;
                List<StringWithTags> resultStrs = new ArrayList<>();
                while(question_cursor.moveToNext()) {
                    questionId = question_cursor.getString(0);
                    questionText = question_cursor.getString(1);
                    questiongroupId = question_cursor.getString(2);
                    StringWithTags question = new StringWithTags(questionText, Integer.parseInt(questionId), 1);
                    resultStrs.add(question);
                    count++;
                    Log.v(LOG_TAG, "Survey: " + questionText);
                }
                qcount=count;
                return resultStrs;
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<StringWithTags> result) {
            if (result != null) {
                adapterViewPager = new MyPagerAdapter(getSupportFragmentManager(), qcount, result);
                vpPager.setAdapter(adapterViewPager);
            }
            super.onPostExecute(result);
        }
    }
}

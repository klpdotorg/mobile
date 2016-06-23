package in.org.klp.kontact;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Query;

import java.util.ArrayList;
import java.util.List;

import in.org.klp.kontact.data.SchoolContract;
import in.org.klp.kontact.data.StringWithTags;
import in.org.klp.kontact.db.KontactDatabase;
import in.org.klp.kontact.db.Question;
import in.org.klp.kontact.db.QuestionGroup;
import in.org.klp.kontact.db.QuestionGroupQuestion;
import in.org.klp.kontact.utils.SmartFragmentStatePagerAdapter;

public class Reports extends AppCompatActivity implements display_report.OnFragmentInteractionListener {

    private Long surveyId, questionGroupId, bid;
    Context context=this;
    ViewPager vpPager;
    private KontactDatabase db;
    int qcount=0;
    private SmartFragmentStatePagerAdapter adapterViewPager;
    String boundary="", agg="";

    public void onFragmentInteraction(Uri uri) {
        //you can leave it empty
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);
        db=new KontactDatabase(context);

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
                    return display_report.newInstance(String.valueOf(i), list.get(i).toString(), list.get(i).parent.toString(), "Block Average : 70%", "District Average : 30%" );
            return null;
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

    public class FetchQuestionsTask extends AsyncTask<Void, Void, ArrayList<Question>> {

        private final String LOG_TAG = FetchQuestionsTask.class.getSimpleName();

        @Override
        protected ArrayList<Question> doInBackground(Void... params) {
            Intent intent = getIntent();
            surveyId = intent.getLongExtra("surveyId", 0);

            SquidCursor<QuestionGroup> qgCursor = null;
            SquidCursor<QuestionGroupQuestion> qgqCursor = null;

            Query listQGquery = Query.select().from(QuestionGroup.TABLE)
                    .where(QuestionGroup.SURVEY_ID.eq(surveyId)).limit(1);
            qgCursor = db.query(QuestionGroup.class, listQGquery);

            try {
                while (qgCursor.moveToFirst()) {
                    questionGroupId = qgCursor.get(QuestionGroup.ID);
                    Query listQGQquery = Query.select().from(QuestionGroupQuestion.TABLE)
                            .where(QuestionGroupQuestion.QUESTIONGROUP_ID.eq(questionGroupId));
                    qgqCursor = db.query(QuestionGroupQuestion.class, listQGQquery);
                    ArrayList<Question> resultQuestions = new ArrayList<Question>();

                    int count=0;
                    while (qgqCursor.moveToNext()) {
                        Long qID = qgqCursor.get(QuestionGroupQuestion.QUESTION_ID);
                        Question question = db.fetch(Question.class, qID);
                        resultQuestions.add(question);
                        count++;
                    }

                    qcount=count;
                    return resultQuestions;
                }
            } finally {
                if (qgCursor != null) {
                    qgCursor.close();
                }
                if (qgqCursor != null) {
                    qgqCursor.close();
                }
                db.close();
            }
            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<Question> result) {
            List<StringWithTags> questions=new ArrayList<StringWithTags>();
            if (result != null) {
                for (Question question : result){
                    StringWithTags ques=new StringWithTags(question.getText(), question.getId(), fetchAnswers(new Long(question.getId())));
                    questions.add(ques);
                }
                adapterViewPager = new MyPagerAdapter(getSupportFragmentManager(), qcount, questions);
                vpPager.setAdapter(adapterViewPager);
            }
            super.onPostExecute(result);
        }
    }

    private String fetchAnswers(Long qid) {
        Intent intent = getIntent();
        bid = intent.getLongExtra("bid", 0);
        //Long qid=params[0];
        int schoolcount=0, responses=1, aggregate=0;

        Cursor cursor_sc, cursor_resp, cursor_agg, cursor_block_agg;

        cursor_sc = db.rawQuery("select count("+ SchoolContract.SchoolEntry._ID+") as count from school where boundary_id=" +String.valueOf(bid),null);
        try {
            while (cursor_sc.moveToNext()) {
                schoolcount=Integer.parseInt(cursor_sc.getString(0));
            }
        } finally {
            if (cursor_sc != null)
                cursor_sc.close();
        }

        cursor_resp = db.rawQuery("select count(_id) as count from school where boundary_id=" + bid +" and _id in " +
                "(select school_id from story where _id in (select story_id from answer where question_id=" + qid + " and text='true'))",null);
        try {
            while (cursor_resp.moveToNext()) {
                responses=Integer.parseInt(cursor_resp.getString(0));
            }
        } finally {
            if (cursor_resp != null)
                cursor_resp.close();
        }

        cursor_agg = db.rawQuery("select sum(case when text='true' then 1 else 0 end) as total from answer" +
                " where question_id=" + qid + " and story_id in (select _id from story where school_id in (" +
                "select _id from school where boundary_id=" + bid + "))", null);
        try {
            while (cursor_agg.moveToNext()) {
                aggregate=Integer.parseInt(cursor_agg.getString(0));
            }
        } catch (NumberFormatException e) {
            aggregate=0;
        }finally {
            if (cursor_agg != null)
                cursor_agg.close();
        }

        cursor_block_agg = db.rawQuery("select sum(case when text='true' then 1 else 0 end) as total from answer" +
                " where question_id=" + qid + " and story_id in (select _id from story where school_id in (" +
                "select _id from school where boundary_id=" + bid + "))", null);
        try {
            while (cursor_block_agg.moveToNext()) {
                aggregate=Integer.parseInt(cursor_agg.getString(0));
            }
        } catch (Exception e) {
            aggregate=0;
        }finally {
            if (cursor_agg != null)
                cursor_agg.close();
        }

        if (schoolcount==0)
            return String.valueOf(100*responses/1)+"%(0/"+schoolcount+")";
        else
            return String.valueOf(100*responses/schoolcount)+"%("+responses+"/"+schoolcount+")";
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }
}

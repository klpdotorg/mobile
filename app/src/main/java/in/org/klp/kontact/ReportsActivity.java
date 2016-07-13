package in.org.klp.kontact;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.yahoo.squidb.data.ICursor;
import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Query;

import java.util.ArrayList;
import java.util.List;

import in.org.klp.kontact.data.StringWithTags;
import in.org.klp.kontact.db.KontactDatabase;
import in.org.klp.kontact.db.Question;
import in.org.klp.kontact.db.QuestionGroup;
import in.org.klp.kontact.db.QuestionGroupQuestion;
import in.org.klp.kontact.utils.SmartFragmentStatePagerAdapter;

public class ReportsActivity extends AppCompatActivity implements ReportsFragment.OnFragmentInteractionListener {

    private Long surveyId, questionGroupId, bid, sdate, edate;
    Context context=this;
    ViewPager vpPager;
    private KontactDatabase db;
    int qcount=0;
    private SmartFragmentStatePagerAdapter adapterViewPager;

    public void onFragmentInteraction(Uri uri) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);
        db = ((KLPApplication) getApplicationContext()).getDb();

        String[] boundry_text= getIntent().getStringExtra("boundary").split(",");

        TextView tv=(TextView) findViewById(R.id.dist_name);
        tv.setText(boundry_text[0]);
        tv=(TextView) findViewById(R.id.blck_name);
        tv.setText(boundry_text[1]);
        tv=(TextView) findViewById(R.id.clst_name);
        tv.setText(boundry_text[2]);

        fetchQuestions();
        vpPager = (ViewPager) findViewById(R.id.vpPager);
        vpPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            // This method will be invoked when a new page becomes selected.
            @Override
            public void onPageSelected(int position) {
            }

            // This method will be invoked when the current page is scrolled
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            // Called when the scroll state changes:
            // SCROLL_STATE_IDLE, SCROLL_STATE_DRAGGING, SCROLL_STATE_SETTLING
            @Override
            public void onPageScrollStateChanged(int state) {
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
                    return ReportsFragment.newInstance(String.valueOf(i), list.get(i).toString(), list.get(i).parent.toString() );
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
                    StringWithTags ques=new StringWithTags(question.getTextKn() != null ? question.getTextKn() : question.getText(), question.getId(), fetchAnswers(new Long(question.getId())));
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
        sdate=intent.getLongExtra("sdate",0);
        edate=intent.getLongExtra("edate",0);
        int schoolcount=0, responses=0, ans=0, yes=0, no=0, dn=0, schoolwithresponse=0;

        ICursor cursor_sc, cursor_agg, cursor_block_agg;

        cursor_sc = db.rawQuery("select count(_id) as count from school where boundary_id=" +String.valueOf(bid),null);
        try {
            while (cursor_sc.moveToNext()) {
                schoolcount=Integer.parseInt(cursor_sc.getString(0));
            }
        } finally {
            if (cursor_sc != null)
                cursor_sc.close();
        }

        cursor_agg = db.rawQuery("select inst._id, sum(case when ans.text='Yes' then 1 else 0 end) as yes," +
                "sum(case when ans.text='No' then 1 else 0 end) as no, sum(case when ans.text not in ('Yes','No') " +
                "then 1 else 0 end) as dn," +
                "count(ans.text) as response from answer as ans, school as inst, story as st where ans.question_id=" + qid + " " +
                "and ans.story_id=st._id and st.school_id=inst._id and inst.boundary_id=" + bid +
                " and ans.created_at>=" + sdate + " and ans.created_at<=" + edate + " group by inst._id", null);
        try {
            while (cursor_agg.moveToNext()) {
                responses+=Integer.parseInt(cursor_agg.getString(4));
                yes+=Integer.parseInt(cursor_agg.getString(1));
                no+=Integer.parseInt(cursor_agg.getString(2));
                dn+=Integer.parseInt(cursor_agg.getString(3));
                schoolwithresponse+=1;
            }
        } catch (Exception e) {
        }finally {
            if (cursor_agg != null)
                cursor_agg.close();
        }

        //if (schoolcount==0 || responses==0)
        return schoolcount+"|"+schoolwithresponse+"|"+responses+"|"+yes+"|"+no+"|"+dn;
        //else
        //    return String.valueOf(100*ans/responses)+"|"+schoolcount+"|"+schoolwithresponse+"|"+responses+"|("+ans+"/"+responses+" Responses)";
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }
}

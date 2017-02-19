package in.org.klp.konnect;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import in.org.klp.konnect.adapters.StoryAdapter;
import in.org.klp.konnect.db.Boundary;
import in.org.klp.konnect.db.KontactDatabase;
import in.org.klp.konnect.db.School;
import in.org.klp.konnect.db.Story;
import in.org.klp.konnect.utils.SessionManager;

public class StoriesActivity extends AppCompatActivity {
    private StoryAdapter mStoryAdapter;
    private KontactDatabase db;
    private Long surveyId;
    private String surveyName;
    private Long boundaryId, sdate, edate;
    private SessionManager mSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stories);

        mSession = new SessionManager(getApplicationContext());
        HashMap<String, String> user = mSession.getUserDetails();
        String loggedinUserId = user.get(SessionManager.KEY_ID);

        db = ((KLPApplication) getApplicationContext()).getDb();
        surveyId = getIntent().getLongExtra("surveyId", 0);
        surveyName = getIntent().getStringExtra("surveyName");
        String bName = getIntent().getStringExtra("boundary");
        boundaryId = getIntent().getLongExtra("bid", 0);
        sdate = getIntent().getLongExtra("sdate", 0);
        edate = getIntent().getLongExtra("edate", 0);

        mStoryAdapter = new StoryAdapter(
                new ArrayList<Story>(),
                this, db, loggedinUserId
        );

        TextView tv_bdry = (TextView) findViewById(R.id.tv_bdry);
        TextView tv_daterange = (TextView) findViewById(R.id.tv_daterange);
        TextView tv_total_stories = (TextView) findViewById(R.id.tv_total_stories);
        TextView tv_total_by_user = (TextView) findViewById(R.id.tv_total_by_user);
        TextView tv_total_by_user_not_synced = (TextView) findViewById(R.id.tv_total_by_user_not_synced);

        tv_bdry.setText(bName);
        tv_daterange.setText(
                String.format(
                        getString(R.string.meta_daterange),
                        getDate(sdate, "dd-MM-yyyy"),
                        getDate(edate, "dd-MM-yyyy")
                )
        );

        SquidCursor<School> schoolsInBdryCursor = db.query(School.class, Query.select().from(School.TABLE).where(School.BOUNDARY_ID.eq(boundaryId)));
        List<Long> schids = new ArrayList<Long>();

        try {
            while (schoolsInBdryCursor.moveToNext()) {
                School sch = new School(schoolsInBdryCursor);
                schids.add(sch.getId());
            }
        } finally {
            schoolsInBdryCursor.close();
        }

        Query listStoryQuery = Query.select()
                .from(Story.TABLE)
                .where(Story.CREATED_AT.gte(sdate).and(Story.CREATED_AT.lte(edate).and(Story.SCHOOL_ID.in(schids))))
                .orderBy(Story.CREATED_AT.desc());
        SquidCursor<Story> storyCursor = db.query(Story.class, listStoryQuery);

        Query listStoryByUserQuery = listStoryQuery.where(Story.USER_ID.eq(loggedinUserId));
        SquidCursor<Story> storyByUserCursor = db.query(Story.class, listStoryByUserQuery);

        Query listStoryByUserNotSyncedQuery = listStoryQuery.where(Story.SYNCED.eq(0));
        SquidCursor<Story> storyByUserNSCursor = db.query(Story.class, listStoryByUserNotSyncedQuery);

        try {
            if (storyCursor.getCount() > 0) {
                // we have stories in DB, get them
                tv_total_stories.setText(
                        String.format(
                                getString(R.string.meta_total),
                                storyCursor.getCount()
                        )
                );
                tv_total_by_user.setText(
                        String.format(
                                getString(R.string.meta_total_by_user),
                                storyByUserCursor.getCount()
                        )
                );
                tv_total_by_user_not_synced.setText(
                        String.format(
                                getString(R.string.meta_total_by_user_nsy),
                                storyByUserNSCursor.getCount()
                        )
                );

                try {
                    while (storyCursor.moveToNext()) {
                        Story story = new Story(storyCursor);
                        mStoryAdapter.add(story);
                    }
                } finally {
                    if (storyCursor != null) {
                        storyCursor.close();
                    }
                }
            } else {
                Toast.makeText(StoriesActivity.this, "No stories found!", Toast.LENGTH_LONG).show();
            }
        } finally {
            storyCursor.close();
            storyByUserCursor.close();
            storyByUserNSCursor.close();
        }

        ListView listview = (ListView) findViewById(R.id.listview_story);
        listview.setAdapter(mStoryAdapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Long storyId = mStoryAdapter.getItem(i).getId();
                Log.d("Story", storyId.toString());
//                String storyName = mStoryAdapter.getItem(i).getName();
//                Intent intent = new Intent(getActivity(), StoryDetails.class);
//                intent.putExtra("storyId", storyId);
//                intent.putExtra("storyName", storyName);
//                startActivity(intent);
            }
        });
    }

    public static String getDate(long milliSeconds, String dateFormat)
    {
        // Create a DateFormatter object for displaying date in specified format.
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return formatter.format(calendar.getTime());
    }
}

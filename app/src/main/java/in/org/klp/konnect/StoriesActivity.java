package in.org.klp.konnect;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Query;

import java.util.ArrayList;
import java.util.HashMap;

import in.org.klp.konnect.adapters.StoryAdapter;
import in.org.klp.konnect.db.KontactDatabase;
import in.org.klp.konnect.db.Story;
import in.org.klp.konnect.utils.SessionManager;

public class StoriesActivity extends AppCompatActivity {
    private StoryAdapter mStoryAdapter;
    private KontactDatabase db;
    private Long surveyId;
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

        mStoryAdapter = new StoryAdapter(
                new ArrayList<Story>(),
                this, db, loggedinUserId
        );

        Query listStoryQuery = Query.select().from(Story.TABLE).orderBy(Story.CREATED_AT.desc());
        SquidCursor<Story> storyCursor = db.query(Story.class, listStoryQuery);

        if (db.countAll(Story.class) > 0) {
            // we have stories in DB, get them
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
}

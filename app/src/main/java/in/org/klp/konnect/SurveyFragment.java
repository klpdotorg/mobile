package in.org.klp.konnect;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Query;

import java.util.ArrayList;

import in.org.klp.konnect.adapters.SurveyAdapter;
import in.org.klp.konnect.db.KontactDatabase;
import in.org.klp.konnect.db.Survey;

public class SurveyFragment extends Fragment {

    private SurveyAdapter mSurveyAdapter;
    private KontactDatabase db;

    public SurveyFragment() {
    }

    @Override
    public void onCreate(Bundle SavedInstanceState) {
        super.onCreate(SavedInstanceState);
        setHasOptionsMenu(true);

        db = ((KLPApplication) getActivity().getApplicationContext()).getDb();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mSurveyAdapter = new SurveyAdapter(
                new ArrayList<Survey>(),
                getActivity()
        );
        View rootView = inflater.inflate(R.layout.fragment_survey, container, false);

        Query listSurveyQuery = Query.select().from(Survey.TABLE);
        SquidCursor<Survey> surveyCursor = db.query(Survey.class, listSurveyQuery);

        if (db.countAll(Survey.class) > 0) {
            // we have surveys in DB, get them
            try {
                while (surveyCursor.moveToNext()) {
                    Survey survey = new Survey(surveyCursor);
                    mSurveyAdapter.add(survey);
                }
            } finally {
                if (surveyCursor != null) {
                    surveyCursor.close();
                }
            }
        }

        ListView listview = (ListView) rootView.findViewById(R.id.listview_survey);
        listview.setAdapter(mSurveyAdapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Long surveyId = mSurveyAdapter.getItem(i).getId();
                String surveyName = mSurveyAdapter.getItem(i).getName();
                Intent intent = new Intent(getActivity(), SurveyDetails.class);
                intent.putExtra("surveyId", surveyId);
                intent.putExtra("surveyName", surveyName);
                startActivity(intent);
            }
        });

        return rootView;
    }
}

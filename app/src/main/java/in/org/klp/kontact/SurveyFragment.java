package in.org.klp.kontact;

import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Query;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import in.org.klp.kontact.adapters.SurveyAdapter;
import in.org.klp.kontact.data.SurveyDbHelper;
import in.org.klp.kontact.db.Boundary;
import in.org.klp.kontact.db.KontactDatabase;
import in.org.klp.kontact.db.Survey;

public class SurveyFragment extends Fragment {

    private SurveyAdapter mSurveyAdapter;
    private KontactDatabase db;

    public SurveyFragment() {
    }

    @Override
    public void onCreate(Bundle SavedInstanceState) {
        super.onCreate(SavedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.surveyfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            updateSurvey();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateSurvey() {
        FetchSurveyTask surveyTask = new FetchSurveyTask();
        surveyTask.execute();
    }

    @Override
    public void onStart() {
        super.onStart();
        updateSurvey();
    }

    // FIXME: String parsing to get the survey_id.
    // Should replace with proper cursor adapter implementation.
    public String getSurveyId(String str) {
        String[] tokens = str.split(":");
        return tokens[0];
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mSurveyAdapter = new SurveyAdapter(
                new ArrayList<Survey>(),
                getActivity()
        );

        View rootView = inflater.inflate(R.layout.fragment_survey, container, false);

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
            }});

        return rootView;
    }

    public class FetchSurveyTask extends AsyncTask<Void, Void, ArrayList<Survey>> {

        private final String LOG_TAG = FetchSurveyTask.class.getSimpleName();
        SurveyDbHelper dbHelper;

        @Override
        protected ArrayList<Survey> doInBackground(Void... params) {
            db = new KontactDatabase(getActivity());

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String surveyJsonStr = null;

            Query listSurveyQuery = Query.select().from(Survey.TABLE);
            SquidCursor<Survey> surveyCursor = db.query(Survey.class, listSurveyQuery);
            ArrayList<Survey> resultSurveys = new ArrayList<Survey>();

            if (db.countAll(Survey.class) > 0) {
                // we have surveys in DB, get them
                try {
                    while (surveyCursor.moveToNext()) {
                        Survey survey = new Survey(surveyCursor);
                        resultSurveys.add(survey);
                    }
                    return resultSurveys;
                } finally {
                    if (surveyCursor != null) {
                        surveyCursor.close();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<Survey> result) {
            if (result != null) {
                mSurveyAdapter.clear();
                for (Survey survey : result) {
                    mSurveyAdapter.add(survey);
                }
            }
            super.onPostExecute(result);
        }

        private void saveSurveyDataFromJson(String surveyJsonStr)
                throws JSONException {

            dbHelper = new SurveyDbHelper(getActivity());

            final String FEATURES = "features";
            JSONObject surveyJson = new JSONObject(surveyJsonStr);
            JSONArray surveyArray = surveyJson.getJSONArray(FEATURES);

            for (int i = 0; i < surveyArray.length(); i++) {

                String surveyId;
                String surveyName;
                String surveyPartner;

                // Get the JSON object representing the survey
                JSONObject surveyObject = surveyArray.getJSONObject(i);

                // Get the JSON object representing the partner
                JSONObject partnerObject = surveyObject.getJSONObject("partner");

                surveyId = surveyObject.getString("id");
                surveyName = surveyObject.getString("name");
                surveyPartner = partnerObject.getString("name");

                try {
                    dbHelper.insert_survey(Integer.parseInt(surveyId), surveyPartner, surveyName);
                } catch (SQLiteException e) {
                    Log.v(LOG_TAG, "Survey Insert Error: " + e.toString());
                }
            }

        }

    }
}

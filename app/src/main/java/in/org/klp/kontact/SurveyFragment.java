package in.org.klp.kontact;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

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

import in.org.klp.kontact.data.SurveyDbHelper;

public class SurveyFragment extends Fragment {

    private ArrayAdapter<String> mSurveyAdapter;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mSurveyAdapter = new ArrayAdapter<String>(
                getActivity(),
                R.layout.list_item_survey,
                R.id.list_item_survey_textview,
                new ArrayList<String>()
        );

        View rootView = inflater.inflate(R.layout.fragment_survey, container, false);

        ListView listview = (ListView) rootView.findViewById(R.id.listview_survey);
        listview.setAdapter(mSurveyAdapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String survey_id = mSurveyAdapter.getItem(i);
                Intent intent = new Intent(getActivity(), QuestionActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, survey_id);
                startActivity(intent);
            }});

        return rootView;
    }

    public class FetchSurveyTask extends AsyncTask<Void, Void, String[]> {

        private final String LOG_TAG = FetchSurveyTask.class.getSimpleName();
        SurveyDbHelper dbHelper;

        @Override
        protected String[] doInBackground(Void... params) {

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String surveyJsonStr = null;

            dbHelper = new SurveyDbHelper(getActivity());

            Cursor cursor = dbHelper.list_surveys();
            while(cursor.moveToNext()) {
                Log.v(LOG_TAG, "Survey Insert Error: " + cursor.getColumnName(1).toString());
            }

            try {
                final String SURVEY_BASE_URL = "http://dev.klp.org.in/api/v1/surveys/";

                Uri builtUri = Uri.parse(SURVEY_BASE_URL).buildUpon().build();

                URL url = new URL(builtUri.toString());

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                surveyJsonStr = buffer.toString();

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
            try {
                return getSurveyDataFromJson(surveyJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {
                mSurveyAdapter.clear();
                for (String surveyStr : result) {
                    mSurveyAdapter.add(surveyStr);
                }
            }
            super.onPostExecute(result);
        }

        private String[] getSurveyDataFromJson(String surveyJsonStr)
                throws JSONException {

            dbHelper = new SurveyDbHelper(getActivity());

            final String FEATURES = "features";
            JSONObject surveyJson = new JSONObject(surveyJsonStr);
            JSONArray surveyArray = surveyJson.getJSONArray(FEATURES);

            String[] resultStrs = new String[surveyArray.length()];
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

                resultStrs[i] = surveyId + " - " + surveyName;
            }

            return resultStrs;

        }

    }
}

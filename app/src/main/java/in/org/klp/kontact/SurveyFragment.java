package in.org.klp.kontact;

import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
            FetchSurveyTask surveyTask = new FetchSurveyTask();
            surveyTask.execute();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        String[] data = {
                "Akshara Primary School Survey",
                "GKA Survey",
                "Akshaya Patra Survey",
                "PreSchool Survey",
                "EkStep Survey",
                "Notorious Survey",
                "The Ultimate Survey"
        };

        List<String> Surveys = new ArrayList<String>(Arrays.asList(data));

        mSurveyAdapter = new ArrayAdapter<String>(
                getActivity(),
                R.layout.list_item_survey,
                R.id.list_item_survey_textview,
                Surveys
        );

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        ListView listview = (ListView) rootView.findViewById(R.id.listview_survey);
        listview.setAdapter(mSurveyAdapter);

        return rootView;
    }

    public class FetchSurveyTask extends AsyncTask<Void, Void, String[]> {

        private final String LOG_TAG = FetchSurveyTask.class.getSimpleName();

        @Override
        protected String[] doInBackground(Void... params) {

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String surveyJsonStr = null;

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

            final String FEATURES = "features";

            JSONObject surveyJson = new JSONObject(surveyJsonStr);
            JSONArray surveyArray = surveyJson.getJSONArray(FEATURES);

            String[] resultStrs = new String[17];
            for (int i = 0; i < surveyArray.length(); i++) {

                String surveyId;
                String sourceVersion;
                String sourceName;
                String startDate;
                String endDate;

                // Get the JSON object representing the survey
                JSONObject surveyObject = surveyArray.getJSONObject(i);

                surveyId = surveyObject.getString("id");
                sourceVersion = surveyObject.getString("version");
                sourceName = surveyObject.getString("source");
                startDate = surveyObject.getString("start_date");
                endDate = surveyObject.getString("end_date");

                resultStrs[i] = sourceVersion + " - " + sourceName;
            }
            return resultStrs;

        }

    }
}

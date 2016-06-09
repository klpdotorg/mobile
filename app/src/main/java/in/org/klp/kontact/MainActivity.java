package in.org.klp.kontact;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import in.org.klp.kontact.data.SurveyDbHelper;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Button notification_button = (Button) findViewById(R.id.notification_button);
        notification_button.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v) {
                createNotification();
            }
        });

        Button survey_button = (Button) findViewById(R.id.survey_button);
        survey_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Intent myIntent = new Intent(MainActivity.this, SurveyActivity.class);
                startActivity(myIntent);
            }
        });

        Button sync_button = (Button) findViewById(R.id.sync_button);
        sync_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                updateSurvey();
            }
        });
    }

    private void createNotification() {
        // Placeholder to create notification about sync.
        // new DownloadTask(getApplicationContext()).execute(0);
    }

    private void updateSurvey() {
        FetchSurveyTask surveyTask = new FetchSurveyTask();
        surveyTask.execute();
    }

    public class FetchSurveyTask extends AsyncTask<Void, Void, String[]> {

        private final String LOG_TAG = FetchSurveyTask.class.getSimpleName();
        SurveyDbHelper dbHelper;

        private String processURL(String apiURL, String type) {
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String JsonStr = null;

            try {
                final String SURVEY_BASE_URL = apiURL;

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
                JsonStr = buffer.toString();

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
                if (type == "survey") {
                    saveSurveyDataFromJson(JsonStr);
                }
                else if (type == "questiongroup") {
                    saveQuestiongroupDataFromJson(JsonStr);
                }
                else {
                    saveQuestionDataFromJson(JsonStr);
                }
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected String[] doInBackground(Void... params) {

            // Populate surveys
            processURL("http://dev.klp.org.in/api/v1/surveys/", "survey");

            // Populate questiongroups
            processURL("http://dev.klp.org.in/api/v1/questiongroups/?source=sms", "questiongroup");

            // Populate questions
            processURL("http://dev.klp.org.in/api/v1/questions/?source=sms", "question");

            return null;

        }

        @Override
        protected void onPostExecute(String[] result) {
//            if (result != null) {
//                mSurveyAdapter.clear();
//                for (String surveyStr : result) {
//                    mSurveyAdapter.add(surveyStr);
//                }
//            }
            super.onPostExecute(result);
        }

        private void saveQuestiongroupDataFromJson(String questiongroupJsonStr)
                throws JSONException {
            dbHelper = new SurveyDbHelper(MainActivity.this);

            final String FEATURES = "features";
            JSONObject questiongroupJson = new JSONObject(questiongroupJsonStr);
            JSONArray questiongroupArray = questiongroupJson.getJSONArray(FEATURES);

            for (int i = 0; i < questiongroupArray.length(); i++) {

                Integer groupId;
                Integer status;
                Integer start_date;
                Integer end_date;
                Integer version;
                Integer surveyId;
                String source;

                // Get the JSON object representing the survey
                JSONObject questiongroupObject = questiongroupArray.getJSONObject(i);

                // Get the JSON object representing the partner
                JSONObject surveyObject = questiongroupObject.getJSONObject("survey");

                groupId = questiongroupObject.getInt("id");
                status = questiongroupObject.getInt("status");
                start_date = questiongroupObject.getInt("start_date");
                end_date = questiongroupObject.getInt("end_date");
                version = questiongroupObject.getInt("version");
                source = questiongroupObject.getString("source");
                surveyId = surveyObject.getInt("id");

                try {
                    dbHelper.insert_questiongroup(groupId, status, start_date, end_date, version, source, surveyId);
                } catch (SQLiteException e) {
                    Log.v(LOG_TAG, "Questiongroup Insert Error: " + e.toString());
                }

            }
        }

        private void saveSurveyDataFromJson(String surveyJsonStr)
                throws JSONException {

            dbHelper = new SurveyDbHelper(MainActivity.this);

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

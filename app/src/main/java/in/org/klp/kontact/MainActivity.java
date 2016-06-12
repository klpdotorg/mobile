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

        private String processSchoolsUrl(String apiURL) {
            while(!apiURL.equals("null")) {
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
                    apiURL = saveSchoolDataFromJson(JsonStr);
                } catch (JSONException e) {
                    Log.e(LOG_TAG, e.getMessage(), e);
                    e.printStackTrace();
                }
            }

            return null;
        }

        private String processURL(String apiURL, String type) {

            if (type.equals("school")) {
                return processSchoolsUrl(apiURL);
            }
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
                if (type.equals("survey")) {
                    saveSurveyDataFromJson(JsonStr);
                }
                else if (type.equals("questiongroup")) {
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
            processURL("https://dev.klp.org.in/api/v1/surveys/", "survey");

            // Populate questiongroups
            processURL("https://dev.klp.org.in/api/v1/questiongroups/?source=sms", "questiongroup");

            // Populate questions
            processURL("https://dev.klp.org.in/api/v1/questions/?source=sms", "question");

            // Populate schools
            processURL("https://dev.klp.org.in/api/v1/schools/list/", "school");

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

        private String saveSchoolDataFromJson(String schoolJsonStr)
                throws JSONException {
            dbHelper = new SurveyDbHelper(MainActivity.this);

            final String FEATURES = "features";
            JSONObject schoolJson = new JSONObject(schoolJsonStr);
            String next = schoolJson.getString("next");
            JSONArray schoolArray = schoolJson.getJSONArray(FEATURES);

            for (int i = 0; i < schoolArray.length(); i++) {

                Integer schoolId;
                Integer boundaryId;
                Integer diseCode;
                String name;

                JSONObject schoolObject = schoolArray.getJSONObject(i);
                JSONObject boundaryObject = schoolObject.getJSONObject("boundary");

                schoolId = schoolObject.getInt("id");
                boundaryId = boundaryObject.getInt("id");
                diseCode = schoolObject.getInt("dise_info");
                name = schoolObject.getString("name");

                try {
                    dbHelper.insert_school(schoolId, boundaryId, diseCode, name);
                } catch (SQLiteException e) {
                    Log.v(LOG_TAG, "School Insert Error: " + e.toString());
                }
            }
            return next;
        }


        private void saveQuestionDataFromJson(String questionJsonStr)
                throws JSONException {
            dbHelper = new SurveyDbHelper(MainActivity.this);

            final String FEATURES = "features";
            JSONObject questionJson = new JSONObject(questionJsonStr);
            JSONArray questionArray = questionJson.getJSONArray(FEATURES);

            for (int i = 0; i < questionArray.length(); i++) {

                Integer questionId;
                String text;
                String key;
                String options;
                String type;
                String school_type;

                JSONObject questionObject = questionArray.getJSONObject(i);
                JSONObject schoolObject = questionObject.getJSONObject("school_type");
                JSONArray questiongroupSetArray = questionObject.getJSONArray("questiongroup_set");

                questionId = questionObject.getInt("id");
                text = questionObject.getString("text");
                key = questionObject.getString("key");
                options = questionObject.getString("options");
                type = questionObject.getString("question_type");
                school_type = schoolObject.getString("name");

                try {
                    dbHelper.insert_question(questionId, text, key, options, type, school_type);
                } catch (SQLiteException e) {
                    Log.v(LOG_TAG, "Questiongroup Insert Error: " + e.toString());
                }

                for (int j = 0; j < questiongroupSetArray.length(); j++) {
                    Integer throughId;
                    Integer questiongroupId;
                    Integer sequence;

                    Integer status;
                    String source;

                    JSONObject questiongroupObject = questiongroupSetArray.getJSONObject(j);

                    throughId = questiongroupObject.getInt("through_id");
                    questiongroupId = questiongroupObject.getInt("questiongroup");
                    sequence = questiongroupObject.getInt("sequence");
                    status = questiongroupObject.getInt("status");
                    source = questiongroupObject.getString("source");

                    if (source.equals("sms")) {
                        if (status.equals(1)) {
                            try {
                                dbHelper.insert_questiongroupquestion(throughId, questionId, questiongroupId, sequence);
                            } catch (SQLiteException e) {
                                Log.v(LOG_TAG, "QuestiongroupQuestion Insert Error: " + e.toString());
                            }
                        }
                    }

                }
            }
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

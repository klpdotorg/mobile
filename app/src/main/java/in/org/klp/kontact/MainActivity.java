package in.org.klp.kontact;

import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;

import in.org.klp.kontact.data.StringWithTags;
import in.org.klp.kontact.db.KontactDatabase;
import in.org.klp.kontact.db.Survey;
import in.org.klp.kontact.db.Question;
import in.org.klp.kontact.db.QuestionGroup;
import in.org.klp.kontact.db.QuestionGroupQuestion;
import in.org.klp.kontact.db.Boundary;
import in.org.klp.kontact.db.School;

import in.org.klp.kontact.data.SurveyDbHelper;
import in.org.klp.kontact.utils.KLPVolleySingleton;
import in.org.klp.kontact.utils.SessionManager;

public class MainActivity extends AppCompatActivity {
    private SessionManager mSession;
    private Snackbar snackbarSync;
    private FetchSurveyTask surveyTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSession = new SessionManager(getApplicationContext());
        mSession.checkLogin();

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
                snackbarSync = Snackbar.make(view, "Getting data from server...", Snackbar.LENGTH_INDEFINITE)
                    .setAction("STOP", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            surveyTask.cancel(true);
                            snackbarSync.dismiss();
                        }
                    });
                snackbarSync.show();
                updateSurvey();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            logoutUser();
        }

        return super.onOptionsItemSelected(item);
    }

    public void logoutUser() {
        mSession.logoutUser();
    }

    private void updateSurvey() {
        surveyTask = new FetchSurveyTask();
        surveyTask.execute();
    }

    public class FetchSurveyTask extends AsyncTask<Void, Void, String[]> {

        private final String LOG_TAG = FetchSurveyTask.class.getSimpleName();
        SurveyDbHelper dbHelper;
        private KontactDatabase db;

        private String processPaginatedURL(String apiURL, String type) {
            int count = 0;
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
                    if (type.equals("school")) {
                        apiURL = saveSchoolDataFromJson(JsonStr);
                    }
                    else {
                        apiURL = saveBoundaryDataFromJson(JsonStr);
                    }
                } catch (JSONException e) {
                    Log.e(LOG_TAG, e.getMessage(), e);
                    e.printStackTrace();
                }
                count++;
                Log.v(LOG_TAG, "Page count is: " + Integer.toString(count));
            }

            return null;
        }

        private void processURL(String apiURL, final String type) {

            if (type.equals("school") || type.equals("boundary")) {
                processPaginatedURL(apiURL, type);
            }
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String JsonStr = null;

            final String BASE_URL = apiURL;

            StringRequest stringRequest = new StringRequest(Request.Method.GET,
                    BASE_URL, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        if (type.equals("survey")) {
                            saveSurveyDataFromJson(response);
                        } else if (type.equals("questiongroup")) {
                            saveQuestiongroupDataFromJson(response);
                        } else {
                            saveQuestionDataFromJson(response);
                        }
                    } catch (JSONException e) {
                        Log.e(LOG_TAG, e.getMessage(), e);
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.v(LOG_TAG, "Error parsing the survey results");
                }
            });
            KLPVolleySingleton.getInstance(MainActivity.this).addToRequestQueue(stringRequest);
        }

        @Override
        protected String[] doInBackground(Void... params) {
            Log.d(this.toString(), BuildConfig.HOST);

            // Populate surveys
            processURL(BuildConfig.HOST + "/api/v1/surveys/?source=mobile", "survey");

            // Populate questiongroups
            processURL(BuildConfig.HOST + "/api/v1/questiongroups/?source=mobile", "questiongroup");

            // Populate questions
            processURL(BuildConfig.HOST + "/api/v1/questions/?source=mobile", "question");

            // Populate school
//            if (!isCancelled()) {
//                // because this one takes time,
//                // just checking if the async task has been cancelled before starting
//                processURL(BuildConfig.HOST + "/api/v1/schools/list/", "school");
//            }

            // Populate boundaries
            processURL(BuildConfig.HOST + "/api/v1/boundary/admin3s", "boundary");
            processURL(BuildConfig.HOST + "/api/v1/boundary/admin2s", "boundary");
            processURL(BuildConfig.HOST + "/api/v1/boundary/admin1s", "boundary");

            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (snackbarSync != null) {
                snackbarSync.dismiss();
            }
            super.onPostExecute(result);
        }

        private String saveBoundaryDataFromJson(String boundaryJsonStr)
                throws JSONException {
            db = new KontactDatabase(MainActivity.this);

            final String FEATURES = "features";
            JSONObject boundaryJson = new JSONObject(boundaryJsonStr);
            String next = boundaryJson.getString("next");
            JSONArray boundaryArray = boundaryJson.getJSONArray(FEATURES);

            for (int i = 0; i < boundaryArray.length(); i++) {

                Integer boundaryId;
                long parentId;
                String name;
                String hierarchy;
                String school_type;

                JSONObject boundaryObject = boundaryArray.getJSONObject(i);
                if (boundaryObject.has("parent")) {
                    JSONObject parentObject = boundaryObject.getJSONObject("parent");
                    parentId = parentObject.getInt("id");
                }
                else {
                    parentId = 1;
                }

                boundaryId = boundaryObject.getInt("id");
                name = boundaryObject.getString("name");
                hierarchy = boundaryObject.getString("type");
                school_type = boundaryObject.getString("school_type");

                Boundary boundary = new Boundary()
                        .setId(boundaryId)
                        .setBoundaryId(parentId)
                        .setName(name)
                        .setHierarchy(hierarchy)
                        .setType(school_type);
                db.insertWithId(boundary);
            }
            return next;
        }


        private String saveSchoolDataFromJson(String schoolJsonStr)
                throws JSONException {
            db = new KontactDatabase(MainActivity.this);

            final String FEATURES = "features";
            JSONObject schoolJson = new JSONObject(schoolJsonStr);
            String next = schoolJson.getString("next");
            JSONArray schoolArray = schoolJson.getJSONArray(FEATURES);

            for (int i = 0; i < schoolArray.length(); i++) {

                Integer schoolId;
                long boundaryId;
                String name;

                JSONObject schoolObject = schoolArray.getJSONObject(i);
                JSONObject boundaryObject = schoolObject.getJSONObject("boundary");

                schoolId = schoolObject.getInt("id");
                boundaryId = boundaryObject.getInt("id");
                name = schoolObject.getString("name");

                School school = new School()
                        .setId(schoolId)
                        .setBoundaryId(boundaryId)
                        .setName(name);
                db.insertWithId(school);
            }
            return next;
        }


        private void saveQuestionDataFromJson(String questionJsonStr)
                throws JSONException {
            db = new KontactDatabase(MainActivity.this);

            final String FEATURES = "features";
            JSONObject questionJson = new JSONObject(questionJsonStr);
            JSONArray questionArray = questionJson.getJSONArray(FEATURES);

            for (int i = 0; i < questionArray.length(); i++) {

                long questionId;
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

                Question question = new Question()
                        .setId(questionId)
                        .setText(text)
                        .setKey(key)
                        .setOptions(options)
                        .setType(type)
                        .setSchoolType(school_type);
                db.insertWithId(question);

                for (int j = 0; j < questiongroupSetArray.length(); j++) {
                    Integer throughId;
                    long questiongroupId;
                    Integer sequence;

                    Integer status;
                    String source;

                    JSONObject questiongroupObject = questiongroupSetArray.getJSONObject(j);

                    throughId = questiongroupObject.getInt("through_id");
                    questiongroupId = questiongroupObject.getInt("questiongroup");
                    sequence = questiongroupObject.getInt("sequence");
                    status = questiongroupObject.getInt("status");
                    source = questiongroupObject.getString("source");

                    if (source.equals("mobile")) {
                        if (status.equals(1)) {
                            QuestionGroupQuestion questionGroupQuestion = new QuestionGroupQuestion()
                                    .setId(throughId)
                                    .setQuestionId(questionId)
                                    .setQuestiongroupId(questiongroupId)
                                    .setSequence(sequence);
                            db.insertWithId(questionGroupQuestion);
                        }
                    }

                }
            }
        }


        private void saveQuestiongroupDataFromJson(String questiongroupJsonStr)
                throws JSONException {
            db = new KontactDatabase(MainActivity.this);

            final String FEATURES = "features";
            JSONObject questiongroupJson = new JSONObject(questiongroupJsonStr);
            JSONArray questiongroupArray = questiongroupJson.getJSONArray(FEATURES);

            for (int i = 0; i < questiongroupArray.length(); i++) {

                Integer groupId;
                Integer status;
                long start_date;
                long end_date;
                Integer version;
                long surveyId;
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

                QuestionGroup questionGroup = new QuestionGroup()
                        .setId(groupId)
                        .setStatus(status)
                        .setStartDate(start_date)
                        .setEndDate(end_date)
                        .setVersion(version)
                        .setSource(source)
                        .setSurveyId(surveyId);
                db.insertWithId(questionGroup);
            }
        }

        private void saveSurveyDataFromJson(String surveyJsonStr)
                throws JSONException {
            db = new KontactDatabase(MainActivity.this);

            final String FEATURES = "features";
            JSONObject surveyJson = new JSONObject(surveyJsonStr);
            JSONArray surveyArray = surveyJson.getJSONArray(FEATURES);

            for (int i = 0; i < surveyArray.length(); i++) {

                Integer surveyId;
                String surveyName;
                String surveyPartner;

                // Get the JSON object representing the survey
                JSONObject surveyObject = surveyArray.getJSONObject(i);

                // Get the JSON object representing the partner
                JSONObject partnerObject = surveyObject.getJSONObject("partner");

                surveyId = surveyObject.getInt("id");
                surveyName = surveyObject.getString("name");
                surveyPartner = partnerObject.getString("name");

                Survey survey = new Survey()
                        .setId(surveyId)
                        .setName(surveyName)
                        .setPartner(surveyPartner);
                db.insertWithId(survey);
            }

        }

    }

}

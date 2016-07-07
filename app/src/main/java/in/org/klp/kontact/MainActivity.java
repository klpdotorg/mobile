package in.org.klp.kontact;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.data.TableModel;
import com.yahoo.squidb.sql.Property;
import com.yahoo.squidb.sql.Query;
import com.yahoo.squidb.sql.Update;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import in.org.klp.kontact.db.Answer;
import in.org.klp.kontact.db.Boundary;
import in.org.klp.kontact.db.KontactDatabase;
import in.org.klp.kontact.db.Question;
import in.org.klp.kontact.db.QuestionGroup;
import in.org.klp.kontact.db.QuestionGroupQuestion;
import in.org.klp.kontact.db.School;
import in.org.klp.kontact.db.Story;
import in.org.klp.kontact.db.Survey;
import in.org.klp.kontact.utils.KLPVolleySingleton;
import in.org.klp.kontact.utils.SessionManager;

import static junit.framework.Assert.assertTrue;

public class MainActivity extends AppCompatActivity {
    private SessionManager mSession;
    private Snackbar snackbarSync;
    private SyncDownloadTask downloadTask;
    private ProgressDialog progressDialog = null;
    private KontactDatabase db;
    private Integer syncRequestCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSession = new SessionManager(getApplicationContext());
        mSession.checkLogin();

        Button survey_button = (Button) findViewById(R.id.survey_button);
        survey_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent myIntent = new Intent(MainActivity.this, SurveyActivity.class);
                startActivity(myIntent);
            }
        });

        Button sync_button = (Button) findViewById(R.id.sync_button);
        sync_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doSync();
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

    private void doSync() {
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setTitle("Uploading");
        progressDialog.setMessage("Uploading responses to server");
        progressDialog.setIndeterminate(true);
        progressDialog.show();

        syncUpload();
    }

    public void syncUpload() {
        HashMap<String, String> user = mSession.getUserDetails();

        db = ((KLPApplication) getApplicationContext()).getDb();
        Query listStoryQuery = Query.select().from(Story.TABLE)
                .where(Story.SYNCED.eq(0));
        SquidCursor<Story> storiesCursor = db.query(Story.class, listStoryQuery);
        SquidCursor<Answer> answerCursor = null;

        JSONObject requestJson = new JSONObject();
        JSONArray storyArray = new JSONArray();

        try {
            while (storiesCursor.moveToNext()) {
                Story story = new Story(storiesCursor);
                JSONObject storyJson = db.modelObjectToJson(story);

                answerCursor = db.query(Answer.class,
                        Query.select().from(Answer.TABLE)
                                .where(Answer.STORY_ID.eq(story.getId()))
                );

                JSONArray answerArray = new JSONArray();
                while (answerCursor.moveToNext()) {
                    Answer answer = new Answer(answerCursor);
                    JSONObject answerJson = db.modelObjectToJson(answer);
                    answerArray.put(answerJson);
                }
                storyJson.put("answers", answerArray);
                storyArray.put(storyJson);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            if (storiesCursor != null) storiesCursor.close();
            if (answerCursor != null) answerCursor.close();
        }
        try {
            requestJson.put("stories", storyArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final String SYNC_URL = BuildConfig.HOST + "/api/v1/sync";

        RequestFuture<JSONObject> future = RequestFuture.newFuture();
        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.POST, SYNC_URL, requestJson, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Log.d(this.toString(), response.toString(4));
                            // TODO: show error
                            String error = response.get("error").toString();

                            JSONArray success = response.getJSONArray("success");
                            for (int i = 0; i < success.length(); i++) {
                                Update storyUpdate = Update.table(Story.TABLE)
                                        .set(Story.SYNCED, 1)
                                        .where(Story.ID.eq(success.get(i)));
                                db.update(storyUpdate);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        progressDialog.setMessage("Uploading finished");
                        progressDialog.setTitle("Downloading");

                        SyncDownloadTask downloadTask = new SyncDownloadTask();
                        downloadTask.execute();
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub
                        error.printStackTrace();
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> user = mSession.getUserDetails();
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization", "Token " + user.get("token"));
                return headers;
            }
        };
        KLPVolleySingleton.getInstance(MainActivity.this).addToRequestQueue(jsObjRequest);

    }

    public class SyncDownloadTask extends AsyncTask<Void, String, String[]> {

        private final String LOG_TAG = SyncDownloadTask.class.getSimpleName();
        private KontactDatabase db;

        private void processPaginatedURL(String apiURL, final String type) {
            StringRequest stringRequest = new StringRequest(Request.Method.GET,
                    apiURL, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    publishProgress("Downloading " + type);
                    String next;
                    try {
                        if (type.equals("school")) {
                            next = saveSchoolDataFromJson(response);
                        } else {
                            next = saveBoundaryDataFromJson(response);
                        }
                        Log.v(LOG_TAG, next);
                        if (!next.equals("null")) {
                            processPaginatedURL(next, type);
                        }
                    } catch (JSONException e) {
                        Log.e(LOG_TAG, e.getMessage(), e);
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(LOG_TAG, "Error parsing the " + type + " results");
                }
            });
            KLPVolleySingleton.getInstance(MainActivity.this).addToRequestQueue(stringRequest);
        }

        private void processURL(String apiURL, final String type) {

            if (type.equals("school") || type.equals("boundary")) {
                processPaginatedURL(apiURL, type);
                return;
            }

            StringRequest stringRequest = new StringRequest(Request.Method.GET,
                    apiURL, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    publishProgress("Downloading " + type);

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
                    }

                    syncRequestCount--;
                    if (syncRequestCount == 0) progressDialog.dismiss();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    syncRequestCount--;
                    if (syncRequestCount == 0) progressDialog.dismiss();
                    Log.v(LOG_TAG, "Error parsing the survey results");
                }
            });

            KLPVolleySingleton.getInstance(MainActivity.this).addToRequestQueue(stringRequest);
            syncRequestCount++;
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

            return new String[0];
        }

        @Override
        protected void onProgressUpdate(String... values) {
            progressDialog.setMessage(values[0]);
        }

        @Override
        protected void onPostExecute(String[] result) {
            progressDialog.dismiss();
        }

        private String saveBoundaryDataFromJson(String boundaryJsonStr)
                throws JSONException {
            db = ((KLPApplication) getApplicationContext()).getDb();

            final String FEATURES = "features";
            JSONObject boundaryJson = new JSONObject(boundaryJsonStr);
            String next_url = boundaryJson.getString("next");
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
                } else {
                    parentId = 1;
                }

                boundaryId = boundaryObject.getInt("id");
                name = boundaryObject.getString("name");
                hierarchy = boundaryObject.getString("type");
                school_type = boundaryObject.getString("school_type");

                Boundary boundary = new Boundary()
                        .setId(boundaryId)
                        .setParentId(parentId)
                        .setName(name)
                        .setHierarchy(hierarchy)
                        .setType(school_type);
                db.insertWithId(boundary);
            }
            return next_url;
        }


        private String saveSchoolDataFromJson(String schoolJsonStr)
                throws JSONException {
            db = ((KLPApplication) getApplicationContext()).getDb();

            final String FEATURES = "features";
            JSONObject schoolJson = new JSONObject(schoolJsonStr);
            String next_url = schoolJson.getString("next");
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
            return next_url;
        }


        private void saveQuestionDataFromJson(String questionJsonStr)
                throws JSONException {
            db = ((KLPApplication) getApplicationContext()).getDb();

            final String FEATURES = "features";
            JSONObject questionJson = new JSONObject(questionJsonStr);
            JSONArray questionArray = questionJson.getJSONArray(FEATURES);
            db.deleteAll(Question.class);

            for (int i = 0; i < questionArray.length(); i++) {

                long questionId;
                String text;
                String text_kn;
                String display_text;
                String key;
                String options;
                String type;
                String school_type;

                JSONObject questionObject = questionArray.getJSONObject(i);
                JSONObject schoolObject = questionObject.getJSONObject("school_type");
                JSONArray questiongroupSetArray = questionObject.getJSONArray("questiongroup_set");

                questionId = questionObject.getInt("id");
                text = questionObject.getString("text");
                text_kn = questionObject.getString("text_kn");
                display_text = questionObject.getString("display_text");
                key = questionObject.getString("key");
                options = questionObject.getString("options");
                type = questionObject.getString("question_type");
                school_type = schoolObject.getString("name");

                Question question = new Question()
                        .setId(questionId)
                        .setText(text)
                        .setTextKn(text_kn)
                        .setDisplayText(display_text)
                        .setKey(key)
                        .setOptions(options)
                        .setType(type)
                        .setSchoolType(school_type);
                
                db.insertWithId(question);

                for (int j = 0; j < questiongroupSetArray.length(); j++) {
                    JSONObject questiongroupObject = questiongroupSetArray.getJSONObject(j);

                    Integer throughId = questiongroupObject.getInt("through_id");
                    long questiongroupId = questiongroupObject.getInt("questiongroup");
                    Integer sequence = questiongroupObject.getInt("sequence");
                    Integer status = questiongroupObject.getInt("status");
                    String source = questiongroupObject.getString("source");

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
            db = ((KLPApplication) getApplicationContext()).getDb();

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
            db = ((KLPApplication) getApplicationContext()).getDb();

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

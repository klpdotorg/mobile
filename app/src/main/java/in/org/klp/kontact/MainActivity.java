package in.org.klp.kontact;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Query;
import com.yahoo.squidb.sql.Update;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
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

public class MainActivity extends AppCompatActivity {
    private SessionManager mSession;
    private Snackbar snackbarSync;
    private SyncDownloadTask downloadTask;
    private ProgressDialog progressDialog = null;
    private KontactDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = ((KLPApplication) getApplicationContext()).getDb();

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
        // if the app just updated or this is the first run, disable survey button
        // survey button is enabled again in postExecute() of Download Sync
        if (isSyncNeeded()) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("You should \"Sync\" data to get updates from KLP.")
                    .setTitle("Sync Needed");
            builder.setPositiveButton("Ok, Sync Now", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User clicked OK button
                    startSync();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }

        Button sync_button = (Button) findViewById(R.id.sync_button);
        sync_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSync();
            }
        });
    }

    public boolean isSyncNeeded() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int currentVersion = 0;
        try {
            currentVersion = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_CONFIGURATIONS).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(this.toString(), "if you're here, you're in trouble");
            return true;
        }

        final int lastVersion = prefs.getInt("lastVersion", -1);
        if (currentVersion > lastVersion) {
            // first time running the app or app just updated
            prefs.edit().putInt("lastVersion", currentVersion).commit();
            return true;
        } else {
            return false;
        }
    }

    public void startSync() {
        // disable all buttons
        // call syncUpload()
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setTitle("Uploading");
        progressDialog.setMessage("Uploading responses to server");
        progressDialog.setIndeterminate(true);
        progressDialog.show();

        Button survey_button = (Button) findViewById(R.id.survey_button);
        survey_button.setEnabled(false);
        survey_button.setAlpha(.5f);

        syncUpload();
    }

    public void endSync() {
        // enable all buttons
        // dismiss sync progress dialog
        Button survey_button = (Button) findViewById(R.id.survey_button);
        survey_button.setEnabled(true);
        survey_button.setAlpha(1f);

        progressDialog.dismiss();
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
        this.finish();
    }

    public void syncUpload() {
        HashMap<String, String> user = mSession.getUserDetails();

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

    public class SyncDownloadTask extends AsyncTask<Void, String, Void> {

        private final String LOG_TAG = SyncDownloadTask.class.getSimpleName();
        private Integer syncRequestCount;

        @Override
        protected void onPreExecute() {
            syncRequestCount = 0;
        }

        private void processURL(String apiURL, final String type) {
            StringRequest stringRequest = new StringRequest(Request.Method.GET,
                    apiURL, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    publishProgress("Downloading " + type);
                    String next_url = null;

                    try {
                        if (type.equals("survey")) {
                            saveSurveyDataFromJson(response);
                        } else if (type.equals("questiongroup")) {
                            saveQuestiongroupDataFromJson(response);
                        } else if (type.equals("question")) {
                            saveQuestionDataFromJson(response);
                        } else if (type.equals("school")) {
                            next_url = saveSchoolDataFromJson(response);
                            if (next_url != "null") processURL(next_url, type);
                        } else if (type.equals("boundary")) {
                            next_url = saveBoundaryDataFromJson(response);
                            if (next_url != "null") processURL(next_url, type);
                        } else if (type.equals("story")) {
                            next_url = saveStoryDataFromJson(response);
                            if (next_url != "null") processURL(next_url, type);
                        }
                    } catch (JSONException e) {
                        Log.e(LOG_TAG, e.getMessage(), e);
                    }

                    syncRequestCount--;
                    if (syncRequestCount == 0) endSync();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    syncRequestCount--;
                    if (syncRequestCount == 0) endSync();
                    Log.v(LOG_TAG, "Error parsing the " + type + " results: " + error.getMessage());
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

            KLPVolleySingleton.getInstance(MainActivity.this).addToRequestQueue(stringRequest);
            syncRequestCount++;
        }

        @Override
        protected Void doInBackground(Void... params) {
            Log.d(this.toString(), BuildConfig.HOST);

            // Populate surveys
            processURL(BuildConfig.HOST + "/api/v1/surveys/?source=mobile", "survey");

            // Populate questiongroups
            processURL(BuildConfig.HOST + "/api/v1/questiongroups/", "questiongroup");

            // Populate questions
            processURL(BuildConfig.HOST + "/api/v1/questions/", "question");

            // Populate stories
            // `admin2=detect` is a special flag that lets the server decide which blocks the user
            // has been most active in. If server can't find any blocks, it wont bother.
            // For detect to work, user authentication headers must be sent with it.
            processURL(BuildConfig.HOST + "/api/v1/stories/?source=csv&answers=yes&admin2=detect&per_page=200", "story");

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            progressDialog.setMessage(values[0]);
        }

        @Override
        protected void onPostExecute(Void result) {
            // TODO: Do something
        }

        private String saveBoundaryDataFromJson(String boundaryJsonStr)
                throws JSONException {
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

        private String saveStoryDataFromJson(String storyJsonStr)
                throws JSONException {

            final String FEATURES = "features";
            JSONObject storyJson = new JSONObject(storyJsonStr);
            String next_url = storyJson.getString("next");
            JSONArray storyArray = storyJson.getJSONArray(FEATURES);
            Log.d(LOG_TAG, String.valueOf(storyArray.length()));

            for (int i = 0; i < storyArray.length(); i++) {
                Log.d(LOG_TAG, storyArray.getJSONObject(i).toString());

                JSONObject storyObject = storyArray.getJSONObject(i);

                Long schoolId = storyObject.getLong("school");
                Long userId = storyObject.getLong("user");
                Long groupId = storyObject.getLong("group");
                String createdAt = storyObject.getString("created_at");
                String userType = storyObject.getJSONObject("user_type").getString("name");
                String sysId = storyObject.getString("id");
                Timestamp createdAtTimestamp;

                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS");
                    Date parsedDate = dateFormat.parse(createdAt);
                    createdAtTimestamp = new Timestamp(parsedDate.getTime());
                    Log.d(LOG_TAG, createdAtTimestamp.toString());
                } catch (Exception e) {
                    Log.e(LOG_TAG, e.toString());
                    continue;
                }

                SquidCursor<Story> storyCursor = db.query(Story.class,
                        Query.select().where(Story.SYSID.eq(sysId)));
                if (storyCursor.getCount() == 0) {
                    Story story = new Story()
                            .setUserId(userId)
                            .setSchoolId(schoolId)
                            .setGroupId(groupId)
                            .setRespondentType(userType)
                            .setSynced(1)
                            .setSysid(sysId);

                    if (createdAtTimestamp != null) {
                        story.setCreatedAt(createdAtTimestamp.getTime());
                    }

                    db.persist(story);

                    JSONArray storyAnswers = storyObject.getJSONArray("answers");
                    for (int j = 0; j < storyAnswers.length(); j++) {
                        JSONObject answerObject = storyAnswers.getJSONObject(j);
                        JSONObject question = answerObject.getJSONObject("question");

                        Long questionId = question.getLong("id");
                        String answerText = answerObject.getString("text");

                        Answer answer = new Answer()
                                .setStoryId(story.getId())
                                .setQuestionId(questionId)
                                .setText(answerText)
                                .setCreatedAt(createdAtTimestamp.getTime());
                        db.persist(answer);
                    }
                } else {
                    // TODO: 1/11/16 To replace previous story entry?
                }
            }
            return next_url;
        }


        private void saveQuestionDataFromJson(String questionJsonStr)
                throws JSONException {
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

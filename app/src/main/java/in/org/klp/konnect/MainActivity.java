package in.org.klp.konnect;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Query;
import com.yahoo.squidb.sql.Update;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Stack;

import in.org.klp.konnect.db.Answer;
import in.org.klp.konnect.db.Boundary;
import in.org.klp.konnect.db.KontactDatabase;
import in.org.klp.konnect.db.Question;
import in.org.klp.konnect.db.QuestionGroup;
import in.org.klp.konnect.db.QuestionGroupQuestion;
import in.org.klp.konnect.db.School;
import in.org.klp.konnect.db.Story;
import in.org.klp.konnect.db.Survey;
import in.org.klp.konnect.utils.SessionManager;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func3;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    private SessionManager mSession;
    private DownloadTasks dt;
    private ProgressDialog progressDialog = null;
    private KontactDatabase db;
    private OkHttpClient okclient = new OkHttpClient();
    private Stack<String> thingsToDo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = ((KLPApplication) getApplicationContext()).getDb();

        mSession = new SessionManager(getApplicationContext());
        mSession.checkLogin();
        // Log user details to be used for crashlytics
        logUserToCrashlytics();

        final Button sync_button = (Button) findViewById(R.id.sync_button);
        sync_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sync();
            }
        });

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
                    sync_button.callOnClick();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private void logUserToCrashlytics() {
        HashMap<String, String> user = mSession.getUserDetails();

        Crashlytics.setUserIdentifier(user.get(SessionManager.KEY_ID));
        Crashlytics.setUserName(user.get(SessionManager.KEY_NAME));
    }


    public void logError(String tag, Throwable e) {
        Log.e(tag, e.getMessage());
        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
        e.printStackTrace();
    }

    public void log(String tag, String msg) {
//        Log.d(tag, msg);
        Toast.makeText(MainActivity.this, tag + ": " + msg, Toast.LENGTH_LONG).show();
    }

    public boolean isSyncNeeded() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int currentVersion = 0;
        try {
            currentVersion = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_CONFIGURATIONS).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
//            Log.d(this.toString(), "if you're here, you're in trouble");
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

    public void sync() {
        dt = new DownloadTasks();

        HashMap<String, String> API_URLS = new HashMap<String, String>();
        API_URLS.put("survey", "/api/v1/surveys/?source=mobile");
        API_URLS.put("questiongroup", "/api/v1/questiongroups/");
        API_URLS.put("question", "/api/v1/questions/");

        String story_url = "/api/v1/stories/?source=csv&source=mobile&answers=yes&admin2=detect&per_page=100";
        Story last_story = db.fetchByQuery(Story.class,
                Query.select().where(Story.SYSID.neq(null)).orderBy(Story.SYSID.desc()).limit(1));
        if (last_story != null) {
            story_url += "&since_id=" + last_story.getSysid();
        }

        API_URLS.put("story", story_url);

        thingsToDo = new Stack<String>();
        thingsToDo.push("upload");
        thingsToDo.push("survey");
        thingsToDo.push("question");
        thingsToDo.push("questiongroup");
        thingsToDo.push("story");

        preSync("Uploading", "Uploading responses to server");
        getUploadObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<JSONObject>() {
                    @Override
                    public void onCompleted() {
                        log("Upload Observer", "Upload completed");
                        testProgress("upload");
                        preSync("Downloading", "Downloading Surveys, Questions and Stories");
                    }

                    @Override
                    public void onError(Throwable e) {
                        logError("Upload Observer", e);
                    }

                    @Override
                    public void onNext(JSONObject response) {
                        try {
//                            Log.d(this.toString(), response.toString());
                            // TODO: show error
                            String error = response.optString("error");

                            if (error != null && !error.isEmpty() && error != "null") {
                                Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                            } else {
                                JSONArray success = response.optJSONArray("success");
                                if (success != null && success.length() > 0) {
//                                    Log.d("Upload onNext", success.toString());
                                    for (int i = 0; i < success.length(); i++) {
                                        Update storyUpdate = Update.table(Story.TABLE)
                                                .set(Story.SYNCED, 1)
                                                .where(Story.ID.eq(success.get(i)));
                                        db.update(storyUpdate);
                                    }
                                }

                                JSONArray failed = response.optJSONArray("failed");
                                if (failed.length() > 0) {
                                    log("Upload onNext", "Upload failed for Story ids: " + failed.toString());
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

        Observable.zip(
                getDownloadObservable(BuildConfig.HOST + API_URLS.get("survey")),
                getDownloadObservable(BuildConfig.HOST + API_URLS.get("questiongroup")),
                getDownloadObservable(BuildConfig.HOST + API_URLS.get("question")),
                new Func3<JSONObject, JSONObject, JSONObject, HashMap<String, JSONObject>>() {
                    @Override
                    public HashMap<String, JSONObject> call(JSONObject surveyJSON, JSONObject qgJSON, JSONObject qJSON) {
                        HashMap<String, JSONObject> responseMap = new HashMap();
                        responseMap.put("survey", surveyJSON);
                        responseMap.put("questiongroup", qgJSON);
                        responseMap.put("question", qJSON);
                        return responseMap;
                    }
                }
        )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<HashMap<String, JSONObject>>() {
                    @Override
                    public void onCompleted() {
                        log("multiple observer", "Called");
                    }

                    @Override
                    public void onError(Throwable e) {
                        logError("MultiDownload Observer", e);
                    }

                    @Override
                    public void onNext(HashMap<String, JSONObject> resultMap) {
                        for (String key: resultMap.keySet()) {
                            try {
                                if (key == "survey") {
                                    dt.saveSurveyDataFromJson(resultMap.get(key));
                                    testProgress("survey");
                                }
                                else if (key == "questiongroup") {
                                    dt.saveQuestiongroupDataFromJson(resultMap.get(key));
                                    testProgress("questiongroup");
                                }
                                else if (key == "question") {
                                    dt.saveQuestionDataFromJson(resultMap.get(key));
                                    testProgress("question");
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });

        getDownloadObservable(BuildConfig.HOST + API_URLS.get("story"))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .concatMap(new Func1<JSONObject, Observable<JSONObject>>() {
                    @Override
                    public Observable<JSONObject> call(JSONObject okresponse) {
                        return Observable.just(okresponse);
                    }
                })
                .subscribe(new Subscriber<JSONObject>() {
                    @Override
                    public void onCompleted() {
                        testProgress("story");
                    }

                    @Override
                    public void onError(Throwable e) {
                        logError("StoryDowload Observer", e);
                    }

                    @Override
                    public void onNext(JSONObject okresponse) {
                        try {
                            dt.saveStoryDataFromJson(okresponse);
                        } catch (JSONException e) {
                            logError("storyDL Subscriber", e);
                        }
                    }
                });
    }

    public void preSync(String title, String message) {
        // disable all buttons
        // show progress dialog
        // dismiss progress dialog if it's already showing
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setIndeterminate(true);
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.setTitle(title);
        progressDialog.setMessage(message);

        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }

        Button survey_button = (Button) findViewById(R.id.survey_button);
        survey_button.setEnabled(false);
        survey_button.setAlpha(.5f);
    }

    public void testProgress(String thing) {
        if (thingsToDo.contains(thing)) {
            thingsToDo.remove(thing);
        }

        if (thingsToDo.toArray().length == 0 && progressDialog.isShowing()) {
            endSync();
        }
    }

    public void endSync() {
        // enable all buttons
        // dismiss sync progress dialog
        if (progressDialog != null) {
            Button survey_button = (Button) findViewById(R.id.survey_button);
            survey_button.setEnabled(true);
            survey_button.setAlpha(1f);

            progressDialog.dismiss();
        }
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

    public Observable<JSONObject> getUploadObservable() {
        return Observable.create(new Observable.OnSubscribe<JSONObject>() {
            @Override
            public void call(Subscriber<? super JSONObject> subscriber) {
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
                MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                HashMap<String, String> user = mSession.getUserDetails();
                RequestBody body = RequestBody.create(JSON, requestJson.toString());
                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url(SYNC_URL)
                        .post(body)
                        .addHeader("Authorization", "Token " + user.get("token"))
                        .build();
                try {
                    okhttp3.Response okresponse = okclient.newCall(request).execute();
                    JSONObject jsonResp = new JSONObject(okresponse.body().string());
                    subscriber.onNext(jsonResp);
                    subscriber.onCompleted();
                } catch (IOException e) {
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                } catch (JSONException e) {
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public Observable<JSONObject> getDownloadObservable(final String url) {
//        Log.d("observableDL", url);
        return Observable.create(new Observable.OnSubscribe<JSONObject>() {
            @Override
            public void call(Subscriber<? super JSONObject> subscriber) {
                if (!url.isEmpty()) {
                    HashMap<String, String> user = mSession.getUserDetails();
                    okhttp3.Request request = new okhttp3.Request.Builder()
                            .url(url)
                            .addHeader("Authorization", "Token " + user.get("token"))
                            .build();
                    try {
                        okhttp3.Response okresponse = okclient.newCall(request).execute();
                        String okresponse_body = okresponse.body().string();
//                        Log.d("Output", okresponse_body);
                        JSONObject okresponse_json = new JSONObject(okresponse_body);
                        subscriber.onNext(okresponse_json);
                        subscriber.onCompleted();
                    } catch (IOException e) {
                        logError("DlObErr IO", e);
                    } catch (JSONException e) {
                        logError("DlObErr JSON", e);
                    }
                }
            }
        }).concatMap(new Func1<JSONObject, Observable<? extends JSONObject>>() {
            @Override
            public Observable<? extends JSONObject> call(JSONObject okresponse) {
                String next = okresponse.optString("next");
                if (next == null || next == "null") {
                    return Observable.just(okresponse);
                }
                return Observable.just(okresponse)
                        .concatWith(getDownloadObservable(next));
            }
        });
    }


    public class DownloadTasks {
        private final String LOG_TAG = "DownloadTask";

        private String saveBoundaryDataFromJson(JSONObject boundaryJson)
                throws JSONException {
            final String FEATURES = "features";
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


        private String saveSchoolDataFromJson(JSONObject schoolJson)
                throws JSONException {

            final String FEATURES = "features";
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

        private void saveStoryDataFromJson(JSONObject storyJson)
                throws JSONException {
//            Log.d(LOG_TAG, "Saving Story Data: " + storyJson.toString());
            final String FEATURES = "features";

            JSONArray storyArray = storyJson.getJSONArray(FEATURES);
//            Log.d(LOG_TAG, String.valueOf(storyArray.length()));

            for (int i = 0; i < storyArray.length(); i++) {
                JSONObject storyObject = storyArray.getJSONObject(i);

                Long schoolId = storyObject.getLong("school");
                Long userId = storyObject.getLong("user");
                Long groupId = storyObject.getLong("group");
                String createdAt = storyObject.getString("created_at");
                String userType = storyObject.getJSONObject("user_type").getString("name");
                // Storing the story ID from server as SYSID on the device
                // This helps in keeping the stories unique on the device
                String sysId = storyObject.getString("id");
                Timestamp createdAtTimestamp;

                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS");
                    Date parsedDate = dateFormat.parse(createdAt);
                    createdAtTimestamp = new Timestamp(parsedDate.getTime());
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
                } else if (storyCursor.getCount() > 1) {
                    // delete old stories with same SYSID
                    
                } else {
                    // There is just one story with same SYSID.
                    // So it's the story that was synced before.
                    // Now that we have a copy from the server,
                    // update this copy on the device, as it might
                    // have been updated on the server.
                }
            }
        }


        private void saveQuestionDataFromJson(JSONObject questionJson)
                throws JSONException {
//            Log.d(LOG_TAG, "Saving Question Data: " + questionJson.toString());
            final String FEATURES = "features";
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


        private void saveQuestiongroupDataFromJson(JSONObject questiongroupJson)
                throws JSONException {
//            Log.d(LOG_TAG, "Saving QG Data: " + questiongroupJson.toString());
            final String FEATURES = "features";
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

                Integer qgStatus = questiongroupObject.getInt("status");
                if (!qgStatus.equals(1)) continue;

                // Get the JSON object representing the partner
                JSONObject surveyObject = questiongroupObject.getJSONObject("survey");

                groupId = questiongroupObject.getInt("id");
                status = questiongroupObject.getInt("status");
                start_date = questiongroupObject.optInt("start_date");
                end_date = questiongroupObject.optInt("end_date");
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

        private void saveSurveyDataFromJson(JSONObject surveyJson)
                throws JSONException {
//            Log.d(LOG_TAG, "Saving Survey Data: " + surveyJson.toString());
            final String FEATURES = "features";
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

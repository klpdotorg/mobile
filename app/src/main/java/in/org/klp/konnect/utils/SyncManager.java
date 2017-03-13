package in.org.klp.konnect.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Query;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import in.org.klp.konnect.BuildConfig;
import in.org.klp.konnect.KLPApplication;
import in.org.klp.konnect.MainActivity;
import in.org.klp.konnect.R;
import in.org.klp.konnect.ReportsActivity;
import in.org.klp.konnect.db.Answer;
import in.org.klp.konnect.db.Boundary;
import in.org.klp.konnect.db.KontactDatabase;
import in.org.klp.konnect.db.Question;
import in.org.klp.konnect.db.QuestionGroup;
import in.org.klp.konnect.db.QuestionGroupQuestion;
import in.org.klp.konnect.db.School;
import in.org.klp.konnect.db.Story;
import in.org.klp.konnect.db.Survey;
import needle.Needle;
import needle.UiRelatedTask;
import okhttp3.OkHttpClient;

/**
 * Created by bibhas on 23/2/17.
 */

public class SyncManager {
    public boolean doUpload = false, doUpdateSurvey = false, doDownloadStories = false;
    private KontactDatabase db;
    private Context context;
    private SessionManager mSession;
    private OkHttpClient okclient;
    private MenuItem syncButton;

    public String story_url = "/api/v1/stories/?source=csv&source=mobile&answers=yes&per_page=0&is_sync=yes";
    public String host_url = BuildConfig.HOST;

    public SyncManager(Context activity, KontactDatabase db, Boolean doUpload, Boolean doUpdateSurvey, Boolean doDownloadStories) {
        this.context = activity;
        this.db = db;
        this.doUpload = doUpload;
        this.doUpdateSurvey = doUpdateSurvey;
        this.doDownloadStories = doDownloadStories;

        mSession = new SessionManager(context.getApplicationContext());

        okclient = new OkHttpClient.Builder()
                .connectTimeout(0, TimeUnit.SECONDS)
                .writeTimeout(0, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .build();
    }

    public void uploadStories() {

    }

    public void downloadStories() {
        downloadStories(null);
    }

    public void downloadStories(Long clusterid) {
        Toast.makeText(this.context, "Downloading stories. Please wait.", Toast.LENGTH_SHORT).show();

        Boundary cluster = db.fetch(Boundary.class, clusterid);
        syncButton = ((ReportsActivity) context)._menu.findItem(R.id.action_sync_block);
        syncButton.setTitle("Syncing..");

        if (clusterid != null) {
            this.story_url += "&admin2=" + String.valueOf(cluster.getParentId());
        } else {
            this.story_url += "&admin2=detect";
        }

        Story last_story = db.fetchByQuery(Story.class,
                Query.select().where(Story.SYSID.neq(null)).orderBy(Story.SYSID.desc()).limit(1));
        if (last_story != null) {
            story_url += "&since_id=" + last_story.getSysid();
        }

        Needle.onBackgroundThread().execute(new UiRelatedTask<Integer>() {
            @Override
            protected Integer doWork() {
                Log.d("DL", story_url);
                JSONObject resp = download(story_url);
                Integer count = 0;
                DownloadTasks dt = new DownloadTasks();
                try {
                    count = dt.saveStoryDataFromJson(resp);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return count;
            }

            @Override
            protected void thenDoUiRelatedWork(Integer count) {
                Toast.makeText(SyncManager.this.context, String.valueOf(count) + " stories have been downloaded.", Toast.LENGTH_SHORT).show();
                syncButton.setTitle("Sync");
                Intent intent = ((ReportsActivity) context).getIntent();
                ((ReportsActivity) context).finish();
                context.startActivity(intent);
            }
        });
    }

    public JSONObject download(String url) {
        JSONObject resp = new JSONObject();

        url = host_url + url;
        HashMap<String, String> user = mSession.getUserDetails();
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .addHeader("Authorization", "Token " + user.get("token"))
                .build();
        try {
            okhttp3.Response okresponse = okclient.newCall(request).execute();

            if (!okresponse.isSuccessful()) {
                Log.d("Download Error", "There is something wrong with the Internet connection.");
                return new JSONObject();
            }

            if (okresponse.code() == 401) {
                Log.d("Authentication Error", "Something went wrong. Please login again.");
            }

            String okresponse_body = okresponse.body().string();
            resp = new JSONObject(okresponse_body);
        } catch (IOException e) {
            Log.e("DlObErr IO", e.getMessage());
        } catch (JSONException e) {
            Log.e("DlObErr JSON", e.getMessage());
        }

        return resp;
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

        private Integer saveStoryDataFromJson(JSONObject storyJson)
                throws JSONException {
//            Log.d(LOG_TAG, "Saving Story Data: " + storyJson.toString());
            final String FEATURES = "features";

            JSONArray storyArray = storyJson.getJSONArray(FEATURES);
            Log.d(LOG_TAG, "Total stories received: " + String.valueOf(storyArray.length()));

            db.beginTransaction();
            try {
                for (int i = 0; i < storyArray.length(); i++) {
                    JSONObject storyObject = storyArray.getJSONObject(i);

                    Long schoolId = storyObject.getLong("school");
                    Long userId = storyObject.getLong("user");
                    Long groupId = storyObject.getLong("group");
                    String dateOfVisit = storyObject.getString("date_of_visit");
                    String userType = storyObject.getString("user_type");
                    // Storing the story ID from server as SYSID on the device
                    // This helps in keeping the stories unique on the device
                    String sysId = storyObject.getString("id");
                    Timestamp dateOfVisitTS;

                    try {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
                        Date parsedDate = dateFormat.parse(dateOfVisit);
                        dateOfVisitTS = new Timestamp(parsedDate.getTime());
                    } catch (Exception e) {
                        Log.e(LOG_TAG, e.toString());
                        continue;
                    }

                    SquidCursor<Story> storyCursor = db.query(
                            Story.class,
                            Query.select().where(
                                    Story.SCHOOL_ID.eq(schoolId).and(
                                            Story.USER_ID.eq(userId).and(
                                                    Story.SYSID.eq(sysId)
                                            )
                                    )
                            )
                    );

                    try {
                        if (storyCursor.getCount() == 0) {
                            Story story = new Story()
                                    .setUserId(userId)
                                    .setSchoolId(schoolId)
                                    .setGroupId(groupId)
                                    .setRespondentType(userType)
                                    .setSynced(1)
                                    .setSysid(sysId);

                            if (dateOfVisitTS != null) {
                                story.setCreatedAt(dateOfVisitTS.getTime());
                            }
                            db.persist(story);
//                            Log.d("DL", "Story created: " + story.getId());

                            JSONObject storyAnswers = storyObject.getJSONObject("answers");
                            Iterator<String> answerKeys = storyAnswers.keys();

                            while (answerKeys.hasNext()) {
                                String key = answerKeys.next();
                                Long questionId = Long.valueOf(key);
                                String answerText = storyAnswers.getString(key);

                                Answer answer = new Answer()
                                        .setStoryId(story.getId())
                                        .setQuestionId(questionId)
                                        .setText(answerText)
                                        .setCreatedAt(dateOfVisitTS.getTime());
                                db.persist(answer);
//                                Log.d("DL", "Answer Created: " + answer.getId());
                            }
                        } else if (storyCursor.getCount() > 1) {
                            // there are multiple old stories with same SYSID
                            // this should not happen
                        } else {
                            // ignore existing story with same SYSID
                        }
                    } finally {
                        storyCursor.close();
                    }
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            return storyArray.length();
        }


        private void saveQuestionDataFromJson(JSONObject questionJson)
                throws JSONException {
//            Log.d(LOG_TAG, "Saving Question Data: " + questionJson.toString());
            final String FEATURES = "features";
            JSONArray questionArray = questionJson.getJSONArray(FEATURES);

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

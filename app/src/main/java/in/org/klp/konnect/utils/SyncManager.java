package in.org.klp.konnect.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import in.org.klp.konnect.BuildConfig;
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
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;

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
        Toast.makeText(this.context, "Uploading stories. Please wait.", Toast.LENGTH_SHORT).show();
        Needle.onBackgroundThread().execute(new UiRelatedTask<Integer>() {
            @Override
            protected Integer doWork() {
                UploadTask ut = new UploadTask();
                JSONObject uploadJson = doUpload();
                Integer successCount = ut.processUploadResponse(uploadJson);
                return successCount;
            }

            @Override
            protected void thenDoUiRelatedWork(Integer count) {
                Toast.makeText(SyncManager.this.context, "Uploaded " + count + " stories..", Toast.LENGTH_SHORT).show();
            }
        });
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

        List<Long> clusterIds = new ArrayList<Long>();
        SquidCursor<Boundary> clustersInBlockCursor = db.query(Boundary.class, Query.select().from(Boundary.TABLE).where(Boundary.PARENT_ID.eq(cluster.getParentId())));
        try {
            while (clustersInBlockCursor.moveToNext()) {
                Boundary c = new Boundary(clustersInBlockCursor);
                clusterIds.add(c.getId());
            }
        } finally {
            clustersInBlockCursor.close();
        }

        List<Long> schIds = new ArrayList<>();
        SquidCursor<School> schInBlockCursor = db.query(School.class, Query.select().from(School.TABLE).where(School.BOUNDARY_ID.in(clusterIds)));
        try {
            while (schInBlockCursor.moveToNext()) {
                School sch = new School(schInBlockCursor);
                schIds.add(sch.getId());
            }
        } finally {
            schInBlockCursor.close();
        }

        Story last_story = db.fetchByQuery(Story.class,
                Query.select().where(Story.SYSID.neq(null).and(Story.SCHOOL_ID.in(schIds))).orderBy(Story.SYSID.desc()).limit(1));
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
                Toast.makeText(SyncManager.this.context, "Downloaded " + String.valueOf(count) + " stories..", Toast.LENGTH_SHORT).show();
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

    public JSONObject doUpload() {
        Query listStoryQuery = Query.select().from(Story.TABLE)
                .where(Story.SYNCED.eq(0));
        SquidCursor<Story> storiesCursor = db.query(Story.class, listStoryQuery);
        SquidCursor<Answer> answerCursor = null;

        JSONObject requestJson = new JSONObject();
        JSONObject respJson = new JSONObject();
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

            if (!okresponse.isSuccessful()) {
                Log.e("Upload Error", "There is something wrong with the Internet connection.");
                return new JSONObject();
            }

            if (okresponse.code() == 401) {
                Log.e("Authentication Error", "Something went wrong. Please login again.");
            }

            respJson = new JSONObject(okresponse.body().string());
        } catch (IOException e) {
            e.printStackTrace();
            if (e.getMessage() != null) Log.d(this.toString(), e.getMessage());
        } catch (JSONException e) {
            e.printStackTrace();
            if (e.getMessage() != null) Log.d(this.toString(), e.getMessage());
        }
        return respJson;
    }

    public class UploadTask {
        private Integer processUploadResponse(JSONObject response) {
            Integer successCount = 0;
            try {
                Log.d(this.toString(), response.toString());
                // TODO: show error
                String error = response.optString("error");

                if (error != null && !error.isEmpty() && error != "null") {
                    Toast.makeText(SyncManager.this.context, error, Toast.LENGTH_LONG).show();
                } else {
                    JSONObject success = response.getJSONObject("success");
                    Iterator<String> keys = success.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        String sysid = success.getString(key);
                        Update storyUpdate = Update.table(Story.TABLE)
                                .set(Story.SYNCED, 1)
                                .set(Story.SYSID, sysid)
                                .where(Story.ID.eq(Long.valueOf(key)));
                        db.update(storyUpdate);
                        successCount++;
                    }

                    JSONArray failed = response.optJSONArray("failed");
                    if (failed != null && failed.length() > 0) {
                        Log.d("Upload onNext", "Upload failed for Story ids: " + failed.toString());
                    }

                    String command = response.optString("command", "");
                    Log.d("Command Log", command);
                    switch (command) {
                        case "wipe-stories":
                            db.deleteAll(Answer.class);
                            db.deleteAll(Story.class);
                            break;
                        case "wipe-all":
                            db.deleteAll(Answer.class);
                            db.deleteAll(Story.class);
                            db.deleteAll(QuestionGroupQuestion.class);
                            db.deleteAll(QuestionGroup.class);
                            db.deleteAll(Question.class);
                            db.deleteAll(Survey.class);
                            db.deleteAll(School.class);
                            db.deleteAll(Boundary.class);
                            break;
                        default:
                            Log.d("Command Log", "Nothing to do.");
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return successCount;
        }
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

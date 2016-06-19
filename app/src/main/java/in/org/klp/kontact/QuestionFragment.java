package in.org.klp.kontact;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import in.org.klp.kontact.db.Answer;
import in.org.klp.kontact.db.KontactDatabase;
import in.org.klp.kontact.db.Question;
import in.org.klp.kontact.db.QuestionGroup;
import in.org.klp.kontact.db.QuestionGroupQuestion;
import in.org.klp.kontact.db.School;
import in.org.klp.kontact.db.Story;
import in.org.klp.kontact.utils.SessionManager;

public class QuestionFragment extends Fragment {

    private ArrayAdapter<String> mQuestionsAdapter;
    private Long surveyId;
    private String surveyName;
    private Long schoolId;
    private Long questionGroupId;
    private LinearLayout linearLayoutQuestions;
    private KontactDatabase db;
    SessionManager session;

    public QuestionFragment() {
    }

    private void fetchQuestions() {
        FetchQuestionsTask questionsTask = new FetchQuestionsTask();
        questionsTask.execute();
    }

    @Override
    public void onStart() {
        super.onStart();
        fetchQuestions();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mQuestionsAdapter = new ArrayAdapter<String>(
                getActivity(),
                R.layout.list_item_question,
                R.id.list_item_question_textview,
                new ArrayList<String>()
        );

        db = new KontactDatabase(getActivity());

        // check if user is logged in
        session = new SessionManager(getActivity());
        session.checkLogin();

        Intent intent = getActivity().getIntent();

        surveyId = intent.getLongExtra("surveyId", 0);
        surveyName = intent.getStringExtra("surveyName");
        schoolId = intent.getLongExtra("schoolId", 0);

        View rootView = inflater.inflate(R.layout.fragment_question, container, false);

        School school = db.fetch(School.class, schoolId);
        TextView textViewSchool = (TextView) rootView.findViewById(R.id.textViewSchool);
        textViewSchool.setText(school.getName());

        if (surveyId == 0) {
            Intent intentMain = new Intent(getActivity(), MainActivity.class);
            startActivity(intentMain);
        }


        linearLayoutQuestions = (LinearLayout) rootView.findViewById(R.id.linearLayoutQuestions);

        return rootView;
    }

    public class FetchQuestionsTask extends AsyncTask<Void, Void, ArrayList<Question>> {

        private final String LOG_TAG = FetchQuestionsTask.class.getSimpleName();

        @Override
        protected ArrayList<Question> doInBackground(Void... params) {
            Intent intent = getActivity().getIntent();
            surveyId = intent.getLongExtra("surveyId", 0);

            SquidCursor<QuestionGroup> qgCursor = null;
            SquidCursor<QuestionGroupQuestion> qgqCursor = null;

            Query listQGquery = Query.select().from(QuestionGroup.TABLE)
                    .where(QuestionGroup.SURVEY_ID.eq(surveyId)).limit(1);
            qgCursor = db.query(QuestionGroup.class, listQGquery);

            try {
                while (qgCursor.moveToFirst()) {
                    questionGroupId = qgCursor.get(QuestionGroup.ID);
                    Query listQGQquery = Query.select().from(QuestionGroupQuestion.TABLE)
                            .where(QuestionGroupQuestion.QUESTIONGROUP_ID.eq(questionGroupId));
                    qgqCursor = db.query(QuestionGroupQuestion.class, listQGQquery);
                    ArrayList<Question> resultQuestions = new ArrayList<Question>();

                    while (qgqCursor.moveToNext()) {
                        Long qID = qgqCursor.get(QuestionGroupQuestion.QUESTION_ID);
                        Question question = db.fetch(Question.class, qID);
                        resultQuestions.add(question);
                    }

                    return resultQuestions;
                }
            } finally {
                if (qgCursor != null) {
                    qgCursor.close();
                }
                if (qgqCursor != null) {
                    qgqCursor.close();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<Question> result) {
            if (result != null) {
                // TODO: DONT REMOVE THE TEXT VIEW
                linearLayoutQuestions.removeAllViews();

                for (Question question : result) {
                    // TODO: get question type and generate widgets according to it.
                    Switch mSwitch = new Switch(getActivity());
                    mSwitch.setText(question.getText());
                    mSwitch.setTag(question.getId());

                    mSwitch.setTextOn("YES");
                    mSwitch.setTextOff("NO");
                    mSwitch.setChecked(false);
                    mSwitch.setPadding(0, 0, 0, 40);
                    mSwitch.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                    mSwitch.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);

                    linearLayoutQuestions.addView(mSwitch);
                }

                Button mSubmitButton = new Button(getActivity());
                mSubmitButton.setText("Submit");
                mSubmitButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        // Get answers from the current form
                        Map<Long, String> answers = new HashMap<Long, String>();

                        for (int i = 0; i < linearLayoutQuestions.getChildCount(); i++) {
                            View child = linearLayoutQuestions.getChildAt(i);
                            if (child.getClass() == Switch.class) {
                                Switch cSwitch = (Switch) child;
                                Long questionId = Long.parseLong(child.getTag().toString());
                                String answer = String.valueOf(cSwitch.isChecked());
                                answers.put(questionId, answer);
                            }
                        }

                        // Now answerArray is filled with question ids and their answers
                        // TODO: Save answers in DB
                        HashMap<String, String> user = session.getUserDetails();
                        Long currentTS = System.currentTimeMillis();

                        Story story = new Story()
                                .setSchoolId(schoolId)
                                .setUserId(Long.parseLong(user.get(session.KEY_ID)))
                                .setGroupId(questionGroupId)
                                .setCreatedAt(currentTS);
                        db.persist(story);

                        for (Map.Entry<Long, String> entry : answers.entrySet()) {
                            Log.d("entry", entry.getValue());
                            Answer answer = new Answer()
                                    .setStoryId(story.getId())
                                    .setQuestionId(entry.getKey())
                                    .setText(entry.getValue())
                                    .setCreatedAt(currentTS);
                            db.persist(answer);
                        }

                        // Ask if the user wants to record more responses
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setMessage("Do you want to record another response?").setTitle("Response Saved")
                            .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // FIRE ZE MISSILES!
                                    Intent intent = new Intent(getActivity(), QuestionActivity.class);
                                    intent.putExtra("surveyId", surveyId);
                                    intent.putExtra("surveyName", surveyName);
                                    intent.putExtra("schoolId", schoolId);
                                    startActivity(intent);
                                }
                            })
                            .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // User cancelled the dialog
                                    Intent intent = new Intent(getActivity(), SurveyDetails.class);
                                    intent.putExtra("surveyId", surveyId);
                                    intent.putExtra("surveyName", surveyName);
                                    startActivity(intent);
                                }
                            });
                        // Create the AlertDialog object and return it
                        builder.show();
                    }
                });
                linearLayoutQuestions.addView(mSubmitButton);
            }
            super.onPostExecute(result);
        }
    }
}

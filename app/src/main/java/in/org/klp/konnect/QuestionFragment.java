package in.org.klp.konnect;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import in.org.klp.konnect.adapters.QuestionAdapter;
import in.org.klp.konnect.db.Answer;
import in.org.klp.konnect.db.KontactDatabase;
import in.org.klp.konnect.db.Question;
import in.org.klp.konnect.db.QuestionGroup;
import in.org.klp.konnect.db.QuestionGroupQuestion;
import in.org.klp.konnect.db.School;
import in.org.klp.konnect.db.Story;
import in.org.klp.konnect.utils.SessionManager;

public class QuestionFragment extends Fragment {

    private QuestionAdapter mQuestionsAdapter;
    private Long surveyId;
    private String surveyName;
    private Long schoolId;
    private Long questionGroupId;
    private String mSelectedUserType;
    private LinearLayout linearLayoutQuestions;
    private KontactDatabase db;
    SessionManager session;
    private LinkedHashMap<String, String> userType;

    public QuestionFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        db = ((KLPApplication) getActivity().getApplicationContext()).getDb();

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
        TextView textViewSchoolId = (TextView) rootView.findViewById(R.id.textViewSchoolId);
        textViewSchool.setText(school.getName());
        textViewSchoolId.setText("KLP ID: " + String.valueOf(school.getId()));

        if (surveyId == 0) {
            Intent intentMain = new Intent(getActivity(), MainActivity.class);
            startActivity(intentMain);
        }

        // defining user types
        userType = new LinkedHashMap<String, String>();
        userType.put("Parents/ಪೋಷಕ", "PR");
        userType.put("Teachers/ಶಿಕ್ಷಕ", "TR");
        userType.put("Volunteers/ಸ್ವಯಂಸೇವಕ", "VR");
        userType.put("CBO Member/ ಸಿ ಬಿ ಒ ಸದಸ್ಯ", "CM");
        userType.put("Headmaster/ಮುಖ್ಯೋಪಾಧ್ಯಾಯ", "HM");
        userType.put("SDMC Member/ಎಸ್ ಡಿ ಎಮ್ ಸಿ ಸದಸ್ಯ", "SM");
        userType.put("Local Leaders/ಮುಖ್ಯಸ್ಥ", "LL");
        userType.put("Akshara Staff/ಅಕ್ಷರ ಶಿಬ್ಬಂದಿ", "AS");
        userType.put("Educated Youth/ವಿದ್ಯಾವಂತ ಯುವಕ ", "EY");
        userType.put("Education Official/ಶಿಕ್ಷಣ ಅಧಿಕಾರಿ", "EO");
        userType.put("Elected Representative/ಚುನಾಯಿತ ಪ್ರತಿನಿಧಿ", "ER");

        final Spinner spinnerUserType = (Spinner) rootView.findViewById(R.id.spinnerUserType);
        List<String> userTypeNames = new ArrayList<>();
        userTypeNames.addAll(userType.keySet());
        ArrayAdapter<String> userTypeAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_dropdown_item, userTypeNames);
        spinnerUserType.setAdapter(userTypeAdapter);
        mSelectedUserType = "PA";

        // this to remove all the invalid answers created by a bug in last release
        db.deleteWhere(Answer.class, Answer.TEXT.notIn("Yes", "No", "Don't Know"));

        SquidCursor<QuestionGroup> qgCursor = null;
        SquidCursor<QuestionGroupQuestion> qgqCursor = null;
        mQuestionsAdapter = new QuestionAdapter(new ArrayList<Question>(), getActivity());

        // select * from questiongroup where survey_id=surveyId limit 1
        Query listQGquery = Query.select().from(QuestionGroup.TABLE)
                .where(QuestionGroup.SURVEY_ID.eq(surveyId)).limit(1);
        qgCursor = db.query(QuestionGroup.class, listQGquery);

        try {
            while (qgCursor.moveToNext()) {
                questionGroupId = qgCursor.get(QuestionGroup.ID);
                Log.d(this.toString(), questionGroupId.toString());
                // select * from questiongroupquestion
                // where questiongroup_id=questionGroupId
                // order by sequence
                Query listQGQquery = Query.select().from(QuestionGroupQuestion.TABLE)
                        .where(QuestionGroupQuestion.QUESTIONGROUP_ID.eq(questionGroupId))
                        .orderBy(QuestionGroupQuestion.SEQUENCE.asc());
                qgqCursor = db.query(QuestionGroupQuestion.class, listQGQquery);

                while (qgqCursor.moveToNext()) {
                    Long qID = qgqCursor.get(QuestionGroupQuestion.QUESTION_ID);
                    // select * from question where id=qID
                    Question question = db.fetch(Question.class, qID);
                    mQuestionsAdapter.add(question);
                }
            }
        } finally {
            if (qgCursor != null) {
                qgCursor.close();
            }
            if (qgqCursor != null) {
                qgqCursor.close();
            }
        }
        ListView listViewQuestions = (ListView) rootView.findViewById(R.id.listViewQuestions);
        listViewQuestions.setAdapter(mQuestionsAdapter);

        Button btnSubmit = (Button) rootView.findViewById(R.id.btnSubmit);
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HashMap<String, String> user = session.getUserDetails();
                Long currentTS = System.currentTimeMillis();
                mSelectedUserType = userType.get(spinnerUserType.getSelectedItem().toString());

                HashMap<Question, String> answers = mQuestionsAdapter.getAnswers();

                if (answers.isEmpty()) {
                    AlertDialog noAnswerDialog = new AlertDialog.Builder(getContext()).create();
                    noAnswerDialog.setTitle(R.string.survey_empty_response_title);
                    noAnswerDialog.setMessage(getString(R.string.survey_empty_response_body));
                    noAnswerDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.response_neutral),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                }
                            });
                    noAnswerDialog.show();
                } else {
                    Story story = new Story()
                            .setSchoolId(schoolId)
                            .setUserId(Long.parseLong(user.get(session.KEY_ID)))
                            .setGroupId(questionGroupId)
                            .setRespondentType(mSelectedUserType)
                            .setCreatedAt(currentTS);
                    db.persist(story);
                    Log.d(this.toString(), "Created story: " + String.valueOf(story.getId()));

                    for (Map.Entry<Question, String> answer : answers.entrySet()) {
                        Question q = answer.getKey();
                        String a = answer.getValue();

                        Answer new_answer = new Answer()
                                .setStoryId(story.getId())
                                .setQuestionId(q.getId())
                                .setText(a)
                                .setCreatedAt(currentTS);
                        db.persist(new_answer);
                        Log.d(this.toString(), "Created answer: " + String.valueOf(new_answer.getId()));
                    }

                    // Ask if the user wants to record more responses
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setMessage(getString(R.string.prompt_new_rsponse)).setTitle("Response Saved")
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
            }
        });

        return rootView;
    }
}

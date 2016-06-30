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
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Query;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import in.org.klp.kontact.adapters.QuestionAdapter;
import in.org.klp.kontact.db.Answer;
import in.org.klp.kontact.db.KontactDatabase;
import in.org.klp.kontact.db.Question;
import in.org.klp.kontact.db.QuestionGroup;
import in.org.klp.kontact.db.QuestionGroupQuestion;
import in.org.klp.kontact.db.School;
import in.org.klp.kontact.db.Story;
import in.org.klp.kontact.utils.SessionManager;

public class QuestionFragment extends Fragment {

    private QuestionAdapter mQuestionsAdapter;
    private Long surveyId;
    private String surveyName;
    private Long schoolId;
    private Long questionGroupId;
    private LinearLayout linearLayoutQuestions;
    private KontactDatabase db;
    SessionManager session;

    public QuestionFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
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
                    mQuestionsAdapter.addAnswer(question, getString(R.string.answer_unknown));
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

                Story story = new Story()
                        .setSchoolId(schoolId)
                        .setUserId(Long.parseLong(user.get(session.KEY_ID)))
                        .setGroupId(questionGroupId)
                        .setCreatedAt(currentTS);
                db.persist(story);
                Log.d(this.toString(), "Created story: " + String.valueOf(story.getId()));

                HashMap<Question, String> answers = mQuestionsAdapter.getAnswers();
                for (Map.Entry<Question, String> answer: answers.entrySet()) {
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
        });

        return rootView;
    }
}

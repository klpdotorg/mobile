package in.org.klp.kontact;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Query;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;

import in.org.klp.kontact.db.KontactDatabase;
import in.org.klp.kontact.db.Question;
import in.org.klp.kontact.db.QuestionGroup;
import in.org.klp.kontact.db.QuestionGroupQuestion;

public class QuestionFragment extends Fragment {

    private ArrayAdapter<String> mQuestionsAdapter;
    private String surveyId;
    private String surveyName;
    private String schoolId;
    private LinearLayout linearLayoutQuestions;
    private KontactDatabase db;

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

        Intent intent = getActivity().getIntent();

        surveyId = intent.getStringExtra("surveyId");
        surveyName = intent.getStringExtra("surveyName");
        schoolId = intent.getStringExtra("schoolId");

        View rootView = inflater.inflate(R.layout.fragment_question, container, false);

        linearLayoutQuestions = (LinearLayout) rootView.findViewById(R.id.linearLayoutQuestions);

        return rootView;
    }

    public class FetchQuestionsTask extends AsyncTask<Void, Void, ArrayList<Question>> {

        private final String LOG_TAG = FetchQuestionsTask.class.getSimpleName();

        @Override
        protected ArrayList<Question> doInBackground(Void... params) {
            Intent intent = getActivity().getIntent();
            surveyId = intent.getStringExtra("surveyId");

            db = new KontactDatabase(getActivity());
            SquidCursor<QuestionGroup> qgCursor = null;
            SquidCursor<QuestionGroupQuestion> qgqCursor = null;

            Query listQGquery = Query.select().from(QuestionGroup.TABLE)
                    .where(QuestionGroup.SURVEY_ID.eq(Long.valueOf(surveyId))).limit(1);
            qgCursor = db.query(QuestionGroup.class, listQGquery);

            try {
                while (qgCursor.moveToFirst()) {
                    Long qgID = qgCursor.get(QuestionGroup.ID);
                    Query listQGQquery = Query.select().from(QuestionGroupQuestion.TABLE)
                            .where(QuestionGroupQuestion.QUESTIONGROUP_ID.eq(qgID));
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
                        ArrayList answerArray = new ArrayList();

                        for (int i = 0; i < linearLayoutQuestions.getChildCount(); i++) {
                            View child = linearLayoutQuestions.getChildAt(i);
                            if (child.getClass() == Switch.class) {
                                Switch cSwitch = (Switch) child;
                                Long questionId = (Long) child.getTag();
                                String answer = String.valueOf(cSwitch.isChecked());
                            }
                        }

                        // Now answerArray is filled with question ids and their answers
                        // TODO: Save answers in DB

                        // Ask if the user wants to record more responses
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setMessage("Do you want to record another response?")
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

package in.org.klp.kontact;

import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;

import in.org.klp.kontact.data.SurveyDbHelper;

public class QuestionFragment extends Fragment {

    private ArrayAdapter<String> mQuestionsAdapter;
    String surveyId, schoolId;
    String surveyName, boundary;

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
        boundary = intent.getStringExtra("boundary");


        View rootView = inflater.inflate(R.layout.fragment_question, container, false);
        TextView textView=(TextView) rootView.findViewById(R.id.display_boundary);
        textView.setText(boundary);
        ListView listview = (ListView) rootView.findViewById(R.id.listview_question);
        listview.setAdapter(mQuestionsAdapter);

        return rootView;
    }

    public class FetchQuestionsTask extends AsyncTask<Void, Void, String[]> {

        private final String LOG_TAG = FetchQuestionsTask.class.getSimpleName();
        SurveyDbHelper dbHelper;

        @Override
        protected String[] doInBackground(Void... params) {

            dbHelper = new SurveyDbHelper(getActivity());

            Intent intent = getActivity().getIntent();
            surveyId = intent.getStringExtra("surveyId");

            Cursor qg_cursor = dbHelper.list_questiongroups(surveyId);
            if (qg_cursor.getCount() >= 1) {

                String questionText;
                String questiongroupId;

                qg_cursor.moveToNext();
                questiongroupId = qg_cursor.getString(0);
                qg_cursor.close();

                Cursor question_cursor = dbHelper.list_questions(questiongroupId);

                int count = 0;
                String[] resultStrs = new String[question_cursor.getCount()];
                while(question_cursor.moveToNext()) {
                    questionText = question_cursor.getString(1);

                    resultStrs[count] = questionText;
                    count++;
                    Log.v(LOG_TAG, "Survey: " + questionText);
                }

                return resultStrs;
            }
            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {
                mQuestionsAdapter.clear();
                for (String surveyStr : result) {
                    mQuestionsAdapter.add(surveyStr);
                }
            }
            super.onPostExecute(result);
        }
    }
}

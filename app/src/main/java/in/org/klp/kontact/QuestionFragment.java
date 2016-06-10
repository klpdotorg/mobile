package in.org.klp.kontact;

import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;

import in.org.klp.kontact.data.SurveyDbHelper;

public class QuestionFragment extends Fragment {

    private ArrayAdapter<String> mQuestionsAdapter;
    String surveyId;
    String surveyName;

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

        View rootView = inflater.inflate(R.layout.fragment_question, container, false);

        ListView listview = (ListView) rootView.findViewById(R.id.listview_question);
        listview.setAdapter(mQuestionsAdapter);

        return rootView;
    }

    public class FetchQuestionsTask extends AsyncTask<Void, Void, String[]> {

        private final String LOG_TAG = FetchQuestionsTask.class.getSimpleName();
        SurveyDbHelper dbHelper;

        @Override
        protected String[] doInBackground(Void... params) {

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String surveyJsonStr = null;

            dbHelper = new SurveyDbHelper(getActivity());

            Cursor cursor = dbHelper.list_surveys();
            if (cursor.getCount() >= 1) {
                String surveyId;
            }
            return null;
        }
    }
}

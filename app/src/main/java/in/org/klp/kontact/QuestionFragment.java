package in.org.klp.kontact;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class QuestionFragment extends Fragment {

    private ArrayAdapter<String> mQuestionsAdapter;


    public QuestionFragment() {
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

        View rootView = inflater.inflate(R.layout.fragment_question, container, false);

        ListView listview = (ListView) rootView.findViewById(R.id.listview_question);
        listview.setAdapter(mQuestionsAdapter);

        return rootView;
    }
}

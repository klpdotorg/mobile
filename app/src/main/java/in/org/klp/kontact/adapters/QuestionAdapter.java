package in.org.klp.kontact.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import in.org.klp.kontact.R;
import in.org.klp.kontact.data.StringWithTags;
import in.org.klp.kontact.db.Question;

/**
 * Created by bibhas on 6/18/16.
 */
public class QuestionAdapter extends ArrayAdapter<Question> {
    private Context _context;
    private ArrayList<Question> questions;
    private HashMap<Question, String> answers;

    // View lookup cache
    private static class QuestionHolder {
        TextView qText;
        RadioGroup rgQuestion;
    }

    public QuestionAdapter(ArrayList<Question> questions, Context context) {
        super(context, R.layout.list_item_question, questions);
        this._context = context;
        this.questions = questions;
        this.answers = new HashMap<Question, String>();
    }

    @Override
    public int getCount() {
        return questions.size(); // size, lenght, count ...?
    }

    @Override
    public Question getItem(int position) {
        return questions.get(position);
    }

    public HashMap<Question, String> getAnswers() {
        return answers;
    }

    @Override
    public View getView(int position, View convertView, final ViewGroup parent) {
        final Question question = getItem(position);
        QuestionHolder questionHolder = new QuestionHolder();;
        final View result = convertView;

        LayoutInflater inflater = LayoutInflater.from(getContext());
        convertView = inflater.inflate(R.layout.list_item_question, parent, false);
        questionHolder.qText = (TextView) convertView.findViewById(R.id.textViewQuestion);
        questionHolder.qText.setText(String.valueOf(question.getId()) + ": " + question.getText());
        questionHolder.qText.setTag(question.getId());

        questionHolder.rgQuestion = (RadioGroup) convertView.findViewById(R.id.rgQuestion);
        // set question id a key
        questionHolder.rgQuestion.setTag(question.getId());
        for (int i = 0; i < questionHolder.rgQuestion.getChildCount(); i++) {
            RadioButton rb = (RadioButton) questionHolder.rgQuestion.getChildAt(i);
            if (answers.get(question) == null) {
                if (rb.isChecked()) {
                    answers.put(question, rb.getText().toString());
                }
            } else if (rb.getText().toString() == answers.get(question)) {
                rb.setChecked(true);
            }
        }

        questionHolder.rgQuestion.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                RadioButton rb = (RadioButton) parent.findViewById(radioGroup.getCheckedRadioButtonId());
                answers.put(question, rb.getText().toString());
            }
        });

        return convertView;
    }
}

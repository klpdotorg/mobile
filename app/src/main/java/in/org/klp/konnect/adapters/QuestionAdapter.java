package in.org.klp.konnect.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import in.org.klp.konnect.R;
import in.org.klp.konnect.db.Question;

/**
 * Created by bibhas on 6/18/16.
 */
public class QuestionAdapter extends ArrayAdapter<Question> {
    private Context _context;
    private ArrayList<Question> questions;
    private HashMap<Question, String> answers;
    private HashMap<Integer, String> radioButtons;

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

        // can't rely on the radio button text as
        // we're using kannada strings, so using button text ID
        // which is same for both english and kannada strings
        // Here we're just mapping button id to Yes/No/Don't Know
        // because those are what we're saving in database
        this.radioButtons = new HashMap<Integer, String>();
        this.radioButtons.put(R.id.radioYes, "Yes");
        this.radioButtons.put(R.id.radioNo, "No");
        this.radioButtons.put(R.id.radioNoAnswer, "Don't Know");
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

    public void addAnswer(Question question, String answer) {
        this.answers.put(question, answer);
    }

    @Override
    public View getView(int position, View convertView, final ViewGroup parent) {
        final Question question = getItem(position);
        QuestionHolder questionHolder = new QuestionHolder();;
        final View result = convertView;

        LayoutInflater inflater = LayoutInflater.from(_context);
        convertView = inflater.inflate(R.layout.list_item_question, parent, false);
        questionHolder.qText = (TextView) convertView.findViewById(R.id.textViewQuestion);
        questionHolder.qText.setText(question.getTextKn() != null ? question.getTextKn() : question.getText());
        questionHolder.qText.setTag(question.getId());
        questionHolder.qText.setTypeface(questionHolder.qText.getTypeface(), Typeface.BOLD);

        questionHolder.rgQuestion = (RadioGroup) convertView.findViewById(R.id.rgQuestion);
        // set question id a key
        questionHolder.rgQuestion.setTag(question.getId());
        for (int i = 0; i < questionHolder.rgQuestion.getChildCount(); i++) {
            RadioButton rb = (RadioButton) questionHolder.rgQuestion.getChildAt(i);
            if (answers.get(question) == null) {
                if (rb.isChecked()) {
                    answers.put(question, radioButtons.get(rb.getId()));
                }
            } else if (radioButtons.get(rb.getId()) == answers.get(question)) {
                rb.setChecked(true);
            }
        }

        questionHolder.rgQuestion.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                RadioButton rb = (RadioButton) parent.findViewById(radioGroup.getCheckedRadioButtonId());
                answers.put(question, radioButtons.get(rb.getId()));
            }
        });

        return convertView;
    }
}

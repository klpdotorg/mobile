package in.org.klp.kontact.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import in.org.klp.kontact.R;
import in.org.klp.kontact.db.Survey;

/**
 * Created by bibhas on 6/18/16.
 */
public class SurveyAdapter extends ArrayAdapter<Survey> {
    private Context _context;
    private ArrayList<Survey> surveys;

    // View lookup cache
    private static class SurveyHolder {
        TextView name;
    }

    public SurveyAdapter(ArrayList<Survey> surveys, Context context) {
        super(context, R.layout.list_item_survey, surveys);
        this._context = context;
        this.surveys = surveys;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Survey survey = getItem(position);
        SurveyHolder surveyHolder = new SurveyHolder();;
        final View result = convertView;

        LayoutInflater inflater = LayoutInflater.from(getContext());
        convertView = inflater.inflate(R.layout.list_item_survey, parent, false);
        surveyHolder.name = (TextView) convertView.findViewById(R.id.list_item_survey_textview);
        surveyHolder.name.setText(survey.getName());
        surveyHolder.name.setTag(survey.getId());

        return convertView;
    }
}

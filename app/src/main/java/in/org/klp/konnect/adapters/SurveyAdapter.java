package in.org.klp.konnect.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import in.org.klp.konnect.R;
import in.org.klp.konnect.db.Survey;

/**
 * Created by bibhas on 6/18/16.
 */
public class SurveyAdapter extends ArrayAdapter<Survey> {
    private Context _context;
    private ArrayList<Survey> surveys;

    // View lookup cache
    private static class SurveyHolder {
        TextView name;
        TextView partner;
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
        surveyHolder.name = (TextView) convertView.findViewById(R.id.textViewName);
        surveyHolder.name.setText(survey.getName());
        surveyHolder.name.setTag(survey.getId());

        surveyHolder.partner = (TextView) convertView.findViewById(R.id.textViewPartner);
        surveyHolder.partner.setText(survey.getPartner());

        return convertView;
    }
}

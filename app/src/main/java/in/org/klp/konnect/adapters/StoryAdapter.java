package in.org.klp.konnect.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import in.org.klp.konnect.R;
import in.org.klp.konnect.db.Answer;
import in.org.klp.konnect.db.Boundary;
import in.org.klp.konnect.db.KontactDatabase;
import in.org.klp.konnect.db.School;
import in.org.klp.konnect.db.Story;

/**
 * Created by bibhas on 6/18/16.
 */
public class StoryAdapter extends ArrayAdapter<Story> {
    private Context _context;
    private ArrayList<Story> stories;
    private KontactDatabase db;
    private String loggedinUserId;

    // View lookup cache
    private static class SurveyHolder {
        TextView school;
        TextView metaBdry;
        TextView metaMedium;
        TextView metaSmall;
        TextView metaRight;
    }

    public StoryAdapter(ArrayList<Story> stories, Context context, KontactDatabase db, String loggedinUserId) {
        super(context, R.layout.list_item_survey, stories);
        this._context = context;
        this.stories = stories;
        this.db = db;
        this.loggedinUserId = loggedinUserId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Story story = getItem(position);
        School school = db.fetch(School.class, story.getSchoolId());
        Boundary cluster = db.fetch(Boundary.class, school.getBoundaryId());
        Boundary block = db.fetch(Boundary.class, cluster.getParentId());
        Boundary district = db.fetch(Boundary.class, block.getParentId());
        Integer answerCount = db.count(Answer.class, Answer.STORY_ID.eq(story.getId()));
        SurveyHolder surveyHolder = new SurveyHolder();

        LayoutInflater inflater = LayoutInflater.from(getContext());
        convertView = inflater.inflate(R.layout.list_item_story, parent, false);

        surveyHolder.school = (TextView) convertView.findViewById(R.id.tvSchoolName);
        surveyHolder.school.setText(school.getName());
        surveyHolder.school.setTag(story.getId());

        surveyHolder.metaBdry = (TextView) convertView.findViewById(R.id.tvMetaBdry);
        surveyHolder.metaBdry.setText(cluster.getName() + ", " + block.getName() + ", " + district.getName());

        surveyHolder.metaMedium = (TextView) convertView.findViewById(R.id.tvMetaMedium);
        String txt_metamedium = String.format(parent.getResources().getString(R.string.meta_medium), answerCount.toString());
        surveyHolder.metaMedium.setText(txt_metamedium);

        surveyHolder.metaSmall = (TextView) convertView.findViewById(R.id.tvMetaSmall);
        String txt_metasmall = String.format(
                parent.getResources().getString(R.string.meta_small),
                (loggedinUserId.equals(story.getUserId().toString()) ? "you" : "User " + story.getUserId().toString()),
                getDate(story.getCreatedAt())
        );
        surveyHolder.metaSmall.setText(txt_metasmall);

        surveyHolder.metaRight = (TextView) convertView.findViewById(R.id.tvMetaRight);
        surveyHolder.metaRight.setText((story.getSynced().equals(1)) ? "synced" : "not synced");
        surveyHolder.metaRight.setTextColor((story.getSynced().equals(1)) ? _context.getResources().getColor(R.color.colorBrandGreen) : _context.getResources().getColor(R.color.colorBrandRed  ));

        return convertView;
    }

    private String getDate(long time) {
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(time);
        String date = DateFormat.format("dd-MM-yyyy 'a't hh:MM:ss aa", cal).toString();
        return date;
    }
}

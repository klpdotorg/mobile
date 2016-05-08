package in.org.klp.kontact;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        String[] data = {
                "Akshara Primary School Survey",
                "GKA Survey",
                "Akshaya Patra Survey",
                "PreSchool Survey",
                "EkStep Survey",
                "Notorious Survey",
                "The Ultimate Survey"
        };

        List<String> Surveys = new ArrayList<String>(Arrays.asList(data));

        return inflater.inflate(R.layout.fragment_main, container, false);
    }
}

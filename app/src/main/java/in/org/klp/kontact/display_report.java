package in.org.klp.kontact;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link display_report.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link display_report#newInstance} factory method to
 * create an instance of this fragment.
 */
public class display_report extends Fragment {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "question_name";
    private static final String ARG_PARAM3 = "agg";
    private static final String ARG_PARAM4 = "blck_agg";
    private static final String ARG_PARAM5 = "dist_agg";

    private String mParam1;
    private String q_name, agg, schoolcount, schoolwithresponse, responses, desc, blck_agg, dist_agg;

    private OnFragmentInteractionListener mListener;

    public display_report() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment display_report.
     */
    public static display_report newInstance(String param1, String param2, String param3, String param4, String param5) {
        display_report fragment = new display_report();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        args.putString(ARG_PARAM3, param3);
        args.putString(ARG_PARAM4, param4);
        args.putString(ARG_PARAM5, param5);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            q_name = getArguments().getString(ARG_PARAM2);
            String[] output = getArguments().getString(ARG_PARAM3).toString().trim().split("\\|");
            agg=output[0]+"%";
            schoolcount=output[1];
            schoolwithresponse=output[2];
            responses=output[3];
            desc=output[4];
            blck_agg = getArguments().getString(ARG_PARAM4);
            dist_agg = getArguments().getString(ARG_PARAM5);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view= inflater.inflate(R.layout.fragment_display_report, container, false);
        TextView tv=(TextView) view.findViewById(R.id.question_name);
        tv.setText(q_name);
        tv=(TextView) view.findViewById(R.id.aggregate);
        tv.setText(agg);
        tv=(TextView) view.findViewById(R.id.school_count);
        tv.setText(schoolcount);
        tv=(TextView) view.findViewById(R.id.school_resp_count);
        tv.setText(schoolwithresponse);
        tv=(TextView) view.findViewById(R.id.res_count);
        tv.setText(responses);
        tv=(TextView) view.findViewById(R.id.desc);
        tv.setText(desc);
        tv=(TextView) view.findViewById(R.id.block_aggregate);
        tv.setText(blck_agg);
        tv=(TextView) view.findViewById(R.id.district_aggregate);
        tv.setText(dist_agg);
        return view;
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}

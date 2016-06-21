package in.org.klp.kontact.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import in.org.klp.kontact.LoginActivity;

/**
 * Created by Subha on 5/31/16.
 */
public class SignUpResultDialogFragment extends DialogFragment {


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final Bundle args = getArguments();
        builder.setMessage(args.getString("result"))
                .setTitle(args.getString("title", ""))
                .setNeutralButton(args.getString("buttonText"), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if(args.getString("buttonText").equalsIgnoreCase("Login")) {
                            Intent intent = new Intent(getActivity(), LoginActivity.class);
                            startActivity(intent);
                        }
                    }
                });

        // Create the AlertDialog object and return it
        return builder.create();
    }
}

package in.org.klp.kontact.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import in.org.klp.kontact.R;


/**
 * Created by Subha on 7/6/16.
 */
public class ChangePasswordDialogFragment extends DialogFragment
{
    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface NoticeDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog);
        public void onDialogNegativeClick(DialogFragment dialog);
    }

    NoticeDialogListener mListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(inflater.inflate(R.layout.dialog_change_password, null))
        .setMessage("Change Password")
                .setPositiveButton(R.string.Submit, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //Check the fields..
                        EditText oldPassword = (EditText) getActivity().findViewById(R.id.oldpassword);
                        EditText newPassword = (EditText) getActivity().findViewById(R.id.newpassword);
                        EditText reenterPassword = (EditText) getActivity().findViewById(R.id.reenterpassword);
                        View focusView = null;
                        boolean cancel = false;
                        if(oldPassword.getText() == null || oldPassword.getText().length() == 0)
                        {
                            oldPassword.setError(getString(R.string.error_field_required));
                            focusView = oldPassword;
                            cancel = true;
                        }
                        else if(newPassword.getText() == null || newPassword.getText().length() == 0)
                        {
                            newPassword.setError(getString(R.string.error_field_required));
                            focusView = newPassword;
                            cancel = true;
                        }
                        else if(reenterPassword.getText() == null || reenterPassword.getText().length() == 0)
                        {
                            reenterPassword.setError(getString(R.string.error_field_required));
                            focusView = reenterPassword;
                            cancel = true;
                        }
                        else if(!newPassword.getText().equals(reenterPassword.getText()))
                        {
                            reenterPassword.setError(getString(R.string.dialog_error_password_mismatch));
                            focusView = reenterPassword;
                            cancel = true;
                        }
                        if (cancel) {
                            // There was an error; don't attempt login and focus the first
                            // form field with an error.
                            focusView.requestFocus();
                        } 

                    }
                })
                .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }


    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (NoticeDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }
}

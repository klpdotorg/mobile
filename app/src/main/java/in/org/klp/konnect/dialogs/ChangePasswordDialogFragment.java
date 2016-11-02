//package in.org.klp.kontact.dialogs;
//
//import android.annotation.TargetApi;
//import android.app.Activity;
//import android.app.AlertDialog;
//import android.app.Dialog;
//import android.app.DialogFragment;
//import android.content.DialogInterface;
//import android.content.DialogInterface.OnClickListener;
//import android.os.Build.VERSION_CODES;
//import android.os.Bundle;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.widget.EditText;
//
//import in.org.klp.konnect.R;
//
//
///**
// * Created by Subha on 7/6/16.
// */
//public class ChangePasswordDialogFragment extends DialogFragment
//{
//
//    boolean dismissDialog = true;
//    /* The activity that creates an instance of this dialog fragment must
//     * implement this interface in order to receive event callbacks.
//     * Each method passes the DialogFragment in case the host needs to query it. */
//    public interface NoticeDialogListener {
//        public void onDialogPositiveClick(DialogFragment dialog);
//        public void onDialogNegativeClick(DialogFragment dialog);
//    }
//
//    NoticeDialogListener mListener;
//
//    @TargetApi(VERSION_CODES.JELLY_BEAN_MR1)
//    @Override
//    public Dialog onCreateDialog(Bundle savedInstanceState) {
//        // Use the Builder class for convenient dialog construction
//        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//        // Get the layout inflater
//        LayoutInflater inflater = getActivity().getLayoutInflater();
//
//        // Inflate and set the layout for the dialog
//        // Pass null as the parent view because its going in the dialog layout
//        builder.setView(inflater.inflate(R.layout.dialog_change_password, null))
//        .setMessage("Change Password")
//                .setPositiveButton(R.string.Submit, new OnClickListener() {
//                    public void onClick(DialogInterface dialog, int id) {
//                        Dialog alertDialog = (Dialog)dialog;
//                        //Check the fields..
//                        EditText oldPassword = (EditText) alertDialog.findViewById(R.id.oldpassword);
//                        EditText newPassword = (EditText) alertDialog.findViewById(R.id.newpassword);
//                        EditText reenterPassword = (EditText) alertDialog.findViewById(R.id.reenterpassword);
//                        View focusView = null;
//
//                        if(oldPassword.getText() == null || oldPassword.getText().length() == 0)
//                        {
//                            oldPassword.setError(getString(R.string.error_field_required));
//                            focusView = oldPassword;
//                            dismissDialog = false;
//                        }
//                        else if(newPassword.getText() == null || newPassword.getText().length() == 0)
//                        {
//                            newPassword.setError(getString(R.string.error_field_required));
//                            focusView = newPassword;
//                            dismissDialog = false;
//                        }
//                        else if(reenterPassword.getText() == null || reenterPassword.getText().length() == 0)
//                        {
//                            reenterPassword.setError(getString(R.string.error_field_required));
//                            focusView = reenterPassword;
//                            dismissDialog = false;
//                        }
//                        else if(!newPassword.getText().equals(reenterPassword.getText()))
//                        {
//                            reenterPassword.setError(getString(R.string.dialog_error_password_mismatch));
//                            focusView = reenterPassword;
//                            dismissDialog = false;
//                        }
//                        if (!dismissDialog) {
//                            // There was an error; don't attempt login and focus the first
//                            // form field with an error.
//                            focusView.requestFocus();
//                            //builder.show();
//                            //((Dialog) dialog).show();
//                        }
//
//
//
//                    }
//                })
//                .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int id) {
//                        // User cancelled the dialog
//                    }
//                });
//
//        // Create the AlertDialog object and return it
//        return builder.create();
//    }
//
//
//    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
//    @Override
//    public void onAttach(Activity activity) {
//        super.onAttach(activity);
//        // Verify that the host activity implements the callback interface
//        try {
//            // Instantiate the NoticeDialogListener so we can send events to the host
//            mListener = (NoticeDialogListener) activity;
//        } catch (ClassCastException e) {
//            // The activity doesn't implement the interface, throw exception
//            throw new ClassCastException(activity.toString()
//                    + " must implement NoticeDialogListener");
//        }
//    }
//}

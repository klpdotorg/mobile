package in.org.klp.kontact;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import in.org.klp.kontact.dialogs.SignUpResultDialogFragment;
import in.org.klp.kontact.utils.RESTResponse;
import in.org.klp.kontact.utils.Utils;

public class UserRegistrationActivity extends AppCompatActivity {

    public String LOG_TAG = UserRegistrationActivity.class.getSimpleName();

    //UI references

    private AutoCompleteTextView emailWidget;
    private TextView passwordWidget;
    private TextView verifyPasswordWidget;
    private TextView lastNameWidget, firstNameWidget, phoneNoWidget;

    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_registration);

        TextView loginLink = (TextView)findViewById(R.id.backtologin);

        Linkify.addLinks(loginLink, Linkify.ALL);

        loginLink.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(UserRegistrationActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//
//
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Button mEmailSignUpButton = (Button) findViewById(R.id.register_button);
        emailWidget = (AutoCompleteTextView)findViewById(R.id.user_email);
        passwordWidget = (TextView)findViewById(R.id.password);
        verifyPasswordWidget = (TextView)findViewById(R.id.verify_password);
        firstNameWidget = (TextView)findViewById(R.id.user_first_name);
        lastNameWidget = (TextView)findViewById(R.id.user_last_name);
        phoneNoWidget = (TextView)findViewById(R.id.user_phone);
        mEmailSignUpButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {


                //Add error checking here
                String emailValue = ((Editable) emailWidget.getText()).toString();
                String passwordValue = ((Editable)passwordWidget.getText()).toString();
                String verifyPasswordValue = ((Editable)verifyPasswordWidget.getText()).toString();
                String firstNameValue = ((Editable)firstNameWidget.getText()).toString();
                String lastNameValue = ((Editable)lastNameWidget.getText()).toString();
                String phoneNoValue = ((Editable)phoneNoWidget.getText()).toString();

                View focusView = null;
                boolean cancel = false;

                if(TextUtils.isEmpty(emailValue)) {
                    emailWidget.setError("This field is required");
                    focusView = emailWidget;
                    cancel = true;
                }
                else if(!isEmailValid(emailValue))
                {
                    emailWidget.setError("This email address is invalid");
                    focusView = emailWidget;
                    cancel = true;
                }
                else if(TextUtils.isEmpty(passwordValue))
                {
                    passwordWidget.setError("This field is required");
                    focusView = passwordWidget;
                    cancel = true;
                }
                else if(TextUtils.isEmpty(verifyPasswordValue) || !passwordValue.equals(verifyPasswordValue))
                {
                    verifyPasswordWidget.setError("This field is required and must match the value entered in the password field");
                    focusView = verifyPasswordWidget;
                    cancel = true;
                }
                else if(TextUtils.isEmpty(firstNameValue))
                {
                    firstNameWidget.setError("This field is required");
                    focusView = firstNameWidget;
                    cancel = true;
                }
                else if(TextUtils.isEmpty(lastNameValue))
                {
                    lastNameWidget.setError("This field is required");
                    focusView = lastNameWidget;
                    cancel = true;
                }
                else if(TextUtils.isEmpty(phoneNoValue) || phoneNoValue.length()<10 || !TextUtils.isDigitsOnly(phoneNoValue))
                {
                    phoneNoWidget.setError("Please enter a valid phone number");
                    focusView = phoneNoWidget;
                    cancel = true;
                }

                //If no errors, proceed with post to server.
                if(!cancel) {
                    String[] dummyData = new String[]{emailValue, passwordValue, firstNameValue, lastNameValue, phoneNoValue};
                    new RegisterUserTask().execute(dummyData);
                }
                else
                {
                    //There was an error. Do not attempt sign up. Just show the form field with the error
                    focusView.requestFocus();
                }
                //Delete this
//                RESTResponse dummyResp = new RESTResponse(HttpURLConnection.HTTP_CREATED, "worked");
//                finishSignUp(dummyResp);

            }
        });
    }

    protected void finishSignUp(RESTResponse registrationResp)
    {
        //{"id": 165, "email": "abc@www.com", "mobile_no": "5632145236", "first_name": "First Name", "last_name": "Last Name", "opted_email": false, "token": "6c222fbbc0af067baad708e8918fa589f8a7efa3", "volunteer_activities": [], "image": "", "organizations": [], "about": "", "twitter_handle": "", "fb_url": "", "website": "", "photos_url": "", "youtube_url": ""}
        Bundle signUpResult = new Bundle();
        if(registrationResp.getStatusCode() == HttpURLConnection.HTTP_CREATED) {
            signUpResult.putString("result", "You have successfully signed up! Click Login to Login");
            signUpResult.putString("buttonText", "Login");
        }
        else
        {
            signUpResult.putString("result", "Sign up failed. Please try again. Message: " + registrationResp.getErrorMessage());
            signUpResult.putString("buttonText", "Try Again");
        }
        SignUpResultDialogFragment resultDialog = new SignUpResultDialogFragment();
        resultDialog.setArguments(signUpResult);

        resultDialog.show(getSupportFragmentManager(), "Registration result");
        clearAllFields();

    }

    private void clearAllFields()
    {
        emailWidget.clearComposingText();
        passwordWidget.clearComposingText();
        verifyPasswordWidget.clearComposingText();
        firstNameWidget.clearComposingText();
        lastNameWidget.clearComposingText();
        phoneNoWidget.clearComposingText();

        emailWidget.requestFocus();
    }

    public class RegisterUserTask extends AsyncTask<String,Void,RESTResponse>
    {
        private final String LOG_TAG = RegisterUserTask.class.getSimpleName();
        /**
         * Override this method to perform a computation on a background thread. The
         * specified parameters are the parameters passed to {@link #execute}
         * by the caller of this task.
         * <p/>
         * This method can call {@link #publishProgress} to publish updates
         * on the UI thread.
         *
         * @param params The parameters of the task.
         * @return A result, defined by the subclass of this task.
         * @see #onPreExecute()
         * @see #onPostExecute
         * @see #publishProgress
         */
        @Override
        protected RESTResponse doInBackground(String... params) {
            String email = params[0];
            String password=params[1];
            String firstName=params[2];
            String lastName=params[3];
            String phoneNo = params[4];

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            RESTResponse response = null;
            // Will contain the raw JSON response as a string.
            String userInfo = null;

            try {
                final String USER_BASE_URL = "http://dev.klp.org.in/api/v1/users";

                Uri builtUri = Uri.parse(USER_BASE_URL).buildUpon().build();

                URL url = new URL(builtUri.toString());
                String requestParams = "email="+ URLEncoder.encode(email, "UTF-8") + "&password=" + URLEncoder.encode(password, "UTF-8") + "&first_name=" + URLEncoder.encode(firstName, "UTF-8") + "&last_name=" + URLEncoder.encode(lastName, "UTF-8") + "&mobile_no=" + URLEncoder.encode(phoneNo, "UTF-8");
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                urlConnection.setFixedLengthStreamingMode(requestParams.getBytes().length);
                urlConnection.setDoOutput(true);

                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(urlConnection.getOutputStream());
                outputStreamWriter.write(requestParams);
                outputStreamWriter.flush();
                outputStreamWriter.close();

                int responseCode = urlConnection.getResponseCode();
                String responseMsg = urlConnection.getResponseMessage();

                // Read the input stream into a String
                response = Utils.parseHttpResponse(urlConnection);



            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }

            }
            try {
               // Log.v(LOG_TAG, userInfo);
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            return response;
        }

        @Override
        protected void onPostExecute(final RESTResponse userInfo) {
            finishSignUp(userInfo);
        }
    }

}

package in.org.klp.kontact;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class UserRegistrationActivity extends AppCompatActivity {

    public String LOG_TAG = UserRegistrationActivity.class.getSimpleName();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_registration);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//
//
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Button mEmailSignUpButton = (Button) findViewById(R.id.register_button);
        final AutoCompleteTextView emailWidget = (AutoCompleteTextView)findViewById(R.id.user_email);
        final TextView passwordWidget = (TextView)findViewById(R.id.password);
        TextView verifyPasswordWidget = (TextView)findViewById(R.id.verify_password);
        final TextView firstNameWidget = (TextView)findViewById(R.id.user_first_name);
        final TextView lastNameWidget = (TextView)findViewById(R.id.user_last_name);
        final TextView phoneNoWidget = (TextView)findViewById(R.id.user_phone);
        mEmailSignUpButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {


                //Add error checking here
                String emailValue = ((Editable) emailWidget.getText()).toString();
                String passwordValue = ((Editable)passwordWidget.getText()).toString();
                String firstNameValue = ((Editable)firstNameWidget.getText()).toString();
                String lastNameValue = ((Editable)lastNameWidget.getText()).toString();
                String phoneNoValue = ((Editable)phoneNoWidget.getText()).toString();

                String[] dummyData = new String[]{emailValue, passwordValue, firstNameValue, lastNameValue, phoneNoValue};
                new RegisterUserTask().execute(dummyData);
            }
        });
    }

    protected void finishSignUp(String userInfo)
    {
        //{"id": 165, "email": "abc@www.com", "mobile_no": "5632145236", "first_name": "First Name", "last_name": "Last Name", "opted_email": false, "token": "6c222fbbc0af067baad708e8918fa589f8a7efa3", "volunteer_activities": [], "image": "", "organizations": [], "about": "", "twitter_handle": "", "fb_url": "", "website": "", "photos_url": "", "youtube_url": ""}

        Log.v(LOG_TAG, userInfo);
        //Add code to check return values etc...
        Intent intent = new Intent(this.getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }

    public class RegisterUserTask extends AsyncTask<String,Void,String>
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
        protected String doInBackground(String... params) {
            String email = params[0];
            String password=params[1];
            String firstName=params[2];
            String lastName=params[3];
            String phoneNo = params[4];

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

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
                Log.v(LOG_TAG, "Response code is: " + responseCode);
                Log.v(LOG_TAG, "response message is: " + responseMsg);
                if(responseCode == HttpURLConnection.HTTP_OK)
                {
                    Log.v(LOG_TAG, "Response code is: " + responseCode);
                }
                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                userInfo = buffer.toString();

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
            try {
                Log.v(LOG_TAG, userInfo);
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            return userInfo;
        }

        @Override
        protected void onPostExecute(final String userInfo) {
            finishSignUp(userInfo);
        }
    }

}

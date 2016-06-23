package in.org.klp.kontact;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import in.org.klp.kontact.dialogs.SignUpResultDialogFragment;
import in.org.klp.kontact.utils.KLPVolleySingleton;

public class UserRegistrationActivity extends AppCompatActivity {

    public String LOG_TAG = UserRegistrationActivity.class.getSimpleName();

    //UI references

    private AutoCompleteTextView emailWidget;
    private TextView passwordWidget;
    private TextView verifyPasswordWidget;
    private TextView lastNameWidget, firstNameWidget, phoneNoWidget;
    private ProgressDialog progressDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_registration);

        TextView loginLink = (TextView) findViewById(R.id.backtologin);

        Linkify.addLinks(loginLink, Linkify.ALL);

        if (loginLink != null) {
            loginLink.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(UserRegistrationActivity.this, LoginActivity.class);
                    startActivity(intent);
                }
            });
        }
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//
//
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Button mEmailSignUpButton = (Button) findViewById(R.id.register_button);
        emailWidget = (AutoCompleteTextView) findViewById(R.id.user_email);
        passwordWidget = (TextView) findViewById(R.id.password);
        verifyPasswordWidget = (TextView) findViewById(R.id.verify_password);
        firstNameWidget = (TextView) findViewById(R.id.user_first_name);
        lastNameWidget = (TextView) findViewById(R.id.user_last_name);
        phoneNoWidget = (TextView) findViewById(R.id.user_phone);

        if (mEmailSignUpButton != null) {
            mEmailSignUpButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Add error checking here
                    final String emailValue = emailWidget.getText().toString();
                    final String passwordValue = passwordWidget.getText().toString();
                    String verifyPasswordValue = verifyPasswordWidget.getText().toString();
                    final String firstNameValue = firstNameWidget.getText().toString();
                    final String lastNameValue = lastNameWidget.getText().toString();
                    final String phoneNoValue = phoneNoWidget.getText().toString();

                    View focusView = null;
                    boolean cancel = false;

                    if (TextUtils.isEmpty(emailValue)) {
                        emailWidget.setError("This field is required");
                        focusView = emailWidget;
                        cancel = true;
                    } else if (!isEmailValid(emailValue)) {
                        emailWidget.setError("This email address is invalid");
                        focusView = emailWidget;
                        cancel = true;
                    } else if (TextUtils.isEmpty(passwordValue)) {
                        passwordWidget.setError("This field is required");
                        focusView = passwordWidget;
                        cancel = true;
                    } else if (TextUtils.isEmpty(verifyPasswordValue) || !passwordValue.equals(verifyPasswordValue)) {
                        verifyPasswordWidget.setError("This field is required and must match the value entered in the password field");
                        focusView = verifyPasswordWidget;
                        cancel = true;
                    } else if (TextUtils.isEmpty(firstNameValue)) {
                        firstNameWidget.setError("This field is required");
                        focusView = firstNameWidget;
                        cancel = true;
                    } else if (TextUtils.isEmpty(lastNameValue)) {
                        lastNameWidget.setError("This field is required");
                        focusView = lastNameWidget;
                        cancel = true;
                    } else if (TextUtils.isEmpty(phoneNoValue) || phoneNoValue.length() < 10 || !TextUtils.isDigitsOnly(phoneNoValue)) {
                        phoneNoWidget.setError("Please enter a valid phone number");
                        focusView = phoneNoWidget;
                        cancel = true;
                    }

                    //If no errors, proceed with post to server.
                    if (!cancel) {
                        showProgress(true);
                        final String SIGNUP_URL = BuildConfig.HOST + "/api/v1/users";

                        StringRequest stringRequest = new StringRequest(Request.Method.POST,
                                SIGNUP_URL, new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                showSignupResultDialog(
                                        "Success!",
                                        "You have been successfully signed up. Please Login.",
                                        "Login");
                            }
                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                showSignupResultDialog(
                                        "Error",
                                        "Signup failed - " + error.getMessage(),
                                        "Try Again");

                                clearAllFields();
                                showProgress(false);
                            }
                        }) {
                            @Override
                            protected Map<String, String> getParams() {
                                // set the POST params
                                Map<String, String> params = new HashMap<String, String>();
                                params.put("email", emailValue);
                                params.put("password", passwordValue);
                                params.put("first_name", firstNameValue);
                                params.put("last_name", lastNameValue);
                                params.put("mobile_no", phoneNoValue);
                                return params;
                            }
                            /*
                             * Becuase the server returns the error response as JSON,
                             * need to parse it before showing
                             */
                            @Override
                            protected VolleyError parseNetworkError(VolleyError volleyError){
                                VolleyError error;

                                if(volleyError.networkResponse != null && volleyError.networkResponse.data != null){
                                    try {
                                        JSONObject jsonError = new JSONObject(new String(volleyError.networkResponse.data));
                                        error = new VolleyError(jsonError.getString("detail"));
                                    } catch (JSONException e) {
                                        error = new VolleyError(new String(volleyError.networkResponse.data));
                                    }
                                    volleyError = error;
                                }

                                return volleyError;
                            }
                        };
                        // Add request to the RequestQueue maintained by the Singleton
                        KLPVolleySingleton.getInstance(UserRegistrationActivity.this).addToRequestQueue(stringRequest);
                    } else {
                        //There was an error. Do not attempt sign up. Just show the form field with the error
                        focusView.requestFocus();
                    }
                }
            });
        }
    }

    private void showSignupResultDialog(String title, String message, String buttonText) {
        Bundle signUpResult = new Bundle();
        signUpResult.putString("title", title);
        signUpResult.putString("result", message);
        signUpResult.putString("buttonText", buttonText);

        SignUpResultDialogFragment resultDialog = new SignUpResultDialogFragment();
        resultDialog.setArguments(signUpResult);
        resultDialog.show(getSupportFragmentManager(), "Registration result");
    }

    private boolean isEmailValid(String email) {
        if (TextUtils.isEmpty(email)) {
            return false;
        } else {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private void showProgress(final boolean show) {
        if (show) {
            progressDialog = new ProgressDialog(UserRegistrationActivity.this);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage("Authenticating...");
            progressDialog.show();
        } else {
            if (progressDialog != null) progressDialog.cancel();
        }
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

}

package in.org.klp.kontact;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import in.org.klp.kontact.utils.KLPVolleySingleton;

/**
 * Created by Subha on 7/13/16.
 */
public class ForgotPasswordActivity extends AppCompatActivity{
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgotpassword);
        final EditText userEmail = (EditText) findViewById(R.id.forgot_user_email);
        final Button resetButton = (Button) findViewById(R.id.reset_password_button);
        resetButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = userEmail.getText().toString();
                if (email == null || email.length() == 0) {
                    userEmail.setError("Enter a valid e-mail");
                } else {
                    resetPassword(email);
                }
            }
        });

        final Button backToLogin = (Button) findViewById(R.id.email_sign_in_button);
        backToLogin.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
                startActivity(i);
                finish();
            }
        });
    }

    protected void finishReset(String response)
    {
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        Log.v(ForgotPasswordActivity.class.getSimpleName(), "Finishing reset -- " + response);
        Toast.makeText(ForgotPasswordActivity.this, "Password Reset Email Sent", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    protected void resetPassword(final String email)
    {
        progressDialog = new ProgressDialog(ForgotPasswordActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Sending your reset mail.");
        progressDialog.show();

        String USER_RESET_URL = BuildConfig.HOST  + "/api/v1/password-reset/request";
        StringRequest stringRequest = new StringRequest(Request.Method.POST,
               USER_RESET_URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                if (response != null) finishReset(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error.getMessage() != null) {
                    Log.d(this.toString(), error.getMessage());
                } else {
                    Toast.makeText(ForgotPasswordActivity.this, "Something Went Wrong!", Toast.LENGTH_LONG).show();
                }
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                // set the POST params
                Map<String, String> params = new HashMap<String, String>();
                params.put("email", email);

                return params;
            }
            /*
             * Becuase the server returns the error response as JSON,
             * need to parse it before showing
             */
            @Override
            protected VolleyError parseNetworkError(VolleyError volleyError){
                VolleyError error;
                Log.d(this.toString(), volleyError.toString());
                if(volleyError.networkResponse != null && volleyError.networkResponse.data != null){
                    try {
                        JSONObject jsonError = new JSONObject(new String(volleyError.networkResponse.data));
                        error = new VolleyError(jsonError.getString("success"));
                    } catch (JSONException e) {
                        error = new VolleyError(new String(volleyError.networkResponse.data));
                    }
                    volleyError = error;
                }

                return volleyError;
            }
        };
        // Add request to the RequestQueue maintained by the Singleton
        KLPVolleySingleton.getInstance(ForgotPasswordActivity.this).addToRequestQueue(stringRequest);
    }

}

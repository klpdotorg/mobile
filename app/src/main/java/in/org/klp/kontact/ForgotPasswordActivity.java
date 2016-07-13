package in.org.klp.kontact;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

import in.org.klp.kontact.utils.KLPVolleySingleton;

/**
 * Created by Subha on 7/13/16.
 */
public class ForgotPasswordActivity extends AppCompatActivity{


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgotpassword);
        final AutoCompleteTextView userEmail = (AutoCompleteTextView) findViewById(R.id.forgot_user_email);
        final Button resetButton = (Button) findViewById(R.id.reset_password_button);
        resetButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
               String email = userEmail.getText().toString();
                if(email == null || email.length() == 0)
                    userEmail.setError("Enter a valid e-mail");
                else
                    resetPassword(email);
            }
        });
    }

    protected void finishReset(String response)
    {
        Log.v(ForgotPasswordActivity.class.getSimpleName(), "Finishing reset -- " + response);
        Toast.makeText(ForgotPasswordActivity.this, "Password Reset Email Sent", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }
    protected void resetPassword(final String email)
    {
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
                if (error.getMessage() != null) Log.d(this.toString(), error.getMessage());
                if (error.networkResponse == null) {
                    Toast.makeText(ForgotPasswordActivity.this, "No Internet Connection", Toast.LENGTH_LONG).show();
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

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                // set extra headers
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                return headers;
            }
        };
        // Add request to the RequestQueue maintained by the Singleton
        KLPVolleySingleton.getInstance(this).addToRequestQueue(stringRequest);
    }

}

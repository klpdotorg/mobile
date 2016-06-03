package in.org.klp.kontact.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

/**
 * Created by Subha on 6/3/16.
 */
public class Utils
{
    public static String LOG_TAG = Utils.class.getSimpleName();

    public static RESTResponse parseHttpResponse(HttpURLConnection connection) throws IOException {
        RESTResponse response = new RESTResponse(connection.getResponseCode(), connection.getResponseMessage());
        Log.v(LOG_TAG, "Response code is: " + connection.getResponseCode());
        Log.v(LOG_TAG, "response message is: " + connection.getResponseMessage());
        BufferedReader reader = null;
        InputStream inStream = null;
        InputStream errorStream = null;
        StringBuffer buffer = null;
        try {

            inStream = connection.getInputStream();
            buffer = new StringBuffer();

            //If inputStream is not null, then read it
            if (inStream != null) {
                reader = new BufferedReader(new InputStreamReader(inStream));

                String line;

                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }
                if (buffer.length() > 0)
                    response.setResponseBody(buffer.toString());
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(inStream!=null)
                inStream.close();
            if(reader!=null)
                reader.close();
        }


        try
        {
            //Check the error stream now
            errorStream = connection.getErrorStream();

            if (errorStream != null) {
                StringBuffer errorMessage = new StringBuffer();
                reader = new BufferedReader(new InputStreamReader(errorStream));
                String line;

                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    errorMessage.append(line + "\n");
                    System.out.println(errorMessage.toString());
                }
                if(errorMessage.length() > 0)
                {
                    response.setErrorMessage(errorMessage.toString());
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //Clean everything up here
            if(reader!=null)
                reader.close();

            if(errorStream!=null)
                errorStream.close();
        }
    return response;
    }
}

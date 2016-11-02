package in.org.klp.konnect.utils;

/**
 * Created by Subha on 6/3/16.
 */
public class RESTResponse
{
    private int statusCode = 0;
    private String statusMessage = null;
    private String respBody = null;
    private String errorMessage = null;

    public RESTResponse(int statusCode, String statusMessage)
    {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
    }

    public void setResponseBody(String body)
    {
        this.respBody = body;
    }

    public String getResponseBody()
    {
        return this.respBody;
    }

    public void setErrorMessage(String error)
    {
        this.errorMessage = error;
    }

    public String getErrorMessage()
    {
        return this.errorMessage;
    }

    public int getStatusCode()
    {
        return this.statusCode;
    }

    public String getStatusMessage()
    {
        return this.statusMessage;
    }
}

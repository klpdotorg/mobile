package in.org.klp.konnect.data;

/**
 * Created by deviprasad on 20/6/16.
 */
public class StringWithTags {
    public String string;
    public Object id, parent;
    public boolean type;

    public StringWithTags(String stringPart, Object idpart, Object parentpart) {
        string = stringPart;
        id = idpart;
        parent = parentpart;
        type = false;
    }
    public StringWithTags(String stringPart, Object idpart, Object parentpart, boolean includeid) {
        string = stringPart;
        id = idpart;
        parent = parentpart;
        type = includeid;
    }

    @Override
    public String toString() {
        if (type)
            return String.valueOf(id) + " : " + string;
        else
            return string;
    }
}

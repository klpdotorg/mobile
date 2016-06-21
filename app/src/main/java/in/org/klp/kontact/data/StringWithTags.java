package in.org.klp.kontact.data;

/**
 * Created by deviprasad on 20/6/16.
 */
public class StringWithTags {
    public String string;
    public Object id, parent;

    public StringWithTags(String stringPart, Object idpart, Object parentpart) {
        string = stringPart;
        id = idpart;
        parent = parentpart;
    }

    @Override
    public String toString() {
        return string;
    }
}

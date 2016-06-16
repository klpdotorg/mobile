package in.org.klp.kontact;

/**
 * Created by deviprasad on 28/5/16.
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

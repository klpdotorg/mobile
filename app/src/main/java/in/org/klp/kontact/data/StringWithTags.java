package in.org.klp.kontact.data;

/**
 * Created by deviprasad on 20/6/16.
 */
public class StringWithTags {
    public String string;
    public Object id, parent;
    public int type;

    public StringWithTags(String stringPart, Object idpart, Object parentpart) {
        string = stringPart;
        id = idpart;
        parent = parentpart;
        type = 0;
    }
    public StringWithTags(String stringPart, Object idpart, Object parentpart, int check) {
        string = stringPart;
        id = idpart;
        parent = parentpart;
        type = check;
    }

    @Override
    public String toString() {
        if (type==0)
            return string;
        else
            return String.valueOf(id)+" : "+string;
    }
}

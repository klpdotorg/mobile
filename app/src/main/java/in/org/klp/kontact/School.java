package in.org.klp.kontact;

/**
 * Created by deviprasad on 29/5/16.
 */
public class School {
    private int id,boundary;
    private Object tag_id, tag_boundary;
    private String name;
    public School()
    {
    }
    public School(int id, String name, int boundary)
    {
        this.id=id;
        this.name=name;
        this.boundary=boundary;
    }
    public void setId(int id) {
        this.id = id;
    }
    public void setName(String name) {
        this.name = name;
    }

    public void setBoundary(int boundary) {
        this.boundary = boundary;
    }
    public int getId() {
        return id;
    }
    public int getBoundary() {
        return boundary;
    }
    public String getName() {
        return name;
    }
}

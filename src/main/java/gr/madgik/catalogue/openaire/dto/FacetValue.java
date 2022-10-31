package gr.madgik.catalogue.openaire.dto;

public class FacetValue {
    private String _id;
    private Integer count;

    public FacetValue() {
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
}

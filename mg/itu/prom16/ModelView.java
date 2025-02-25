package mg.itu.prom16;

import java.util.HashMap;

public class ModelView {
    private String viewName;
    private HashMap<String, Object> data;

    public ModelView(String nameView) {
        this.viewName = nameView;
    }

    public HashMap<String, Object> getData() {
        if (this.data == null) {
            data = new HashMap<String, Object>();
        }
        return data;
    }

    public void setData(HashMap<String, Object> list) {
        this.data = list;
    }

    public void addObject(String key, Object value) {
        this.getData().put(key, value);
    }

    public String getViewName() {
        return "/" + viewName;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }
}

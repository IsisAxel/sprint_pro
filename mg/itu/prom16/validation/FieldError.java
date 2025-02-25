package mg.itu.prom16.validation;

public class FieldError {
    String fieldName;
    String message;
    Object value;
    public FieldError() {
    }
    public FieldError(String fieldName, String message, Object value) {
        this.fieldName = fieldName;
        this.message = message;
        this.value = value;
    }
    public String getFieldName() {
        return fieldName;
    }
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public Object getValue() {
        return value;
    }
    public void setValue(Object value) {
        this.value = value;
    }
}



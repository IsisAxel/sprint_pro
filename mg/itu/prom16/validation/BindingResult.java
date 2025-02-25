package mg.itu.prom16.validation;

import java.util.List;

import mg.itu.prom16.ModelView;

public class BindingResult {
    ModelView previousPage;
    List<FieldError> fieldErrors;
    public BindingResult() {
    }
    public BindingResult(ModelView previousPage, List<FieldError> fieldErrors) {
        this.previousPage = previousPage;
        this.fieldErrors = fieldErrors;
    }
    public ModelView getPreviousPage() {
        return previousPage;
    }
    public void setPreviousPage(ModelView previousPage) {
        this.previousPage = previousPage;
    }
    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }
    public void setFieldErrors(List<FieldError> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }

    public boolean containError()
    {
        return !fieldErrors.isEmpty();
    }
}
package mg.itu.prom16;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import mg.itu.prom16.validation.BindingResult;
import mg.itu.prom16.validation.exception.EmailException;
import mg.itu.prom16.validation.exception.MaxException;
import mg.itu.prom16.validation.exception.MinException;
import mg.itu.prom16.validation.exception.NotEmptyException;

public class Mapping {
    Class<?> ControlleClass;    
    HashMap<String,Method> methods;
    public Mapping(Class<?> controlleClass, HashMap<String,Method> methods) {
        ControlleClass = controlleClass;
        this.methods = methods;
    }
    public Class<?> getControlleClass() {
        return ControlleClass;
    }
    public void setControlleClass(Class<?> controlleClass) {
        ControlleClass = controlleClass;
    }
    public HashMap<String,Method> getMethods() {
        return methods;
    }
    public void setMethods(HashMap<String,Method> methods) {
        this.methods = methods;
    }
    
    public String invokeStringMethod(HttpServletRequest req) throws Exception
    {
        String verb = req.getMethod();
        HashMap<String,Method> map = getMethods();
        if (!verb.equals(ClassScanner.getVerb(map.get(verb)))) {
            throw new Exception("Invalid verb for this link");
        }
        return (String)map.get(verb).invoke(this.getControlleClass().getDeclaredConstructor().newInstance());
    }

    public boolean isRestAPI(HttpServletRequest req) throws Exception
    {
        String verb = req.getMethod();
        HashMap<String,Method> map = getMethods();
        return map.get(verb).isAnnotationPresent(RestAPI.class);
    }

    protected static BindingResult containError(Object[] args) {
        for (Object object : args) {
            if (object instanceof BindingResult) {
                BindingResult br = (BindingResult) object;
                if (br.containError()) {
                    return br;
                }
            }
        }
        return null;
    }

    public Object invoke(HttpServletRequest request , Map<String,Mapping> controllerList) throws Exception
    {
        BindingResult br = null;
        try {
            String verb = request.getMethod();
            HashMap<String,Method> map = getMethods();
            Method method =  map.get(verb);
            if (!verb.equals(ClassScanner.getVerb(method))) {
                throw new Exception("Invalid verb for this link");
            }
            Object ob = getControlleClass().getDeclaredConstructor().newInstance();
            Map<String,String> params = ServletUtil.extractParameters(request);
            Object[] args = ServletUtil.getMethodArguments(request , method, params);
            br = containError(args);
            ServletUtil.processSession(ob, request);
            if (br != null) {
                ServletUtil.validationErrorRedirect(request, method , br, controllerList);
            }
            ServletUtil.isAuthorized(method, request);
            return method.invoke(ob, args);
        } catch (Exception e) {
            throw e;
        }
    }
}

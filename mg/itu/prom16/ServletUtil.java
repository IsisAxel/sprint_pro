package mg.itu.prom16;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import mg.itu.prom16.validation.annotation.Max;
import mg.itu.prom16.validation.annotation.Min;
import mg.itu.prom16.validation.annotation.NotEmpty;
import mg.itu.prom16.validation.annotation.Validate;
import mg.itu.prom16.authorization.annotation.AuthorizedRoles;
import mg.itu.prom16.authorization.annotation.LoginRequired;
import mg.itu.prom16.authorization.exception.UnauthorizedException;
import mg.itu.prom16.validation.BindingResult;
import mg.itu.prom16.validation.FieldError;
import mg.itu.prom16.validation.annotation.Email;
import mg.itu.prom16.validation.annotation.ErrorUrl;
import mg.itu.prom16.validation.exception.*;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.security.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public abstract class ServletUtil {
    public static Map<String, String> extractParameters(HttpServletRequest request) {
        Map<String, String> parameters = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values.length > 0) {
                parameters.put(key, values[0]);
            }
        });
        return parameters;
    }

    public static Object[] getMethodArguments(HttpServletRequest request, Method method, Map<String, String> params) throws Exception {
        Parameter[] parameters = method.getParameters();
        Object[] arguments = new Object[parameters.length];
        List<FieldError> errors = new ArrayList<>();
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            ReqParam reqParam = parameter.getAnnotation(ReqParam.class);
            ReqBody reqBody = parameter.getAnnotation(ReqBody.class);
            ReqFile reqFile = parameter.getAnnotation(ReqFile.class);
        
            if (reqBody != null) {
                // Si le paramètre est une List, on crée une instance d'ArrayList
                if (List.class.isAssignableFrom(parameter.getType())) {
                    ParameterizedType genericType = (ParameterizedType) parameter.getParameterizedType();
                    Class<?> listElementClass = (Class<?>) genericType.getActualTypeArguments()[0];
                    List<Object> list = new ArrayList<>();
                    String listPrefix = parameter.getName(); // Par exemple "passagers"
                    int index = 0;
                    while (true) {
                        String currentPrefix = listPrefix + "[" + index + "]";
                        boolean found;
                        Object element = null;
                        if (isPrimitiveOrWrapper(listElementClass)) {
                            // Pour les types simples, on cherche une clé égale à currentPrefix
                            if (params.containsKey(currentPrefix)) {
                                String paramValue = params.get(currentPrefix);
                                element = TypeConverter.convert(paramValue, listElementClass);
                                found = true;
                            } else {
                                found = false;
                            }
                        } else {
                            // Pour les objets complexes, on cherche une clé qui commence par currentPrefix + "."
                            found = params.keySet().stream().anyMatch(key -> key.startsWith(currentPrefix + "."));
                            if (found) {
                                element = listElementClass.getDeclaredConstructor().newInstance();
                                populateObjectFields(element, currentPrefix, params, errors);
                            }
                        }
                        if (!found) break;
                        list.add(element);
                        index++;
                    }
                    arguments[i] = list;
                } else {
                    // Paramètre non-listé (objet classique)
                    Constructor<?> constructor = parameter.getType().getDeclaredConstructor();
                    Object obj = constructor.newInstance();
                    populateObjectFields(obj, parameter.getName(), params, errors);
                    arguments[i] = obj;
                }

            } else if (reqFile != null) {
                setMultipartFile(parameter, request, arguments, i);
            } else {
                if (parameter.getType().equals(BindingResult.class)) {
                    BindingResult bindingResult = new BindingResult();
                    bindingResult.setFieldErrors(errors);
                    arguments[i] = bindingResult;
                } else {
                    String paramName = (reqParam != null && !reqParam.value().isEmpty()) ? reqParam.value() : parameter.getName();
                    String paramValue = params.get(paramName);
        
                    if (paramValue != null) {
                        arguments[i] = TypeConverter.convert(paramValue, parameter.getType());
                    } else {
                        arguments[i] = isBooleanType(parameter) ? false : null;
                    }
                }
            }
        }
        return arguments;
    }

    private static void populateObjectFields(Object obj, String parentName, Map<String, String> params, List<FieldError> errors) throws Exception {
        for (Field field : obj.getClass().getDeclaredFields()) {
            String fieldName = parentName.isEmpty() ? field.getName() : parentName + "." + field.getName();
            field.setAccessible(true);

            // Traitement pour les champs de type List<T>
            if (List.class.isAssignableFrom(field.getType())) {
                // Récupérer le type générique de la liste
                ParameterizedType genericType = (ParameterizedType) field.getGenericType();
                Class<?> listElementClass = (Class<?>) genericType.getActualTypeArguments()[0];
                
                List<Object> list = new ArrayList<>();
                int index = 0;
                while (true) {
                    String currentPrefix = fieldName + "[" + index + "]";
                    boolean found;
                    Object element = null;
                    if (isPrimitiveOrWrapper(listElementClass)) {
                        // Pour une liste d'éléments simples, la clé doit être exactement currentPrefix
                        if (params.containsKey(currentPrefix)) {
                            String paramValue = params.get(currentPrefix);
                            element = TypeConverter.convert(paramValue, listElementClass);
                            found = true;
                        } else {
                            found = false;
                        }
                    } else {
                        // Pour une liste d'objets, on cherche des clés commençant par currentPrefix + "."
                        found = params.keySet().stream().anyMatch(key -> key.startsWith(currentPrefix + "."));
                        if (found) {
                            element = listElementClass.getDeclaredConstructor().newInstance();
                            populateObjectFields(element, currentPrefix, params, errors);
                        }
                    }
                    if (!found) break;
                    list.add(element);
                    index++;
                }
                if (!list.isEmpty()) {
                    field.set(obj, list);
                }
            } else {
                // Traitement pour les champs simples ou objets imbriqués
                String paramValue = params.get(fieldName);
        
                if (paramValue != null) {
                    Object value = TypeConverter.convert(paramValue, field.getType());
                    field.set(obj, value);
                    if (field.isAnnotationPresent(Validate.class)) {
                        checkValidation(value, field, errors);
                    }
                } else if (!isPrimitiveOrWrapper(field.getType())) { 
                    // Instanciation récursive d'un objet imbriqué uniquement s'il existe des paramètres commençant par "fieldName."
                    boolean foundNested = params.keySet().stream().anyMatch(key -> key.startsWith(fieldName + "."));
                    if (foundNested) {
                        Constructor<?> constructor = field.getType().getDeclaredConstructor();
                        Object nestedObj = constructor.newInstance();
                        populateObjectFields(nestedObj, fieldName, params, errors);
                        field.set(obj, nestedObj);
                    }
                }
            }
        }
    }

    private static boolean isPrimitiveOrWrapper(Class<?> type) {
        return type.isPrimitive() || 
            type.equals(String.class) || 
            type.equals(Integer.class) || 
            type.equals(Double.class) || 
            type.equals(Long.class) || 
            type.equals(Boolean.class) || 
            type.equals(Float.class) || 
            type.equals(Short.class) || 
            type.equals(Byte.class) ||  
            type.equals(Character.class) ||
            type.getName().startsWith("java.") || 
            type.getName().startsWith("javax.") ||
            type.getName().startsWith("sun.") ||
            type.getName().startsWith("jdk.") ||
            type.getName().startsWith("org.w3c.") ||
            type.getName().startsWith("org.xml.");
    }   

    public static void checkValidation(Object value , Field field , List<FieldError> errors) throws Exception
    {
        if (field.isAnnotationPresent(NotEmpty.class)) {
            if (value instanceof String) {
                String stringValue = (String) value;
                NotEmpty notEmpty = field.getAnnotation(NotEmpty.class);
                if (stringValue.trim().isEmpty()) {
                    String message = notEmpty.message();
                    String mess = "Le champ " + field.getName() + " ne doit pas etre vide";
                    FieldError error = new FieldError(field.getName(), message, value);
                    errors.add(error);
                    if (message.trim().isEmpty()) {
                        error.setMessage(mess);
                    }
                }
            } else {
                String mess = "Le champ " + field.getName() + " doit etre une chaine de caractere" ;
                FieldError error = new FieldError(field.getName(), mess, value);
                errors.add(error);
            }
        }
        if (field.isAnnotationPresent(Min.class)) {
            Min min = field.getAnnotation(Min.class);
            String message = min.message();
            if (value instanceof Number) {
                double d = ((Number) value).doubleValue();
                if (d < min.value()) {
                    String mess = "Le champ " + field.getName() + " doit etre superieur a "+min.value();
                    FieldError error = new FieldError(field.getName(), message, value);
                    errors.add(error);
                    if (message.trim().isEmpty()) {
                        error.setMessage(mess);
                    }
                }
            } else {
                String mess = "Le champ " + field.getName() + " doit etre un nombre ";
                FieldError error = new FieldError(field.getName(), mess, value);
                errors.add(error);
            }
        }
        if (field.isAnnotationPresent(Max.class)) {
            Max max = field.getAnnotation(Max.class);
            String message = max.message();
            if (value instanceof Number) {
                double d = ((Number) value).doubleValue();
                if (d > max.value()) {
                    String mess = "Le champ " + field.getName() + " doit etre inferieur a "+max.value();
                    FieldError error = new FieldError(field.getName(), message, value);
                    errors.add(error);
                    if (message.trim().isEmpty()) {
                        error.setMessage(mess);
                    }
                }
            } else {
                String mess ="Le champ " + field.getName() + " doit etre un nombre ";
                FieldError error = new FieldError(field.getName(), mess, value);
                errors.add(error);
            }
        } 
        if (field.isAnnotationPresent(Email.class)) {
            Email email = field.getAnnotation(Email.class);
            String message = email.message();
            if (value instanceof String) {
                boolean emailValid = isValidEmail(value.toString());
                if (!emailValid) {
                    String mess = "Le champ " + field.getName() + " doit etre un email valide";
                    FieldError error = new FieldError(field.getName(), message, value);
                    errors.add(error);
                    if (message.trim().isEmpty()) {
                        error.setMessage(mess);
                    }
                }
            } else {
                String mess = "Le champ " + field.getName() + " doit etre une chaine de caractere";
                FieldError error = new FieldError(field.getName(), mess, value);
                errors.add(error);
            }
        }
    }

    public static boolean isValidEmail(String email) {
        return Pattern.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", email);
    }

    public static void processSession(Object obj, HttpServletRequest request) throws Exception {
        Class<?> clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            if (field.getType().equals(MySession.class)) {
                field.setAccessible(true);
                Object sessionInstance = field.get(obj);
                if (sessionInstance == null) {
                    sessionInstance = MySession.class.getDeclaredConstructor().newInstance();
                    field.set(obj, sessionInstance);
                }

                MySession session = (MySession) sessionInstance;
                session.setSession(request.getSession());
                break;
            }
        }
    }

    private static boolean isBooleanType(Parameter parameter) {
        return parameter.getType().equals(boolean.class) || parameter.getType().equals(Boolean.class);
    }

    private static void setMultipartFile(Parameter argParameter, HttpServletRequest request, Object[] values , int index) throws Exception {
        ReqFile reqFile = (ReqFile)argParameter.getAnnotation(ReqFile.class);
        String nameFile = "";
        if (reqFile != null && !reqFile.value().isEmpty()) {
            nameFile = reqFile.value();
        } else {
            nameFile = argParameter.getName();
        }
        int i = 0;
        Part part = request.getPart(nameFile);
        if (part == null) {
            values[i] = null;
        } else if (argParameter.getType().isAssignableFrom(MultiPartFile.class)) {
            Class class1 = argParameter.getType();
            Constructor constructor = class1.getDeclaredConstructor();
            Object o = constructor.newInstance();
            MultiPartFile mlprt = (MultiPartFile)o;
            mlprt.buildInstance(part, "1859");
            values[i] = mlprt;
        } else {
            throw new Exception("Parameter not valid Exception for File!");
        }
    }

    public  static void validationErrorRedirect(HttpServletRequest request , Method method , BindingResult br , Map<String,Mapping> controllerList) throws Exception
    {
        if (method.isAnnotationPresent(ErrorUrl.class)) {
            ErrorUrl errorHandlerUrl = method.getAnnotation(ErrorUrl.class);
            String handlerUrlPage = errorHandlerUrl.url();

            request = new HttpServletRequestWrapper(request) {
                @Override
                public String getMethod() {
                    return "GET";
                }
            };

            Mapping mapping =  controllerList.get(handlerUrlPage);
            Object val = mapping.invoke(request , controllerList);

            if (val instanceof ModelView) {
                br.setPreviousPage((ModelView) val);  
            } else {
                throw new Exception("La page a retourner doit retourne une valeur de type ModelView");
            }
        } else {
            throw new Exception("Annotation ErrorUrl not found on the method :" + method.getName());
        }
    }

    public static boolean isAuthenticated(HttpSession session) {
        Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
        return Boolean.TRUE.equals(isAuthenticated);
    }

    public static boolean hasRoles(HttpSession session, String[] requiredRoles) {
        String userRole = (String) session.getAttribute("userRole");
        if (userRole == null) {
            return false;
        }
        userRole = userRole.toUpperCase();

        for (String role : requiredRoles) {
            if (userRole.equals(role)) {
                return true;
            }
        }
        return false;
    }

    public static void isAuthorized(Method method, HttpServletRequest request) throws Exception {        
        HttpSession session = request.getSession();
        // if (method.isAnnotationPresent(LoginRequired.class) 
        //     && method.isAnnotationPresent(AuthorizedRoles.class)) {
        //     throw new IllegalArgumentException("Les deux annotations ne peuvent pas presentes en meme dans la methode "+ method.getName());
        // }
        if (method.isAnnotationPresent(LoginRequired.class)) {
            if (!isAuthenticated(session)) {
                throw new UnauthorizedException("Access denied, Login required");
            }
        } else if (method.isAnnotationPresent(AuthorizedRoles.class)) {
            if (!isAuthenticated(session)) {
                throw new UnauthorizedException("Access denied, Login required !");
            }
            String[] roles = method.getAnnotation(AuthorizedRoles.class).roles();
            if (!hasRoles(session, roles)) {
                throw new UnauthorizedException("Access denied, required roles: " + String.join(", ", roles) + " !!");
            }
        }
    }
}

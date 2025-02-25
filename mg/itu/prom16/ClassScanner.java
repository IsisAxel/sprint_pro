package mg.itu.prom16;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.HashMap;

import java.lang.reflect.Method;

import mg.itu.prom16.Mapping;
public class ClassScanner {

    public static Map<String,Mapping> getMapping(String packageName,Class<? extends Annotation> annotationClass) throws Exception{
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String packagePath = packageName.replace(".", "/");
        Enumeration<URL> resources = classLoader.getResources(packagePath);
    
        Map<String,Mapping> map=new HashMap<String,Mapping>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if (resource.getProtocol().equals("file")) {
                File directory = new File(resource.getPath());
                for (File file : directory.listFiles()) {
                    if (file.isFile() && file.getName().endsWith(".class")) {
                        String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                        Class<?> class1=Class.forName(className);
                        if (class1.isAnnotationPresent(annotationClass)) {
                            map.putAll(getMethods(class1,map));                            
                        }
                    }
                }
            }
        }
        return map;
    }    
    private static Map<String,Mapping> getMethods(Class<?> clazz , Map<String,Mapping> allMapping) throws Exception{
        
        Map<String,Mapping> methods = new HashMap<String,Mapping>();
        for (Method method : clazz.getDeclaredMethods()) {
            mg.itu.prom16.URL an= method.getAnnotation(mg.itu.prom16.URL.class);
            String link = an.url();
            if(an!=null && !link.isEmpty()){
                String verb = getVerb(method);

                if (allMapping.containsKey(link)) {
                    if (allMapping.get(link).getMethods().containsKey(verb)) {
                        throw new Exception("Duplicate url for verb = "+verb+" and link = "+link);         
                    }
                }

                if(methods.containsKey(link)){
                    Mapping map = methods.get(link);
                    HashMap<String, Method> apiRequests = map.getMethods();
  
                    if (!apiRequests.containsKey(verb) && !allMapping.get(link).getMethods().containsKey(verb)) {
                       apiRequests.put(verb, method);
                       methods.put(link, map);
                    }
                    else {
                        throw new Exception("Duplicate url for verb = "+verb+" and link = "+link);
                    }
                } else {
                    HashMap<String, Method> apiRequests = new HashMap<String, Method>();
                    apiRequests.put(verb, method);
                    Mapping mapping = new Mapping(clazz, apiRequests);
  
                    methods.put(link, mapping);
                }
            }
        }
        return methods;
    }

    public static String getVerb(Method m)
    {
        if (m.isAnnotationPresent(Post.class)) {
            return "POST";
        }
        return "GET";
    }
    
}

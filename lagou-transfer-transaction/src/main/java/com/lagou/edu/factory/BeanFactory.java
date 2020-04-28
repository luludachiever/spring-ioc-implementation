package com.lagou.edu.factory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import org.springframework.stereotype.Service;
import org.springframework.web.context.support.StandardServletEnvironment;

public class BeanFactory {
    private static Map<String, Object> map = new HashMap<>();
    private static Map<String, String> h = new HashMap<>();
    private static Map<String, Set<String>> g = new HashMap<>(); //B is A field, A is key
    private static Map<String, Set<String>> depends = new HashMap<>();
    private static Set<String> keys = new HashSet<>();
    private Iterator<Class<?>> itr;

    public BeanFactory() throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException {
        findAnotations();
    }

    //find service and repo
    void instantiateClasses(Set<Class<?>> classes) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        itr = classes.iterator();
        while (itr.hasNext()) {
            Class<?> cur = itr.next();
            Annotation[] cAnno = (cur.getAnnotations());
            String className = cur.getName();
            for (int i = 0; i < cAnno.length; ++i) {
                String[] types = cAnno[i].toString().split("\\(");
                String key = types[0];
                String[] val = types[1].split("\\=");
                String classN = val[1].replaceAll("[^A-Za-z]+", "");
                classN = classN.substring(0, 1).toUpperCase() + classN.substring(1);
                Class<?> bClass = Class.forName(className);
                if (key.equals("@org.springframework.stereotype.Service") || key.equals("@org.springframework.stereotype.Repository")) {
                    if (bClass.isInterface() == false) {
                        Object o = bClass.newInstance();
                        map.put(classN, o);
                        h.put(o.toString().split("@")[0], classN);
                    }
                }
            }
        }
    }

    void CreateGraph() throws InvocationTargetException, IllegalAccessException {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object v = entry.getValue();
            String key = entry.getKey();
            Field[] fields = v.getClass().getDeclaredFields();
            for (int i = 0; i < fields.length; ++i) {
                Annotation[] anno = fields[i].getDeclaredAnnotations();
                if (anno.length == 0) {
                    continue;
                }
                String[] FaString = anno[i].toString().split("\\(");
                String annoF = FaString[0];
                if (annoF.equals("@org.springframework.beans.factory.annotation.Autowired")) {
                    String type = fields[i].toString().split(" ")[1];
                    String[] type1 = type.split("\\.");
                    String t = type1[type1.length - 1];
                    if (map.get(type) != null) {
                        t = type;
                    }

                        if (g.get(t) == null) {
                            Set<String> s = new HashSet<>();
                            g.put(t, s);
                        }
                        g.get(t).add(key);
                        keys.add(t);
                        if (depends.get(key) == null) {
                            Set<String> s = new HashSet<>();
                            depends.put(key, s);
                        }
                        depends.get(key).add(t);
                    }
                }
            }
        }

    void solveDependency() throws InvocationTargetException, IllegalAccessException {

           Set<String> copy = new HashSet<>();
           copy.addAll(keys);
            for (String k : keys) {
                for (Map.Entry<String, Set<String>> entry : g.entrySet()) {
                     String kk = entry.getKey();
                     Set<String> val = entry.getValue();
                    if (val.contains(k) == true && copy.contains(k) == true) {
                        copy.remove(k);
                        break;
                    }
                }
            }
            Queue<String> q = new LinkedList<>();
            for (String k : copy) {
                q.add(k);
            }
            Set<String> visited = new HashSet<>();
            while (q.size() > 0) {
                String key = q.poll();
                if (visited.contains(key)) {
                    continue;
                }
                visited.add(key);
                Set<String> vals = g.get(key);
                if (vals == null) {
                    continue;
                }
                for (String v : vals) {
                    depends.get(v).remove(key);
                    if (depends.get(v).size() == 0) {
                        Object h = map.get(v);
                        Method[] methods = h.getClass().getMethods();
                        for (int l = 0; l < methods.length; l++) {
                            Method method = methods[l];
                            if(method.getName().equalsIgnoreCase("set" +  key)) {
                                method.invoke(map.get(v), map.get(key));
                                q.add(v);
                            }
                        }
                    }
                }

            }
        }

        void solveTrans(Set<Class<?>> classes) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
            itr = classes.iterator();
            while(itr.hasNext()) {
                //System.out.println(itr.next());
                Class<?> cur = itr.next();
                Annotation[] cAnno = (cur.getAnnotations());
                String className = cur.getName();
                for (int i = 0; i < cAnno.length; ++i) {
                    String[] types = cAnno[i].toString().split("\\(");
                    String key = types[0];
                    String[] val = types[1].split("\\=");
                    Class<?> bClass = Class.forName(className);
                    if (key.equals("@org.springframework.transaction.annotation.Transactional")) {
                        ProxyFactory proxyFactory = (ProxyFactory)map.get("ProxyFactory");
                        Object k = null;
                        if (h.get(className) != null) {
                            Object t = map.get(h.get(className));
                            k = proxyFactory.getJdkProxy(t);
                        } else {
                            k = proxyFactory.getJdkProxy(bClass.newInstance());
                        }
                        map.put(h.get(className), k);
                    }
                }
            }
        }

    public Object  getObject(String type) {
        return map.get(type);
    }
       /*     for (String k : keys) {
                Set<String> vals = g.get(k);
                for (String v : vals) {
                     depends.get(v).remove(k);
                     if (depends.get(v).size() == 0) {
                         Object h = map.get(v);
                         Method[] methods = v.getClass().getMethods();
                         for (int l = 0; l < methods.length; l++) {
                             Method method = methods[l];
                             if(method.getName().equalsIgnoreCase("set" +  k) {
                                 method.invoke(map.get(v), map.get(k));
                             }
                         }
                     }
                }
            }
        }


        if (solved) {
            String type = fields[i].toString().split(" ")[1];
            String[] type1 = type.split("\\.");
            Method[] methods = v.getClass().getMethods();
            for (int l = 0; l < methods.length; l++) {
                Method method = methods[l];
                if(method.getName().equalsIgnoreCase("set" + type1[type1.length - 1])) {
                    // 该方法就是 setAccountDao(AccountDao accountDao)
                    if (map.get(type) != null) {
                        method.invoke(map.get(h.get(v.getClass().toString().split(" ")[1])), map.get(type));
                    } else if (map.get(type1[type1.length - 1]) !=  null) {
                        method.invoke(map.get(h.get(v.getClass().toString().split(" ")[1])), map.get(type1[type1.length - 1]));
                    }
                }
            }
            solved.add();
        }
    }*/

    /*void findAutowire() {
        for (Map.Entry<String, Object> entry :map.entrySet()) {
             Object v = entry.getValue();
             solveDependency(v);
        }
    }*/


    public void findAnotations() throws ClassNotFoundException, IllegalAccessException, InstantiationException, InvocationTargetException {


        Reflections ref = new Reflections(new ConfigurationBuilder().
                setScanners(new SubTypesScanner(false /* don't exclude Object.class */),
                        new ResourcesScanner()).setUrls(ClasspathHelper.forPackage("com.lagou.edu")).
                filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix("com.lagou.edu"))));
        Set<Class<?>> classes = ref.getSubTypesOf(Object.class);
        // initialize all the classes
        instantiateClasses(classes);
        CreateGraph();
        solveDependency();
        solveTrans(classes);
    }
};
        /*
        // find all fields with autowire in class.
        itr = classes.iterator();
        while(itr.hasNext()) {
            Class<?> cur = itr.next();
            Annotation[] cAnno = (cur.getAnnotations());
            String className = cur.getName();
            Field[] fields = cur.getDeclaredFields();
            for (int i = 0; i < fields.length; ++i) {
                Annotation[] Fanno = fields[i].getDeclaredAnnotations();
                for (int j = 0; j < Fanno.length; ++j) {
                    String[] FaString = Fanno[j].toString().split("\\(");
                    String anno = FaString[0];
                    if (anno.equals("@org.springframework.beans.factory.annotation.Autowired")) {
                        String type = fields[i].toString().split(" ")[1];

                         Class<?> aClass = Class.forName(type);
                        Method[] methods = cur.getMethods();
                        String[] type1 = type.split("\\.");
                        for (int l = 0; l < methods.length; l++) {
                            Method method = methods[l];
                            if(method.getName().equalsIgnoreCase("set" + type1[type1.length - 1])) {
                              // 该方法就是 setAccountDao(AccountDao accountDao)
                                if (map.get(type) != null) {
                                    method.invoke(map.get(h.get(cur.toString().split(" ")[1])), map.get(type));
                                } else if (map.get(type1[type1.length - 1]) !=  null) {
                                    System.out.println("set");
                                    System.out.println(map.get(h.get(cur.toString().split(" ")[1])));
                                    System.out.println(map.get(type1[type1.length - 1]));
                                    method.invoke(map.get(h.get(cur.toString().split(" ")[1])), map.get(type1[type1.length - 1]));
                                }
                            }
                        }
                    }

                }
            }
        }

        itr = classes.iterator();
        while(itr.hasNext()) {
            //System.out.println(itr.next());
            Class<?> cur = itr.next();
            Annotation[] cAnno = (cur.getAnnotations());
            String className = cur.getName();
            for (int i = 0; i < cAnno.length; ++i) {
                String[] types = cAnno[i].toString().split("\\(");
                String key = types[0];
                String[] val = types[1].split("\\=");
                Class<?> bClass = Class.forName(className);
                if (key.equals("@org.springframework.transaction.annotation.Transactional")) {
                    ProxyFactory proxyFactory = (ProxyFactory)map.get("ProxyFactory");
                    Object k = null;
                    if (h.get(className) != null) {
                        Object t = map.get(h.get(className));
                        k = proxyFactory.getCglibProxy(t);
                    } else {
                        k = proxyFactory.getCglibProxy(bClass.newInstance());
                    }
                    map.put(h.get(className), k);
                }
            }
        }

        for (Map.Entry<String, Object> entry :map.entrySet()) {
            System.out.println(entry.getKey() + ":" + entry.getValue().toString());
        }
        for (Map.Entry<String,String> entry : h.entrySet()) {
            System.out.println(entry.getKey() + ":" + entry.getValue().toString());
        }
    }

    public Object  getObject(String type) {
           return map.get(type);
    }
}
*/
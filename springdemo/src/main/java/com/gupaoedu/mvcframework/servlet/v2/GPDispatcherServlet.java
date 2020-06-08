package com.gupaoedu.mvcframework.servlet.v2;

import com.gupaoedu.mvcframework.annotation.*;
import com.sun.tools.javac.code.Attribute;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * @author bobstorm
 * @date 2020/6/6 15:12
 */
public class GPDispatcherServlet extends HttpServlet {

    private Map<String, Object> ioc = new HashMap<String, Object>();

    private Properties contexConfig = new Properties();

    private List<String> classNames = new ArrayList<String>();

    private Map<String, Method> handlerMapping = new HashMap<String, Method>();

    public GPDispatcherServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //7.调用
        try {
            doDispatch(req, resp);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPost(req, resp);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        //1.加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        //2.初始化IOC容器

        //3.扫描相关的类
        doScanner(contexConfig.getProperty("scanPackage"));

        //4.实例化扫描到的类，缓存到ioc容器中
        doInstance();

        //5.完成依赖注入
        doAutowired();

        //6.初始化HandlerMapping
        doInitHandlerMapping();

        //==============初始化阶段完成
        System.out.println("GP Spring Framework is initialized");

    }

    private void doInitHandlerMapping() {
        if(ioc.isEmpty()){ return;}

        for (Map.Entry<String, Object> entry: ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            String baseUrl = "";
            if(clazz.isAnnotationPresent(GPRequestMapping.class)){
                baseUrl = clazz.getAnnotation(GPRequestMapping.class).value();
            }

            for(Method method: clazz.getMethods()){
                if(!method.isAnnotationPresent(GPRequestMapping.class)){continue;}
                GPRequestMapping requestMapping = method.getAnnotation(GPRequestMapping.class);

                String url = ("/" + baseUrl + "/" + requestMapping.value()).replace("/+","/");
                handlerMapping.put(url,method);

                System.out.println("Mapped: " + url + "on " + method);
            }
        }
    }

    private void doAutowired() {
        if(ioc.isEmpty()) return;;
        for(Map.Entry<String, Object> entry : ioc.entrySet()){
            //拿到的字段包括 private/public/protected/default
            Field[] fields = entry.getValue().getClass().getDeclaredFields();

            for(Field field : fields){
                if(!field.isAnnotationPresent(GPAutowired.class)){continue;}
                GPAutowired autowired = field.getAnnotation(GPAutowired.class);
                String beanName = autowired.value().trim();
                if("".equals(beanName)){
                    beanName = field.getType().getName();
                }

                //强制暴力访问
                field.setAccessible(true);

                try {
                    //field 相当于
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    private void doInstance() {
        if(classNames.isEmpty()) {return;}
        try{
            for(String className : classNames){
                Class<?> clazz = Class.forName(className);
                //1.默认id:类名首字母小写
                String beanName = toLowerFirstCase(clazz.getSimpleName());
                Object instance = clazz.newInstance();;
                if(clazz.isAnnotationPresent(GPController.class)){
                    instance = clazz.newInstance();
                    ioc.put(beanName, instance);
                }else if(clazz.isAnnotationPresent(GPService.class)){

                    //2.不同包下重名类，自定义beanName
                    GPService service = clazz.getAnnotation(GPService.class);
                    if(!"".equals(service.value())){
                        beanName = service.value();
                    }
                    ioc.put(beanName,instance);

                    //3.全类名（类型的全类名）
                    for(Class<?> s : clazz.getInterfaces()){
                        // 一个借口有多个实现类
                        if(ioc.containsKey(s.getName())){
                            throw  new Exception("The beanName already exists");
                        }
                        ioc.put(s.getName(),instance);
                    }
                }
            }
        }catch ( Exception ignored){

        }

    }

    private String toLowerFirstCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        chars[0]  += 32;
        return String.valueOf(chars);
    }

    private void doScanner(String scanPackage) {
        URL url = this.getClass().getClassLoader()
                .getResource("/" + scanPackage.replaceAll("\\.","/"));
        File classPath = new File(url.getFile());

        for (File file : classPath.listFiles()){

            //如果是文件夹，就递归
            if(file.isDirectory()){
                doScanner(scanPackage + "." + file.getName());
            }else{
                //取反，减少代码嵌套
                if(!file.getName().endsWith(".class")){
                    continue;
                }

                //拿到全类名，包名.类名
                String className = scanPackage + "." + file.getName().replace(".class","");
                classNames.add(className);
            }

        }
    }

    private void doLoadConfig(String contextConfigLocation) {

        //classpath下去找到对应的配置文件同时读取出来，存放到内存中
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            contexConfig.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(null != is){
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }



    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException, InvocationTargetException, IllegalAccessException {
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath,"").replaceAll("/+","/");

        if(!this.handlerMapping.containsKey(url)){
            resp.getWriter().write("404 Not Found");
            return;
        }
        Map<String, String[]> params = req.getParameterMap();
        Method method = this.handlerMapping.get(url);
        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
        //硬编码
//        method.invoke(ioc.get(beanName),new Object[]{req,resp,params.get("name")[0]});

        //方法两类：形参
        Class<?>[] paramsTypes =  method.getParameterTypes();
        //，实参
        Object[] paramsValues = new Object[paramsTypes.length];
        //给实参列表赋值
        for(int i = 0; i < paramsTypes.length; i++){
            Class<?> parameterType = paramsTypes[i];
            if(parameterType == HttpServletRequest.class){
                paramsValues[i] = req;
            }else if(parameterType == HttpServletResponse.class){
                paramsValues[i] = resp;
            }else if(parameterType == String.class){
               Annotation[][] annotations =  method.getParameterAnnotations();
               for(Annotation annotations1: annotations[i]){
                   if(annotations1 instanceof GPRequestParam){
                       String paramName = ((GPRequestParam) annotations1).value();
                       if("".equals(paramName.trim())){
                           String value = Arrays.toString(params.get(paramName)).replaceAll("\\[|\\]","")
                                   .replaceAll("\\s","");
                           paramsValues[i] = value;
                       }
                   }
               }
            }
        }
        //软编码
        method.invoke(ioc.get(beanName),paramsValues);
    }
}

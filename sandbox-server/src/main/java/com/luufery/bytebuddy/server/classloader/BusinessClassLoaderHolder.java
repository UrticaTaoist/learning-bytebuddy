package com.luufery.bytebuddy.server.classloader;

/**
 * @author zhuangpeng
 * @since 2020/1/15
 */
public class BusinessClassLoaderHolder {

    private static final ThreadLocal<DelegateBizClassLoader> holder = new ThreadLocal<DelegateBizClassLoader>();

    public static void setBussinessClassLoader(ClassLoader classLoader){
        if(null == classLoader){
            return;
        }
        System.out.println("这里,正在设置BussinessClassLoader::"+classLoader.getClass().getName());
        DelegateBizClassLoader delegateBizClassLoader = new DelegateBizClassLoader(classLoader);
        holder.set(delegateBizClassLoader);
    }


    public static void removeBussinessClassLoader(){
        System.out.println("这里在删除BussinessClassLoader");
        holder.remove();
    }

    public static DelegateBizClassLoader getBussinessClassLoader(){

        return holder.get();
    }

    public static class DelegateBizClassLoader extends ClassLoader{
        public DelegateBizClassLoader(ClassLoader parent){
            super(parent);
        }

        @Override
        public Class<?> loadClass(final String javaClassName, final boolean resolve) throws ClassNotFoundException {
            return super.loadClass(javaClassName,resolve);
        }
    }
}

package com.lagou.edu.factory;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import com.lagou.edu.utils.TransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository("ProxyFactory")
public class ProxyFactory {
    @Autowired
    private TransactionManager transactionManager;
    public void setTransactionManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }
        /**
         * Jdk动态代理
         * @param obj  委托对象
         * @return   代理对象
         */

        public Object getJdkProxy(Object obj) {

            // 获取代理对象
            return  Proxy.newProxyInstance(obj.getClass().getClassLoader(), obj.getClass().getInterfaces(),
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            Object result = null;

                            try{
                                // 开启事务(关闭事务的自动提交)
                                transactionManager.beginTransaction();

                                result = method.invoke(obj,args);

                                // 提交事务

                                transactionManager.commit();
                            }catch (Exception e) {
                                e.printStackTrace();
                                // 回滚事务
                                transactionManager.rollback();

                                // 抛出异常便于上层servlet捕获
                                throw e;

                            }

                            return result;
                        }
                    });

        }
    public Object getCglibProxy(Object obj) {
        return  Enhancer.create(obj.getClass(), new MethodInterceptor() {
            @Override
            public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
                Object result = null;
                try{
                    // 开启事务(关闭事务的自动提交)
                    transactionManager.beginTransaction();

                    result = method.invoke(obj,objects);

                    // 提交事务

                    transactionManager.commit();
                }catch (Exception e) {
                    e.printStackTrace();
                    // 回滚事务
                    transactionManager.rollback();

                    // 抛出异常便于上层servlet捕获
                    throw e;

                }
                return result;
            }
        });
    }
    }


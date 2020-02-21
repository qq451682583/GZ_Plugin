package com.enjoy.gz_plugin;

import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

public class HookUtil {

    private static final String TARGET_INTENT = "target_intent";

    public static void hookAMS() {
        Field singletonField = null;
        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Class<?> clazz = Class.forName("android.app.ActivityManager");
                singletonField = clazz.getDeclaredField("IActivityManagerSingleton");
            } else {
                Class<?> clazz = Class.forName("android.app.ActivityManagerNative");
                singletonField = clazz.getDeclaredField("gDefault");
            }
            singletonField.setAccessible(true);
            Object singleton = singletonField.get(null);

            Class<?> singletonClass = Class.forName("android.util.Singleton");
            Field mInstanceField = singletonClass.getDeclaredField("mInstance");
            mInstanceField.setAccessible(true);
            final Object mInstance = mInstanceField.get(singleton);

            Class<?> iActivityManagerClass = Class.forName("android.app.IActivityManager");
            // IActivityManager 的Class 对象
            Object proxyInstance = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                    new Class[]{iActivityManagerClass}, new InvocationHandler() {

                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            /**
                             * int result = ActivityManager.getService()
                             *                 .startActivity(whoThread, who.getBasePackageName(), intent,
                             *                         intent.resolveTypeIfNeeded(who.getContentResolver()),
                             *                         token, target != null ? target.mEmbeddedID : null,
                             *                         requestCode, 0, null, options);
                             */
                            if ("startActivity".equals(method.getName())) {

                                int index = 0;

                                // 将插件的Intent 替换为 代理的Intent
                                for (int i = 0; i < args.length; i++) {
                                    if (args[i] instanceof Intent) {
                                        index = i;
                                        break;
                                    }
                                }
                                // 插件的 -- 1
                                Intent intent = (Intent) args[index];

                                // 替换成代理的Intent
                                Intent proxyIntent = new Intent();
                                proxyIntent.setClassName("com.enjoy.gz_plugin",
                                        ProxyActivity.class.getName());

                                // 保存插件的Intent
                                proxyIntent.putExtra(TARGET_INTENT, intent);

                                args[index] = proxyIntent;
                            }

                            // IActivityManager 对象 --- 通过反射
                            return method.invoke(mInstance, args);
                        }
                    });

            // IActivityManager对象 = proxyInstance
            // new IActivityManager();// 这个是系统的  == new IActivityManagerProxy();
            mInstanceField.set(singleton, proxyInstance);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void hookHandler() {
        try {

            Class<?> clazz = Class.forName("android.app.ActivityThread");
            Field sCurrentActivityThreadField = clazz.getDeclaredField("sCurrentActivityThread");
            sCurrentActivityThreadField.setAccessible(true);
            Object activityThread = sCurrentActivityThreadField.get(null);

            Field mHField = clazz.getDeclaredField("mH");
            mHField.setAccessible(true);
            Object mH = mHField.get(activityThread);

            Class<?> handlerClass = Class.forName("android.os.Handler");
            Field mCallbackField = handlerClass.getDeclaredField("mCallback");
            mCallbackField.setAccessible(true);

            // Handler 对象
            // 创建一个 Callback 替换系统的 Callback 对象
            mCallbackField.set(mH, new Handler.Callback() {

                @Override
                public boolean handleMessage(@NonNull Message msg) {
                    // 替换Intent
                    switch (msg.what) {
                        case 100:
                            // ActivityClientRecord == msg.obj
                            try {
                                // 代理  2
                                Field intentField = msg.obj.getClass().getDeclaredField("intent");
                                intentField.setAccessible(true);
                                Intent proxyIntent = (Intent) intentField.get(msg.obj);

                                //插件的Intent
                                Intent intent = proxyIntent.getParcelableExtra(TARGET_INTENT);
                                // 判断调用的是否是插件的，如果不是插件的，intent就会为空
                                if (intent != null) {
                                    intentField.set(msg.obj, intent);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                        case 159:
                            //msg.obj -- ClientTransaction
                            //private List<ClientTransactionItem> mActivityCallbacks;

                            //ActivityClientRecord

                            try {
                                Field mActivityCallbacksField = msg.obj.getClass().getDeclaredField("mActivityCallbacks");
                                mActivityCallbacksField.setAccessible(true);
                                //List<ClientTransactionItem>

                                List mActivityCallbacks = (List) mActivityCallbacksField.get(msg.obj);

                                for (int i = 0; i < mActivityCallbacks.size(); i++) {
                                    //LaunchActivityItem
                                    if (mActivityCallbacks.get(i).getClass().getName()
                                            .equals("android.app.servertransaction.LaunchActivityItem")) {
                                        //mActivityCallbacks.get(i) == LauncgActivityItem
                                        Object launchActivityItem = mActivityCallbacks.get(i);
                                        Field mIntentField = launchActivityItem.getClass().getDeclaredField("mIntent");
                                        mIntentField.setAccessible(true);
                                        //代理的
                                        Intent proxyIntent = (Intent) mIntentField.get(launchActivityItem);
                                        Intent intent = proxyIntent.getParcelableExtra(TARGET_INTENT);
                                        //插件的Intent 替换代理的Intent
                                        if (intent != null) {
                                            mIntentField.set(launchActivityItem, intent);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                        default:
                            break;
                    }
                    return false;//1
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }


    }


}

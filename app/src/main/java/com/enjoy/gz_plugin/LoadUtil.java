package com.enjoy.gz_plugin;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

public class LoadUtil {

    // 插件的apk 的路径
    private final static String apkPath = "/sdcard/plugin-debug.apk";

    public static void loadClass(Context context) {

        try {
            // dexElements 的 Field 对象
            Class<?> dexPathListClass = Class.forName("dalvik.system.DexPathList");
            Field dexElementsField = dexPathListClass.getDeclaredField("dexElements");
            dexElementsField.setAccessible(true);

            // Pathlist 的 field 对象
            Class<?> clazz = Class.forName("dalvik.system.BaseDexClassLoader");
            Field pathListField = clazz.getDeclaredField("pathList");
            pathListField.setAccessible(true);

            /**
             * 宿主 的
             */
            // 获取 宿主的 PathClassLoader
            ClassLoader pathClassLoader = context.getClassLoader();

            // 获取 Pathlist 对象
            Object hostPathList = pathListField.get(pathClassLoader);


            // 获取 宿主的 dexElements
            Object[] hostDexElements = (Object[]) dexElementsField.get(hostPathList);

            /**
             * 插件 的
             */
            // 创建 插件的 DexClassLoader
            ClassLoader dexClassLoader = new DexClassLoader(apkPath, context.getCacheDir().getAbsolutePath(),
                    null, pathClassLoader);

            // 获取 Pathlist 对象
            Object pluginPathList = pathListField.get(dexClassLoader);

            // 获取 插件的 dexElements
            Object[] pluginDexElements = (Object[]) dexElementsField.get(pluginPathList);

            // 创建一个新的数组，合并上面两个
            Object[] dexElements = (Object[]) Array.newInstance(hostDexElements.getClass().getComponentType(),
                    hostDexElements.length + pluginDexElements.length);

            // 将 宿主和插件的dexElements 的值复制到 新的dexElements里面去
            System.arraycopy(hostDexElements, 0, dexElements, 0, hostDexElements.length);
            System.arraycopy(pluginDexElements, 0, dexElements, hostDexElements.length, pluginDexElements.length);

            // 赋值 ==> 宿主的dexElements = 新的dexElements;
            dexElementsField.set(hostPathList, dexElements);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static Resources loadResources(Context context) {

        //Resources -- AssetManager
        AssetManager assetManager = null;
        try {
            assetManager = AssetManager.class.newInstance();
            Method addAssetPathMethod = assetManager.getClass().getMethod("addAssetPath", String.class);
            addAssetPathMethod.invoke(assetManager,apkPath);

            Resources resources = context.getResources();

            return new Resources(assetManager,resources.getDisplayMetrics(),resources.getConfiguration());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

}

package com.didichuxing.doraemonkit.plugin.bytecode;

import com.android.build.gradle.AppExtension;
import com.didichuxing.doraemonkit.plugin.DokitExtension;
import com.didichuxing.doraemonkit.plugin.StringUtils;
import com.didichuxing.doraemonkit.plugin.bytecode.method.comm.AmapLocationMethodAdapter;
import com.didichuxing.doraemonkit.plugin.bytecode.method.comm.ApplicationOnCreateMethodAdapter;
import com.didichuxing.doraemonkit.plugin.bytecode.method.comm.BaiduLocationMethodAdapter;
import com.didichuxing.doraemonkit.plugin.bytecode.method.comm.FlagMethodAdapter;
import com.didichuxing.doraemonkit.plugin.bytecode.method.comm.OkHttpMethodAdapter;
import com.didichuxing.doraemonkit.plugin.bytecode.method.comm.PlatformHttpMethodAdapter;
import com.didichuxing.doraemonkit.plugin.bytecode.method.comm.TencentLocationMethodAdapter;
import com.didichuxing.doraemonkit.plugin.bytecode.method.comm.TencentLocationSingleMethodAdapter;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Created by jint on 13/12/2019.
 * 类访问器
 */
public final class DokitCommClassAdapter extends ClassVisitor {
    /**
     * 当前类型
     */
    private String className;
    /**
     * 当前类的父类 假如存在的话
     */
    private String superName;
    private DokitExtension dokitExtension;

    /**
     *
     * @param cv cv
     * @param appExtension appExtension
     * @param dokitExtension dokitExtension
     */
    public DokitCommClassAdapter(final ClassVisitor cv, AppExtension appExtension, DokitExtension dokitExtension) {
        super(Opcodes.ASM7, cv);
        this.dokitExtension = dokitExtension;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);

        this.className = name;
        this.superName = superName;

    }


    /**
     * Visits a method of the class. This method <i>must</i> return a new {@link MethodVisitor}
     * instance (or {@literal null}) each time it is called, i.e., it should not return a previously
     * returned visitor.
     *
     * @param access     the method's access flags (see {@link Opcodes}). This parameter also indicates if
     *                   the method is synthetic and/or deprecated.
     * @param methodName the method's name.
     * @param desc       the method's descriptor (see {@link Type}).
     * @param signature  the method's signature. May be {@literal null} if the method parameters,
     *                   return type and exceptions do not use generic types.
     * @param exceptions the internal names of the method's exception classes (see {@link
     *                   Type#getInternalName()}). May be {@literal null}.
     * @return an object to visit the byte code of the method, or {@literal null} if this class
     * visitor is not interested in visiting the code of this method.
     */
    @Override
    public MethodVisitor visitMethod(int access, String methodName, String desc, String signature, String[] exceptions) {
        //从传进来的ClassWriter中读取MethodVisitor
        MethodVisitor mv = cv.visitMethod(access, methodName, desc, signature, exceptions);
        //开关被关闭 不插入代码
        if (!dokitExtension.dokitPluginSwitch) {
            return mv;
        }
        //开发者变量字节码替换
        if (className.equals("com/didichuxing/doraemonkit/DoraemonKitReal") && methodName.equals("install") && desc != null) {
            if (getParamsSize(desc) == 3) {
                log(className, access, methodName, desc, signature);
                //创建MethodVisitor代理
                return mv == null ? null : new FlagMethodAdapter(access, desc, mv);
            }
        }

        //高德地图字节码替换
        if (className.equals("com/amap/api/location/AMapLocationClient") && methodName.equals("setLocationListener")) {
            //创建MethodVisitor代理
            log(className, access, methodName, desc, signature);
            return mv == null ? null : new AmapLocationMethodAdapter(access, desc, mv);
        }

        //腾讯地图字节码替换
        if (className.equals("com/tencent/map/geolocation/TencentLocationManager") && methodName.equals("requestLocationUpdates")) {
            log(className, access, methodName, desc, signature);
            //创建MethodVisitor代理
            return mv == null ? null : new TencentLocationMethodAdapter(access, desc, mv);
        }

        //腾讯地图单次定位
        if (className.equals("com/tencent/map/geolocation/TencentLocationManager") && methodName.equals("requestSingleFreshLocation")) {
            log(className, access, methodName, desc, signature);
            //创建MethodVisitor代理
            return mv == null ? null : new TencentLocationSingleMethodAdapter(access, desc, mv);
        }

        //百度地图定位 汇报函数签名错误 暂时未找到原因
//        if (className.equals("com/baidu/location/LocationClient") && name.equals("registerLocationListener")) {
//            log(className, access, name, desc, signature);
//            //创建MethodVisitor代理
//            return mv == null ? null : new BaiduLocationMethodAdapter(access, desc, mv);
//        }

        //百度地图定位
        if (methodName.equals("onReceiveLocation") && desc.equals("(Lcom/baidu/location/BDLocation;)V")) {
            log(className, access, methodName, desc, signature);
            //创建MethodVisitor代理
            return mv == null ? null : new BaiduLocationMethodAdapter(access, desc, mv);
        }


        //okhttp 拦截器字节码替换
        if (className.equals("okhttp3/OkHttpClient$Builder") && methodName.equals("<init>")) {
            //创建MethodVisitor代理
            log(className, access, methodName, desc, signature);
            return mv == null ? null : new OkHttpMethodAdapter(access, desc, mv);
        }

        //didi平台端 网络 拦截器字节码替换
        if (className.equals("didihttp/DidiHttpClient$Builder") && methodName.equals("<init>")) {
            //创建MethodVisitor代理
            log(className, access, methodName, desc, signature);
            return mv == null ? null : new PlatformHttpMethodAdapter(access, desc, mv);
        }
        //app启动hook点 onCreate()函数 兼容MultiDex
        if (!StringUtils.isEmpty(superName) && (superName.equals("android/app/Application") || superName.equals("android/support/multidex/MultiDexApplication")) && methodName.equals("onCreate") && desc.equals("()V")) {
            log(className, access, methodName, desc, signature);
            return mv == null ? null : new ApplicationOnCreateMethodAdapter(access, methodName, desc, mv);
        }

        //过滤所有类中当前方法中所有的字节码
        return mv;

    }

    /**
     * 获取形参个数
     *
     * @param desc
     * @return
     */
    private int getParamsSize(String desc) {
        //(Landroid/app/Application;Ljava/util/List;Ljava/lang/String;)V 包含3个参数的install方法实例
        if (desc == null || desc.equals("")) {
            return 0;
        }
        return desc.split(";").length - 1;
    }

    /**
     * 日志输出
     *
     * @param className
     * @param access
     * @param name
     * @param desc
     * @param signature
     */
    private void log(String className, int access, String name, String desc, String signature) {
        System.out.println("matched====>" + "  className===" + className + "   access===" + access + "   methodName===" + name + "   desc===" + desc + "   signature===" + signature);
    }

}
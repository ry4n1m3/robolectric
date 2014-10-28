package org.robolectric.internal;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import com.android.server.pm.Installer;
import com.android.server.pm.PackageManagerService;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.res.ResBunch;
import org.robolectric.res.ResourceLoader;
import org.robolectric.shadows.ShadowActivityThread;
import org.robolectric.shadows.ShadowContextImpl;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowResources;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.robolectric.Robolectric.shadowOf;
import static org.robolectric.internal.ReflectionHelpers.*;

public class ParallelUniverse implements ParallelUniverseInterface {
  private static final String DEFAULT_PACKAGE_NAME = "org.robolectric.default";
  private final RobolectricTestRunner robolectricTestRunner;

  private boolean loggingInitialized = false;
  private SdkConfig sdkConfig;

  public ParallelUniverse(RobolectricTestRunner robolectricTestRunner) {
    this.robolectricTestRunner = robolectricTestRunner;
  }

  @Override
  public void resetStaticState(Config config) {
    Robolectric.reset(config);

    if (!loggingInitialized) {
      ShadowLog.setupLogging();
      loggingInitialized = true;
    }
  }

  /*
   * If the Config already has a version qualifier, do nothing. Otherwise, add a version
   * qualifier for the target api level (which comes from the manifest or Config.emulateSdk()).
   */
  private String addVersionQualifierToQualifiers(String qualifiers) {
    int versionQualifierApiLevel = ResBunch.getVersionQualifierApiLevel(qualifiers);
    if (versionQualifierApiLevel == -1) {
      if (qualifiers.length() > 0) {
        qualifiers += "-";
      }
      qualifiers += "v" + sdkConfig.getApiLevel();
    }
    return qualifiers;
  }

  @Override
  public void setUpApplicationState(Method method, TestLifecycle testLifecycle, boolean strictI18n, ResourceLoader systemResourceLoader, AndroidManifest appManifest, Config config) {
    Robolectric.application = null;
    Class<?> applicationPackageManagerClass = loadClassReflectively(getClass().getClassLoader(), "android.app.ApplicationPackageManager");
    Class<?> contextImplClass = ReflectionHelpers.loadClassReflectively(getClass().getClassLoader(), ShadowContextImpl.CLASS_NAME);
//    Robolectric.packageManager = ReflectionHelpers.callConstructorReflectively(applicationPackageManagerClass, new ClassParameter(contextImplClass, null), new ClassParameter(IPackageManager.class, PackageManagerService.main(null, null, false, false)));
//    Robolectric.packageManager.addPackage(DEFAULT_PACKAGE_NAME);
    ResourceLoader resourceLoader;
    if (appManifest != null) {
      resourceLoader = robolectricTestRunner.getAppResourceLoader(sdkConfig, systemResourceLoader, appManifest);
//      Robolectric.packageManager.addManifest(appManifest, resourceLoader);
    } else {
      resourceLoader = systemResourceLoader;
    }

    ShadowResources.setSystemResources(systemResourceLoader);
    String qualifiers = addVersionQualifierToQualifiers(config.qualifiers());
    Resources systemResources = Resources.getSystem();
    Configuration configuration = systemResources.getConfiguration();
    shadowOf(configuration).overrideQualifiers(qualifiers);
    systemResources.updateConfiguration(configuration, systemResources.getDisplayMetrics());
    shadowOf(systemResources.getAssets()).setQualifiers(qualifiers);

//    Class<?> contextImplClass = ReflectionHelpers.loadClassReflectively(getClass().getClassLoader(), ShadowContextImpl.CLASS_NAME);

    Class<?> activityThreadClass = ReflectionHelpers.loadClassReflectively(getClass().getClassLoader(), ShadowActivityThread.CLASS_NAME);
    Object activityThread = callConstructorReflectively(activityThreadClass);
    Robolectric.activityThread = activityThread;

    ReflectionHelpers.setFieldReflectively(activityThread, "mInstrumentation", new RoboInstrumentation());
    ReflectionHelpers.setFieldReflectively(activityThread, "mCompatConfiguration", configuration);

    Context systemContextImpl = ReflectionHelpers.callStaticMethodReflectively(contextImplClass, "createSystemContext", new ClassParameter(activityThreadClass, activityThread));

    Class<?> packageInstalledInfoClass = loadClassReflectively(getClass().getClassLoader(), "com.android.server.pm.PackageManagerService$PackageInstalledInfo");
    Class installArgsClass = loadClassReflectively(getClass().getClassLoader(), "com.android.server.pm.PackageManagerService$InstallArgs");

    final Application application = (Application) testLifecycle.createApplication(method, appManifest, config);

    IPackageManager packageManagerService = PackageManagerService.main(systemContextImpl, new Installer(), false, false);
    Robolectric.packageManager = callConstructorReflectively(applicationPackageManagerClass, new ClassParameter(contextImplClass, systemContextImpl), new ClassParameter(IPackageManager.class, packageManagerService));

    String path = appManifest.getAssetsDirectory().getPath();
    path = path + "/..";
    path = "/Users/pivotal/workspace/robolectric-upstream/robolectric/src/test/resources.zip";
    Object installArgs = callInstanceMethodReflectively(packageManagerService, "createInstallArgs", new ClassParameter(int.class, PackageManager.INSTALL_INTERNAL),
        new ClassParameter(String.class, path), new ClassParameter(String.class, ""), new ClassParameter(String.class, ""));


    Object res;
    try {
      Constructor constructor = packageInstalledInfoClass.getDeclaredConstructor(PackageManagerService.class);
      constructor.setAccessible(true);
      res = constructor.newInstance(packageManagerService);
    } catch (InstantiationException e1) {
      throw new RuntimeException("error instantiating " + packageInstalledInfoClass.getName(), e1);
    } catch (Exception e11) {
      throw new RuntimeException(e11);
    }




    callInstanceMethodReflectively(packageManagerService, "installPackageLI", new ClassParameter(installArgsClass, installArgs),
        new ClassParameter(boolean.class, true), new ClassParameter(packageInstalledInfoClass, res));

    if (application != null) {
      String packageName = appManifest != null ? appManifest.getPackageName() : null;
      if (packageName == null) packageName = DEFAULT_PACKAGE_NAME;

      ApplicationInfo applicationInfo;
      try {
        applicationInfo = Robolectric.packageManager.getApplicationInfo(packageName, 0);
      } catch (PackageManager.NameNotFoundException e) {
        throw new RuntimeException(e);
      }

      Class<?> compatibilityInfoClass = loadClassReflectively(getClass().getClassLoader(), "android.content.res.CompatibilityInfo");

      Object loadedApk = callInstanceMethodReflectively(activityThread, "getPackageInfo", new ClassParameter(ApplicationInfo.class, applicationInfo),
          new ClassParameter(compatibilityInfoClass, null), new ClassParameter(ClassLoader.class, getClass().getClassLoader()), new ClassParameter(boolean.class, false),
          new ClassParameter(boolean.class, true));

      shadowOf(application).bind(appManifest, resourceLoader);
      if (appManifest == null) {
        // todo: make this cleaner...
        shadowOf(application).setPackageName(applicationInfo.packageName);
      }
      Resources appResources = application.getResources();
      ReflectionHelpers.setFieldReflectively(loadedApk, "mResources", appResources);
      Context contextImpl = callInstanceMethodReflectively(systemContextImpl, "createPackageContext", new ClassParameter(String.class, applicationInfo.packageName), new ClassParameter(int.class, Context.CONTEXT_INCLUDE_CODE));
      ReflectionHelpers.setFieldReflectively(activityThread, "mInitialApplication", application);
      callInstanceMethodReflectively(application, "attach", new ClassParameter(Context.class, contextImpl));

      appResources.updateConfiguration(configuration, appResources.getDisplayMetrics());
      shadowOf(appResources.getAssets()).setQualifiers(qualifiers);
      shadowOf(application).setStrictI18n(strictI18n);

      Robolectric.application = application;
      application.onCreate();
    }
  }

  @Override
  public void tearDownApplication() {
    if (Robolectric.application != null) {
      Robolectric.application.onTerminate();
    }
  }

  @Override
  public Object getCurrentApplication() {
    return Robolectric.application;
  }

  @Override
  public void setSdkConfig(SdkConfig sdkConfig) {
    this.sdkConfig = sdkConfig;
  }
}

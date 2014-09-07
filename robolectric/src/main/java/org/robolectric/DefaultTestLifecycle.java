package org.robolectric;

import android.app.Application;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import org.robolectric.annotation.Config;
import org.robolectric.internal.ClassNameResolver;
import org.robolectric.res.ActivityData;
import org.robolectric.res.builder.RobolectricPackageManager;

import java.lang.reflect.Method;
import java.util.Map;

public class DefaultTestLifecycle implements TestLifecycle {
  /**
   * Override this method if you want to provide your own implementation of Application.
   * <p/>
   * This method attempts to instantiate an application instance as follows:-
   *
   * <li>
   *   <ol>If specified loads the application specified in the Config annotation</ol>
   *   <ol>Attempt to load a test application as documented <a href="http://robolectric.blogspot.com/2013/04/the-test-lifecycle-in-20.html">here</a></ol>
   *   <ol>Use the application as specified in the AndroidManifest.xml</ol>
   *   <ol>Instantiate a standard {@link android.app.Application}</ol>
   * </li>
   *
   * @param method The currently-running test method.
   * @param appManifest The application manifest.
   * @param config The configuration annotation from the test if present.
   * @return An instance of the Application class specified by the ApplicationManifest.xml or an instance of
   *         Application if not specified.
   */
  public Application createApplication(Method method, AndroidManifest appManifest, Config config) {

    Application application = null;
    if (config != null && !config.application().getCanonicalName().equals(Application.class.getCanonicalName())) {
      if (config.application().getCanonicalName() != null) {
        Class<? extends Application> applicationClass = null;
        try {
          applicationClass = new ClassNameResolver<Application>(null, config.application().getCanonicalName()).resolve();
        } catch (ClassNotFoundException e) {
          throw new RuntimeException(e);
        }
        application = newInstance(applicationClass);
      }
    } else if (appManifest != null && appManifest.getApplicationName() != null) {
      Class<? extends Application> applicationClass = null;
      try {
        applicationClass = new ClassNameResolver<Application>(appManifest.getPackageName(), getTestApplicationName(appManifest.getApplicationName())).resolve();
      } catch (ClassNotFoundException e) {
        // no problem
      }

      if (applicationClass == null) {
        try {
          applicationClass = new ClassNameResolver<Application>(appManifest.getPackageName(), appManifest.getApplicationName()).resolve();
        } catch (ClassNotFoundException e) {
          throw new RuntimeException(e);
        }
      }

      application = newInstance(applicationClass);
    } else {
      application = new Application();
    }

    addManifestActivitiesToPackageManager(appManifest, application);

    return application;
  }

  private static Application newInstance(Class<? extends Application> applicationClass) {
    Application application;
    try {
      application = applicationClass.newInstance();
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    return application;
  }

  private void addManifestActivitiesToPackageManager(AndroidManifest appManifest, Application application) {
    if (appManifest != null) {
      Map<String,ActivityData> activityDatas = appManifest.getActivityDatas();

      RobolectricPackageManager packageManager = (RobolectricPackageManager) application.getPackageManager();

      for (ActivityData data : activityDatas.values()) {
        String name = data.getName();
        String activityName = name.startsWith(".") ? appManifest.getPackageName() + name : name;
        packageManager.addResolveInfoForIntent(new Intent(activityName), new ResolveInfo());
      }
    }
  }

  /**
   * Called before each test method is run.
   *
   * @param method the test method about to be run
   */
  public void beforeTest(final Method method) {
    if (Robolectric.application instanceof TestLifecycleApplication) {
      ((TestLifecycleApplication) Robolectric.application).beforeTest(method);
    }
  }

  public void prepareTest(final Object test) {
    if (Robolectric.application instanceof TestLifecycleApplication) {
      ((TestLifecycleApplication) Robolectric.application).prepareTest(test);
    }
  }

  /**
   * Called after each test method is run.
   *
   * @param method the test method that just ran.
   */
  public void afterTest(final Method method) {
    if (Robolectric.application instanceof TestLifecycleApplication) {
      ((TestLifecycleApplication) Robolectric.application).afterTest(method);
    }
  }

  public String getTestApplicationName(String applicationName) {
    int lastDot = applicationName.lastIndexOf('.');
    if (lastDot > -1) {
      return applicationName.substring(0, lastDot) + ".Test" + applicationName.substring(lastDot + 1);
    } else {
      return "Test" + applicationName;
    }
  }
}

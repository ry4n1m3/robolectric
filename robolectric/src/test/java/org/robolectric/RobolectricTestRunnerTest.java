package org.robolectric;

import android.app.Application;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.model.InitializationError;
import org.robolectric.annotation.Config;
import org.robolectric.res.PackageResourceLoader;
import org.robolectric.res.ResourceLoader;
import org.robolectric.res.ResourcePath;
import org.robolectric.shadows.ShadowView;
import org.robolectric.shadows.ShadowViewGroup;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Properties;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.reflect.core.Reflection.method;

public class RobolectricTestRunnerTest {
  @Test public void whenClassHasConfigAnnotation_getConfig_shouldMergeClassAndMethodConfig() throws Exception {
    assertConfig(configFor(Test1.class, "withoutAnnotation"),
        1, "foo", "from-test", "test/res", 2, new Class[]{Test1.class}, Application.class);

    assertConfig(configFor(Test1.class, "withDefaultsAnnotation"),
        1, "foo", "from-test", "test/res", 2, new Class[]{Test1.class}, Application.class);

    assertConfig(configFor(Test1.class, "withOverrideAnnotation"),
        9, "furf", "from-method", "method/res", 8, new Class[]{Test1.class, Test2.class}, Application.class);
  }

  @Test public void whenClassDoesntHaveConfigAnnotation_getConfig_shouldUseMethodConfig() throws Exception {
    assertConfig(configFor(Test2.class, "withoutAnnotation"),
        -1, "--default", "", "res", -1, new Class[]{}, Application.class);

    assertConfig(configFor(Test2.class, "withDefaultsAnnotation"),
        -1, "--default", "", "res", -1, new Class[]{}, Application.class);

    assertConfig(configFor(Test2.class, "withOverrideAnnotation"),
        9, "furf", "from-method", "method/res", 8, new Class[]{Test1.class}, Application.class);
  }

  @Test public void whenClassAndSubclassHaveConfigAnnotation_getConfig_shouldMergeClassSubclassAndMethodConfig() throws Exception {
      assertConfig(configFor(Test3.class, "withoutAnnotation"),
          1, "foo", "from-subclass", "test/res", 2, new Class[]{Test1.class}, Application.class);

    assertConfig(configFor(Test3.class, "withDefaultsAnnotation"),
        1, "foo", "from-subclass", "test/res", 2, new Class[]{Test1.class}, Application.class);

    assertConfig(configFor(Test3.class, "withOverrideAnnotation"),
        9, "furf", "from-method", "method/res", 8, new Class[]{Test1.class, Test2.class}, Application.class);
  }

  @Test public void whenClassDoesntHaveConfigAnnotationButSubclassDoes_getConfig_shouldMergeSubclassAndMethodConfig() throws Exception {
    assertConfig(configFor(Test4.class, "withoutAnnotation"),
        -1, "--default", "from-subclass", "res", -1, new Class[]{}, Application.class);

    assertConfig(configFor(Test4.class, "withDefaultsAnnotation"),
        -1, "--default", "from-subclass", "res", -1, new Class[]{}, Application.class);

    assertConfig(configFor(Test4.class, "withOverrideAnnotation"),
        9, "furf", "from-method", "method/res", 8, new Class[]{Test1.class}, Application.class);
  }

  @Test public void shouldLoadDefaultsFromPropertiesFile() throws Exception {
    Properties properties = properties(
        "emulateSdk: 432\n" +
            "manifest: --none\n" +
            "qualifiers: from-properties-file\n" +
            "resourceDir: from/properties/file/res\n" +
            "reportSdk: 234\n" +
            "shadows: org.robolectric.shadows.ShadowView, org.robolectric.shadows.ShadowViewGroup\n" +
            "application: org.robolectric.TestFakeApp");
    assertConfig(configFor(Test2.class, "withoutAnnotation", properties),
        432, "--none", "from-properties-file", "from/properties/file/res", 234, new Class[] {ShadowView.class, ShadowViewGroup.class}, TestFakeApp.class);
  }

  @Test public void withEmptyShadowList_shouldLoadDefaultsFromPropertiesFile() throws Exception {
    Properties properties = properties("shadows:");
    assertConfig(configFor(Test2.class, "withoutAnnotation", properties),
        -1, "--default", "", "res", -1, new Class[] {}, Application.class);
  }

  @Test public void rememberThatSomeTestRunnerMethodsShouldBeOverridable() throws Exception {
    // super weak test for now, just remember not to make these methods static!

    //noinspection UnusedDeclaration
    class CustomTestRunner extends RobolectricTestRunner {
      public CustomTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
      }

      @Override public PackageResourceLoader createResourceLoader(ResourcePath resourcePath) {
        return super.createResourceLoader(resourcePath);
      }

      @Override
      protected ResourceLoader createAppResourceLoader(ResourceLoader systemResourceLoader,
          AndroidManifest appManifest) {
        return super.createAppResourceLoader(systemResourceLoader, appManifest);
      }
    }
  }

  private Config configFor(Class<?> testClass, String methodName, final Properties configProperties) throws InitializationError {
    return new RobolectricTestRunner(testClass) {
      @Override protected Properties getConfigProperties() {
        return configProperties;
      }
    }.getConfig(method(methodName).withParameterTypes().in(testClass).info());
  }

  private Config configFor(Class<?> testClass, String methodName) throws InitializationError {
    return new RobolectricTestRunner(testClass)
          .getConfig(method(methodName).withParameterTypes().in(testClass).info());
  }

  private void assertConfig(Config config, int emulateSdk, String manifest, String qualifiers, String resourceDir, int reportSdk, Class[] shadows, Class<? extends Application> applicationClass) {
    assertThat(stringify(config)).isEqualTo(stringify(emulateSdk, manifest, qualifiers, resourceDir, reportSdk, shadows));
  }

  @Ignore @Config(emulateSdk = 1, manifest = "foo", reportSdk = 2, shadows = Test1.class, qualifiers = "from-test", resourceDir = "test/res")
  public static class Test1 {
    @Test public void withoutAnnotation() throws Exception {
    }

    @Config
    @Test public void withDefaultsAnnotation() throws Exception {
    }

    @Config(emulateSdk = 9, manifest = "furf", reportSdk = 8, shadows = Test2.class, qualifiers = "from-method", resourceDir = "method/res")
    @Test public void withOverrideAnnotation() throws Exception {
    }
  }

  @Ignore
  public static class Test2 {
    @Test public void withoutAnnotation() throws Exception {
    }

    @Config
    @Test public void withDefaultsAnnotation() throws Exception {
    }

    @Config(emulateSdk = 9, manifest = "furf", reportSdk = 8, shadows = Test1.class, qualifiers = "from-method", resourceDir = "method/res")
    @Test public void withOverrideAnnotation() throws Exception {
    }
  }

  @Ignore
  @Config(qualifiers = "from-subclass")
  public static class Test3 extends Test1 {
  }

  @Ignore
  @Config(qualifiers = "from-subclass")
  public static class Test4 extends Test2 {
  }

  private String stringify(Config config) {
    int emulateSdk = config.emulateSdk();
    String manifest = config.manifest();
    String qualifiers = config.qualifiers();
    String resourceDir = config.resourceDir();
    int reportSdk = config.reportSdk();
    Class<?>[] shadows = config.shadows();
    return stringify(emulateSdk, manifest, qualifiers, resourceDir, reportSdk, shadows);
  }

  private String stringify(int emulateSdk, String manifest, String qualifiers, String resourceDir, int reportSdk, Class<?>[] shadows) {
      String[] stringClasses = new String[shadows.length];
      for (int i = 0; i < stringClasses.length; i++) {
          stringClasses[i] = shadows[i].toString();
      }

      Arrays.sort(stringClasses);

      return "emulateSdk=" + emulateSdk + "\n" +
        "manifest=" + manifest + "\n" +
        "qualifiers=" + qualifiers + "\n" +
        "resourceDir=" + resourceDir + "\n" +
        "reportSdk=" + reportSdk + "\n" +
        "shadows=" +  Arrays.toString(stringClasses);
  }

  private Properties properties(String s) throws IOException {
    StringReader reader = new StringReader(s);
    Properties properties = new Properties();
    properties.load(reader);
    return properties;
  }
}

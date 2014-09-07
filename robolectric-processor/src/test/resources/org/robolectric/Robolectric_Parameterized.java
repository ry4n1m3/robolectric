package org.robolectric;

import javax.annotation.Generated;

import org.robolectric.annotation.processing.objects.Dummy;
import org.robolectric.annotation.processing.objects.ParameterizedDummy;
import org.robolectric.annotation.processing.shadows.ShadowDummy;
import org.robolectric.annotation.processing.shadows.ShadowParameterizedDummy;
import org.robolectric.bytecode.RobolectricInternals;
import org.robolectric.bytecode.ShadowWrangler;

@Generated("org.robolectric.annotation.processing.RoboProcessor")
public class RobolectricBase {

  public static final Class<?>[] DEFAULT_SHADOW_CLASSES = {
    ShadowDummy.class,
    ShadowParameterizedDummy.class
  };
  
  public static ShadowDummy shadowOf(Dummy actual) {
    return (ShadowDummy) shadowOf_(actual);
  }
  
  public static <T, N extends Number> ShadowParameterizedDummy<T,N> shadowOf(ParameterizedDummy<T,N> actual) {
    return (ShadowParameterizedDummy)<T,N> shadowOf_(actual);
  }

  public static void reset() {
    ShadowDummy.resetter_method();
  }

  public static ShadowWrangler getShadowWrangler() {
    return ((ShadowWrangler) RobolectricInternals.getClassHandler());
  }
  
  @SuppressWarnings({"unchecked"})
  public static <P, R> P shadowOf_(R instance) {
    return (P) getShadowWrangler().shadowOf(instance);
  }
}

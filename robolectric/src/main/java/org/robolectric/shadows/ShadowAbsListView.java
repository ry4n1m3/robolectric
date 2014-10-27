package org.robolectric.shadows;

import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.ListAdapter;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.internal.ReflectionHelpers;

import static org.robolectric.Robolectric.directlyOn;

@Implements(AbsListView.class)
public class ShadowAbsListView extends ShadowAdapterView {
  @RealObject AbsListView realAbsListView;
  private AbsListView.OnScrollListener onScrollListener;
  private int smoothScrolledPosition;
  private int lastSmoothScrollByDistance;
  private int lastSmoothScrollByDuration;

  @Implementation
  public void setOnScrollListener(AbsListView.OnScrollListener l) {
    onScrollListener = l;
  }

  @Implementation
  public void smoothScrollToPosition(int position) {
    smoothScrolledPosition = position;
  }

  @Implementation
  public void smoothScrollBy(int distance, int duration) {
    this.lastSmoothScrollByDistance = distance;
    this.lastSmoothScrollByDuration = duration;
  }

  @Implementation
  public void setAdapter(ListAdapter adapter) {
    directlyOn(realAbsListView, AbsListView.class, "setAdapter", new ReflectionHelpers.ClassParameter(ListAdapter.class, adapter));
    if(adapter == null) {
      return;
    }
    for(int i = 0; i < adapter.getCount(); i++) {
      if(adapter.getItemViewType(i) >= adapter.getViewTypeCount()) {
        throw new RuntimeException("getItemViewType() must be between 0 and getViewTypeCount() - 1");
      }
    }
  }

  /**
   * Robolectric accessor for the onScrollListener
   *
   * @return AbsListView.OnScrollListener
   */
  public AbsListView.OnScrollListener getOnScrollListener() {
    return onScrollListener;
  }

  /**
   * Robolectric accessor for the last smoothScrolledPosition
   *
   * @return int position
   */
  public int getSmoothScrolledPosition() {
    return smoothScrolledPosition;
  }

  /**
   * Robolectric accessor for the last smoothScrollBy distance
   *
   * @return int distance
   */
  public int getLastSmoothScrollByDistance() {
    return lastSmoothScrollByDistance;
  }

  /**
   * Robolectric accessor for the last smoothScrollBy duration
   *
   * @return int duration
   */
  public int getLastSmoothScrollByDuration() {
    return lastSmoothScrollByDuration;
  }
}

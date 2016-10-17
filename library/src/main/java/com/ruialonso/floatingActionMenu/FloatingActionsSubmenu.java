package com.ruialonso.floatingactionmenu;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.BounceInterpolator;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by rui.alonso on 27/9/16.
 */

public class FloatingActionsSubmenu extends ViewGroup {
  public static final int EXPAND_UP = 0;
  public static final int EXPAND_DOWN = 1;
  public static final int EXPAND_LEFT = 2;
  public static final int EXPAND_RIGHT = 3;
  public static final int EXPAND_ROUND = 4;
  public static final int EXPAND_FAN = 5;

  public boolean isVisible = false;

  private boolean enableOverlay;
  private int expandDirection;
  private String submenuGroup;
  private int buttonSpacing;
  private int radius;
  private int angleOverflow;
  @DrawableRes private int submenuIconRes;
  private Drawable submenuIcon;

  private int menuLeft;
  private int menuTop;
  private int menuRight;
  private int menuBottom;

  private boolean animationLaunched = false;
  private FloatingActionsMenu menu;

  private List<FloatingActionButton> floatingActionButtonItems;

  private OnFloatingActionSubmenuUpdateListener submenuUpdateListener;

  //region constructor
  public FloatingActionsSubmenu(Context context) {
    super(context);
    init(null, 0);
  }

  public FloatingActionsSubmenu(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(attrs, 0);
  }

  public FloatingActionsSubmenu(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(attrs, defStyleAttr);
  }
  //endregion

  private void init(AttributeSet attrs, int defStyle) {
    loadAttributes(attrs, defStyle);

    initViews();
  }

  private void loadAttributes(AttributeSet attributeSet, int defStyle) {
    TypedArray attrSubmenu =
        getContext().obtainStyledAttributes(attributeSet, R.styleable.FloatingActionsSubmenu, 0, 0);

    enableOverlay =
        attrSubmenu.getBoolean(R.styleable.FloatingActionsSubmenu_fab_enable_overlay, false);
    expandDirection =
        attrSubmenu.getInt(R.styleable.FloatingActionsSubmenu_fab_expand_direction, EXPAND_UP);
    submenuGroup = attrSubmenu.getString(R.styleable.FloatingActionsSubmenu_fab_submenu_group);

    radius =
        attrSubmenu.getDimensionPixelSize(R.styleable.FloatingActionsSubmenu_fab_submenu_radius,
            getResources().getDimensionPixelSize(R.dimen.fab_submenu_default_radius));
    angleOverflow =
        attrSubmenu.getInteger(R.styleable.FloatingActionsSubmenu_fab_submenu_angle_overflow, 0);

    buttonSpacing = attrSubmenu.getInt(R.styleable.FloatingActionsSubmenu_fab_button_spacing,
        getResources().getDimensionPixelSize(R.dimen.fab_button_default_spacing));

    submenuIconRes =
        attrSubmenu.getResourceId(R.styleable.FloatingActionsSubmenu_fab_submenu_icon, 0);

    attrSubmenu.recycle();
  }

  private void initViews() {
    requestDisallowInterceptTouchEvent(false);

    floatingActionButtonItems = new ArrayList<>();

    if (submenuIconRes != 0) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        submenuIcon = getContext().getDrawable(submenuIconRes);
      } else {
        submenuIcon = getContext().getResources().getDrawable(submenuIconRes);
      }
    }
  }

  @Override protected void onFinishInflate() {
    super.onFinishInflate();

    int size = getChildCount();
    for (int i = 0; i < size; i++) {
      Object nextChild = getChildAt(i);
      if (nextChild instanceof FloatingActionButton) {
        floatingActionButtonItems.add((FloatingActionButton) nextChild);
      }
    }
  }

  @Override protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    menu = (FloatingActionsMenu) getParent();

    validateAttributes();
  }

  private void validateAttributes() {
    switch (menu.verticalAlignment) {
      case FloatingActionsMenu.ALIGNMENT_CENTER:
        break;
      case FloatingActionsMenu.ALIGNMENT_TOP:
        if (expandDirection == EXPAND_UP) {
          throw new IllegalArgumentException("Menu alignment top cannot support submenu expand up");
        }
        break;
      case FloatingActionsMenu.ALIGNMENT_BOTTOM:
        if (expandDirection == EXPAND_DOWN) {
          throw new IllegalArgumentException(
              "Menu alignment bottom cannot support submenu expand down");
        }
        break;
    }

    switch (menu.horizontalAlignment) {
      case FloatingActionsMenu.ALIGNMENT_CENTER:
        break;
      case FloatingActionsMenu.ALIGNMENT_LEFT:
        if (expandDirection == EXPAND_LEFT) {
          throw new IllegalArgumentException(
              "Menu alignment left cannot support submenu expand left");
        }
        break;
      case FloatingActionsMenu.ALIGNMENT_RIGHT:
        if (expandDirection == EXPAND_RIGHT) {
          throw new IllegalArgumentException(
              "Menu alignment right cannot support submenu expand right");
        }
        break;
    }
  }

  //region measure
  @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int count = getChildCount();
    // Measurement will ultimately be computing these values.
    int maxHeight = 0;
    int maxWidth = 0;
    int width = 0;
    int height = 0;

    switch (expandDirection) {
      case EXPAND_UP:
      case EXPAND_DOWN:
        for (int i = 0; i < count; i++) {
          final View child = getChildAt(i);
          if (child.getVisibility() == GONE) continue;
          child.measure(widthMeasureSpec, heightMeasureSpec);

          maxWidth = Math.max(maxWidth, child.getMeasuredWidth());
          maxHeight += child.getMeasuredHeight();
        }
        maxHeight += buttonSpacing * (getChildCount() - 1);
        break;
      case EXPAND_LEFT:
      case EXPAND_RIGHT:
        for (int i = 0; i < count; i++) {
          final View child = getChildAt(i);
          if (child.getVisibility() == GONE) continue;
          child.measure(widthMeasureSpec, heightMeasureSpec);

          maxHeight = Math.max(maxHeight, child.getMeasuredHeight());
          maxWidth += child.getMeasuredWidth();
        }

        maxWidth += buttonSpacing * (getChildCount() - 1);
        break;
      case EXPAND_ROUND:
        for (int i = 0; i < count; i++) {
          final View child = getChildAt(i);
          if (child.getVisibility() == GONE) continue;
          child.measure(widthMeasureSpec, heightMeasureSpec);

          maxWidth = Math.max(maxWidth, child.getMeasuredWidth());
          maxHeight = Math.max(maxHeight, child.getMeasuredHeight());
        }

        height += menu.floatingActionMenuButton.getMeasuredHeight();
        height += 2 * maxHeight;
        height += 2 * buttonSpacing;
        height += 2 * radius;

        width += menu.floatingActionMenuButton.getMeasuredWidth();
        width += 2 * maxWidth;
        width += 2 * buttonSpacing;
        width += 2 * radius;

        maxWidth = Math.max(maxWidth, width);
        maxHeight = Math.max(maxHeight, height);
        break;
      case EXPAND_FAN:
        for (int i = 0; i < count; i++) {
          final View child = getChildAt(i);
          if (child.getVisibility() == GONE) continue;
          child.measure(widthMeasureSpec, heightMeasureSpec);

          maxWidth = Math.max(maxWidth, child.getMeasuredWidth());
          maxHeight = Math.max(maxHeight, child.getMeasuredHeight());
        }

        height += menu.floatingActionMenuButton.getMeasuredHeight();
        //TODO: adjust height adding sin(overflowAngle) instead of floatingMenuButton.height
        if (menu.verticalAlignment == FloatingActionsMenu.ALIGNMENT_TOP
            || menu.verticalAlignment == FloatingActionsMenu.ALIGNMENT_BOTTOM) {
          height += maxHeight;
          height += buttonSpacing;
          height += radius;
        } else {
          height += 2 * maxHeight;
          height += 2 * buttonSpacing;
          height += 2 * radius;
        }

        width += menu.floatingActionMenuButton.getMeasuredWidth();
        //TODO: adjust width adding cos(overflowAngle) instead of floatingMenuButton.width
        if (menu.verticalAlignment == FloatingActionsMenu.ALIGNMENT_LEFT
            || menu.verticalAlignment == FloatingActionsMenu.ALIGNMENT_RIGHT) {
          width += maxWidth;
          width += buttonSpacing;
          width += radius;
        } else {
          width += 2 * maxWidth;
          width += 2 * buttonSpacing;
          width += 2 * radius;
        }

        maxWidth = Math.max(maxWidth, width);
        maxHeight = Math.max(maxHeight, height);
        break;
    }

    // Report our final dimensions.
    setMeasuredDimension(maxWidth, maxHeight);
  }
  //endregion

  //region layout
  @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
    //get the available size of child view
    menuLeft = this.getPaddingLeft();
    menuTop = this.getPaddingTop();
    menuRight = this.getMeasuredWidth() - this.getPaddingRight();
    menuBottom = this.getMeasuredHeight() - this.getPaddingBottom();

    int count = getChildCount();

    int childLeft, childTop;
    final int maxWidth = menuRight - menuLeft;
    final int maxHeight = menuBottom - menuTop;

    int nextX;
    int nextY;

    switch (expandDirection) {
      case EXPAND_UP:
        nextY = menuBottom;
        for (int i = 0; i < count; i++) {
          View child = getChildAt(i);
          if (child.getVisibility() == GONE) return;

          childLeft = maxWidth / 2 - child.getMeasuredWidth() / 2;
          childTop = nextY - child.getMeasuredHeight();

          layoutChild(child, maxHeight, maxWidth, childLeft, childTop);

          nextY -= childTop - buttonSpacing;
        }
        break;
      case EXPAND_DOWN:
        nextY = menuTop;
        for (int i = 0; i < count; i++) {
          View child = getChildAt(i);
          if (child.getVisibility() == GONE) return;

          childLeft = maxWidth / 2 - child.getMeasuredWidth() / 2;
          childTop = nextY;

          layoutChild(child, maxHeight, maxWidth, childLeft, childTop);

          nextY += childTop + child.getMeasuredHeight() + buttonSpacing;
        }
        break;
      case EXPAND_LEFT:
        nextX = menuRight;
        for (int i = 0; i < count; i++) {
          View child = getChildAt(i);
          if (child.getVisibility() == GONE) return;

          childLeft = nextX - child.getMeasuredWidth();
          childTop = maxHeight / 2 - child.getMeasuredHeight() / 2;

          layoutChild(child, maxHeight, maxWidth, childLeft, childTop);

          nextX -= childLeft - buttonSpacing;
        }
        break;
      case EXPAND_RIGHT:
        nextX = menuLeft;
        for (int i = 0; i < count; i++) {
          View child = getChildAt(i);
          if (child.getVisibility() == GONE) return;

          childLeft = nextX;
          childTop = maxHeight / 2 - child.getMeasuredHeight() / 2;

          layoutChild(child, maxHeight, maxWidth, childLeft, childTop);

          nextX += childLeft + child.getMeasuredWidth() + buttonSpacing;
        }
        break;
      case EXPAND_ROUND:
        /*
        double startAngle = Math.toRadians(0);
        double endAngle = Math.toRadians(360 - angleOverflow);

        // Prevent overlapping when it is a full circle
        int divisor;
        if (Math.abs(endAngle - startAngle) >= Math.toRadians(360) || count <= 1) {
          divisor = count;
        } else {
          divisor = count - 1;
        }

        double sweepAngle = Math.abs(endAngle - startAngle) / divisor;
        double nextAngle = startAngle;

        Point center = new Point();
        center.x = maxWidth / 2;
        center.y = maxHeight / 2;


        for (int i = 0; i < count; i++) {
          View child = getChildAt(i);
          if (child.getVisibility() == GONE) return;

          childLeft = (int)(Math.cos(nextAngle) * radius) + center.x;
          childTop = (int) -(Math.sin(nextAngle) * radius) + center.y;

          layoutChild(child, maxHeight, maxWidth, childLeft - child.getMeasuredWidth() / 2, childTop
              - menu.floatingActionMenuButton.getMeasuredHeight()
              - child.getMeasuredWidth() / 2);

          nextAngle += sweepAngle;
        }
        */
        break;
      case EXPAND_FAN:
        double startAngle = Math.toRadians(angleOverflow);
        double endAngle = Math.toRadians(180 - angleOverflow);
        Point center = new Point();
        center.y = maxHeight - menu.floatingActionMenuButton.getMeasuredHeight() / 2;

        switch (menu.horizontalAlignment) {
          case FloatingActionsMenu.ALIGNMENT_LEFT:
            startAngle = Math.toRadians(angleOverflow);
            endAngle = Math.toRadians(90 - angleOverflow);
            center.x = menu.floatingActionMenuButton.getMeasuredWidth() / 2;
            break;
          case FloatingActionsMenu.ALIGNMENT_RIGHT:
            startAngle = Math.toRadians(90 + angleOverflow);
            endAngle = Math.toRadians(180 - angleOverflow);
            center.x = maxWidth;
            break;
          case FloatingActionsMenu.ALIGNMENT_CENTER:
            startAngle = Math.toRadians(angleOverflow);
            endAngle = Math.toRadians(180 - angleOverflow);
            center.x = maxWidth / 2;
            break;
        }

        // Prevent overlapping when it is a full circle
        int divisor;
        if (Math.abs(endAngle - startAngle) >= Math.toRadians(360) || count <= 1) {
          divisor = count;
        } else {
          divisor = count - 1;
        }

        double sweepAngle = Math.abs(endAngle - startAngle) / divisor;
        double nextAngle = startAngle;

        for (int i = 0; i < count; i++) {
          View child = getChildAt(i);

          if (child.getVisibility() == GONE) return;

          childLeft = (int) (Math.cos(nextAngle) * radius) + center.x;
          childTop = (int) -(Math.sin(nextAngle) * radius) + center.y;

          layoutChild(child, maxHeight, maxWidth, childLeft - child.getMeasuredWidth() / 2,
              childTop - menu.floatingActionMenuButton.getMeasuredHeight() - child.getMeasuredWidth() / 2);


          nextAngle += sweepAngle;
        }

        break;
    }
  }

  private void layoutChild(final View child, int maxHeight, int maxWidth, final int childLeft,
      final int childTop) {
    //Get the maximum size of the child
    child.measure(MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST),
        MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST));
    final int childWidth = child.getMeasuredWidth();
    final int childHeight = child.getMeasuredHeight();

    child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
  }

  private void layoutChildWithAnim(final View child, int maxHeight, int maxWidth,
      final int childLeft, final int childTop) {
    //Get the maximum size of the child
    child.measure(MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST),
        MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST));
    final int childWidth = child.getMeasuredWidth();
    final int childHeight = child.getMeasuredHeight();

    if (!animationLaunched)  {
      setButtonAnimator(child, childLeft, childTop, childWidth, childHeight);
      ((FloatingActionButton) child).runAnimation();
      animationLaunched = true;
    }
  }
  //endregion

  private void setButtonAnimator(final View child, final int childLeft, final int childTop,
      final int childWidth, final int childHeight) {
    final int fromXDelta = menu.menuButtonCenter.x;
    final int fromYDelta = menu.menuButtonCenter.y;
    final int toXDelta = childLeft + childWidth / 2;
    final int toYDelta = childTop + childHeight / 2;

    ObjectAnimator translateXAnim = ObjectAnimator.ofFloat(child, "x", fromXDelta, toXDelta);
    translateXAnim.setDuration(1000);
    translateXAnim.setInterpolator(new BounceInterpolator());

    ObjectAnimator translateYAnim = ObjectAnimator.ofFloat(child, "y", fromYDelta, toYDelta);
    translateYAnim.setDuration(1000);
    translateYAnim.setInterpolator(new BounceInterpolator());

    AnimatorSet animatorSet = new AnimatorSet();
    animatorSet.play(translateXAnim).with(translateYAnim);
    animatorSet.addListener(new Animator.AnimatorListener() {
      @Override public void onAnimationStart(Animator animation) {
        Log.d("AnimationStart", "fromX:"
            + fromXDelta
            + " -- toX:"
            + toXDelta
            + " fromY:"
            + fromYDelta
            + " toY:"
            + toYDelta);
      }

      @Override public void onAnimationEnd(Animator animation) {
        Log.d("AnimationEnd", "fromX:"
            + fromXDelta
            + " -- toX:"
            + toXDelta
            + " fromY:"
            + fromYDelta
            + " toY:"
            + toYDelta);
        child.layout(toXDelta, toYDelta, toXDelta + childWidth, toYDelta + childHeight);
      }

      @Override public void onAnimationCancel(Animator animation) {

      }

      @Override public void onAnimationRepeat(Animator animation) {

      }
    });
  }

  public void setButtonAnimator(FloatingActionButton button, AnimatorSet animatorSet) {
    button.setAnimatorSet(animatorSet);
  }

  //region Add/Remove buttons
  public boolean addButton(FloatingActionButton floatingActionButton) {
    boolean added;
    if (this.floatingActionButtonItems == null) {
      this.floatingActionButtonItems = new ArrayList<>();
    }
    added = this.floatingActionButtonItems.add(floatingActionButton);
    if (added) addView(floatingActionButton);
    return added;
  }

  public void addButtons(List<FloatingActionButton> floatingActionButtonItems) {
    for (FloatingActionButton floatingActionButton : floatingActionButtonItems) {
      addButton(floatingActionButton);
    }
  }

  public boolean removeButton(FloatingActionButton floatingActionButton) {
    boolean removed = false;
    if (this.floatingActionButtonItems != null) {
      removeView(floatingActionButton);
      removed = this.floatingActionButtonItems.remove(floatingActionButton);
    }
    return removed;
  }
  //endregion

  //region Expand/Collapse submenu
  public void toggle() {
    if (!isVisible) {
      expand();
    } else {
      collapse();
    }
  }

  public void expand() {
    if (!isVisible) {
      isVisible = true;

      setVisibility(VISIBLE);

      if (submenuUpdateListener != null) {
        submenuUpdateListener.onMenuExpanded();
      }
    }
  }

  public void collapse() {
    if (isVisible) {
      isVisible = false;
      animationLaunched = false;

      setVisibility(GONE);

      if (submenuUpdateListener != null) {
        submenuUpdateListener.onMenuCollapsed();
      }
    }
  }

  public void show() {
    isVisible = false;
    expand();
  }

  public void hide() {
    isVisible = true;
    collapse();
  }
  //endregion

  public String getSubmenuGroup() {
    return submenuGroup;
  }

  public Drawable getSubmenuIcon() {
    return submenuIcon;
  }

  public boolean isEnableOverlay() {
    return enableOverlay;
  }

  public int getExpandDirection() {
    return expandDirection;
  }

  public void setOnFloatingActionSubmenuUpdateListener(
      OnFloatingActionSubmenuUpdateListener listener) {
    this.submenuUpdateListener = listener;
  }

  public interface OnFloatingActionSubmenuUpdateListener {
    void onMenuExpanded();

    void onMenuCollapsed();
  }
}

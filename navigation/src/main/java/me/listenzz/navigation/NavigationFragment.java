package me.listenzz.navigation;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * Created by Listen on 2018/1/11.
 */

public class NavigationFragment extends AwesomeFragment implements SwipeBackLayout.SwipeListener {

    private static final String SAVED_SWIPE_BACK_ENABLED = "swipe_back_enabled";

    private SwipeBackLayout swipeBackLayout;
    private boolean swipeBackEnabled = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            swipeBackEnabled = savedInstanceState.getBoolean(SAVED_SWIPE_BACK_ENABLED, false);
        }
        View root = inflater.inflate(R.layout.nav_fragment_navigation, container, false);
        swipeBackLayout = root.findViewById(R.id.navigation_content);
        swipeBackLayout.setEnableGesture(swipeBackEnabled);
        swipeBackLayout.addSwipeListener(this);
        return root;
    }

    public SwipeBackLayout getSwipeBackLayout() {
        return swipeBackLayout;
    }

    public void setSwipeBackEnabled(boolean enabled) {
        this.swipeBackEnabled = enabled;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_SWIPE_BACK_ENABLED, swipeBackEnabled);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState == null) {
            if (rootFragment == null) {
                throw new IllegalArgumentException("必须通过 `setRootFragment` 指定 rootFragment");
            } else {
                setRootFragmentInternal(rootFragment);
            }
        }
    }

    @Override
    public boolean isParentFragment() {
        return true;
    }

    @Override
    protected AwesomeFragment childFragmentForAppearance() {
        return getTopFragment();
    }

    @Override
    protected boolean onBackPressed() {
        FragmentManager fragmentManager = getChildFragmentManager();
        int count = fragmentManager.getBackStackEntryCount();
        if (count > 1) {
            AwesomeFragment topFragment = getTopFragment();
            if (topFragment.backInteractive()) {
                popFragment();
            }
            return true;
        } else {
            return super.onBackPressed();
        }
    }

    private AwesomeFragment rootFragment;

    public void setRootFragment(@NonNull final AwesomeFragment fragment) {
        if (isAdded()) {
            throw new IllegalStateException("NavigationFragment 已经出于 added 状态，不可以再设置 rootFragment");
        }
        this.rootFragment = fragment;
    }

    public AwesomeFragment getRootFragment() {
        FragmentManager fragmentManager = getChildFragmentManager();
        int count = fragmentManager.getBackStackEntryCount();
        if (count > 0) {
            FragmentManager.BackStackEntry backStackEntry = fragmentManager.getBackStackEntryAt(0);
            return (AwesomeFragment) fragmentManager.findFragmentByTag(backStackEntry.getName());
        }
        return null;
    }

    private void setRootFragmentInternal(AwesomeFragment fragment) {
       FragmentHelper.addFragmentToBackStack(getChildFragmentManager(), R.id.navigation_content, fragment, PresentAnimation.None);
    }

    public void pushFragment(@NonNull final AwesomeFragment fragment) {
        scheduleTaskAtStarted(new Runnable() {
            @Override
            public void run() {
                pushFragmentInternal(fragment);
            }
        });
    }

    private void pushFragmentInternal(AwesomeFragment fragment) {
        FragmentHelper.addFragmentToBackStack(getChildFragmentManager(), R.id.navigation_content, fragment, PresentAnimation.Push);
    }

    public void popToFragment(@NonNull final AwesomeFragment fragment) {
        scheduleTaskAtStarted(new Runnable() {
            @Override
            public void run() {
                popToFragmentInternal(fragment);
            }
        });
    }

    private void popToFragmentInternal(AwesomeFragment fragment) {
        AwesomeFragment topFragment = getTopFragment();
        if (topFragment == fragment) {
            return;
        }
        topFragment.setAnimation(PresentAnimation.Push);
        fragment.setAnimation(PresentAnimation.Push);

        fragment.onFragmentResult(topFragment.getRequestCode(), topFragment.getResultCode(), topFragment.getResultData());
        getChildFragmentManager().popBackStack(fragment.getSceneId(), 0);
    }

    public void popFragment() {
        AwesomeFragment after = FragmentHelper.getLatterFragment(getChildFragmentManager(), getTopFragment());
        if (after != null) {
            popToFragment(this);
            return;
        }

        AwesomeFragment before = FragmentHelper.getAheadFragment(getChildFragmentManager(), getTopFragment());
        if (before != null) {
            popToFragment(before);
        }
    }

    public void popToRootFragment() {
        AwesomeFragment awesomeFragment = getRootFragment();
        if (awesomeFragment != null) {
            popToFragment(getRootFragment());
        }
    }

    public void replaceFragment(@NonNull final AwesomeFragment fragment) {
        scheduleTaskAtStarted(new Runnable() {
            @Override
            public void run() {
                replaceFragmentInternal(fragment);
            }
        });
    }

    private void replaceFragmentInternal(AwesomeFragment fragment) {

        FragmentManager fragmentManager = getChildFragmentManager();
        fragmentManager.executePendingTransactions();

        AwesomeFragment topFragment = getTopFragment();
        AwesomeFragment aheadFragment = FragmentHelper.getAheadFragment(fragmentManager, topFragment);
        topFragment.setAnimation(PresentAnimation.Fade);
        if (aheadFragment != null) {
            aheadFragment.setAnimation(PresentAnimation.Fade);
        }
        fragmentManager.popBackStack();

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setReorderingAllowed(true);
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        if (aheadFragment != null) {
            transaction.hide(aheadFragment);
        }
        fragment.setAnimation(PresentAnimation.Fade);
        transaction.add(R.id.navigation_content, fragment, fragment.getSceneId());
        transaction.addToBackStack(fragment.getSceneId());
        transaction.commit();
    }

    public void replaceToRootFragment(@NonNull final AwesomeFragment fragment) {
        scheduleTaskAtStarted(new Runnable() {
            @Override
            public void run() {
                replaceRootFragmentInternal(fragment);
            }
        });
    }

    private void replaceRootFragmentInternal(AwesomeFragment fragment) {
        AwesomeFragment topFragment = getTopFragment();
        AwesomeFragment rootFragment = getRootFragment();
        topFragment.setAnimation(PresentAnimation.Fade);
        rootFragment.setAnimation(PresentAnimation.Fade);

        FragmentManager fragmentManager = getChildFragmentManager();
        fragmentManager.executePendingTransactions();
        fragmentManager.popBackStack(rootFragment.getSceneId(), FragmentManager.POP_BACK_STACK_INCLUSIVE);

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setReorderingAllowed(true);
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        fragment.setAnimation(PresentAnimation.Fade);
        transaction.add(R.id.navigation_content, fragment, fragment.getSceneId());
        transaction.addToBackStack(fragment.getSceneId());
        transaction.commit();
    }

    public void setChildFragments(List<AwesomeFragment> fragments) {
        // TODO
        // 弹出所有旧的 fragment

        // 添加所有新的 fragment
    }

    public AwesomeFragment getTopFragment() {
        if (isAdded()) {
            return (AwesomeFragment) getChildFragmentManager().findFragmentById(R.id.navigation_content);
        }
        return null;
    }


    @Nullable
    @Override
    public NavigationFragment getNavigationFragment() {
        checkNavigationFragment(this);
        return super.getNavigationFragment();
    }

    private void checkNavigationFragment(AwesomeFragment fragment) {
        AwesomeFragment parent = fragment.getParentAwesomeFragment();
        if (parent != null && !parent.getShowsDialog()) {
            if (parent instanceof NavigationFragment) {
                throw new IllegalStateException("should not nest NavigationFragment in the same presentation container");
            }
            checkNavigationFragment(parent);
        }
    }

    @Override
    public void onViewDragStateChanged(int state, float scrollPercent) {
        if (state == SwipeBackLayout.STATE_DRAGGING) {
            AwesomeFragment topFragment = getTopFragment();
            AwesomeFragment before = FragmentHelper.getAheadFragment(getChildFragmentManager(), topFragment);
            if (before != null && before.getView() != null) {
                before.getView().setVisibility(View.VISIBLE);
            }
        } else if (state == SwipeBackLayout.STATE_IDLE) {
            swipeBackLayout.requestLayout();
            AwesomeFragment topFragment = getTopFragment();
            AwesomeFragment before = FragmentHelper.getAheadFragment(getChildFragmentManager(), topFragment);
            if (before != null && before.getView() != null) {
               before.getView().setVisibility(View.GONE);
            }
            if (before != null && scrollPercent >= 1.0f) {
                topFragment.setAnimation(PresentAnimation.None);
                before.setAnimation(PresentAnimation.None);
                before.onFragmentResult(topFragment.getRequestCode(), topFragment.getResultCode(), topFragment.getResultData());
                getChildFragmentManager().popBackStackImmediate(before.getSceneId(), 0);
            }
        }
    }
}

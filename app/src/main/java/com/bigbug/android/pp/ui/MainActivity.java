package com.bigbug.android.pp.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.SimpleOnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.bigbug.android.pp.Config;
import com.bigbug.android.pp.R;
import com.bigbug.android.pp.ui.widget.FragmentPagerAdapter;
import com.bigbug.android.pp.util.HelpUtils;
import com.bigbug.android.pp.util.LogUtils;
import com.localytics.android.Localytics;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static com.bigbug.android.pp.util.LogUtils.LOGD;


public class MainActivity extends BaseActivity implements TabLayout.OnTabSelectedListener {

    private static final String TAG = LogUtils.makeLogTag(MainActivity.class);

    private TabLayout mTabLayout;
    private ViewPager mViewPager;

    private Menu mMenu;

    private final static String SELECTED_TAB = "selected_tab";

    final int[] TAB_NAMES = new int[] {
            R.string.tab_name_prayer,
            R.string.tab_name_partner,
            R.string.tab_name_history,
    };
    final int[] TAB_ICONS = new int[] {
            R.drawable.ic_tab_prayer,
            R.drawable.ic_tab_partner,
            R.drawable.ic_tab_history,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActionBar ab = getSupportActionBar();
        ab.setLogo(R.drawable.logo_with_right_margin);

        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        populateViewPager();
        mTabLayout = (TabLayout) findViewById(R.id.tabs);
        populateTabViews(mViewPager);

        // Build the binding between view pager and tab layout.
        mTabLayout.setOnTabSelectedListener(this);
        mViewPager.addOnPageChangeListener(new MyOnPageChangeListener(mTabLayout));

        if (savedInstanceState != null) {
            mViewPager.setCurrentItem(savedInstanceState.getInt(SELECTED_TAB));
        }

        Localytics.registerPush(Config.GCM_SENDER_ID);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        int position = mViewPager.getCurrentItem();
        ViewPagerAdapter adapter = (ViewPagerAdapter) mViewPager.getAdapter();

        // Update the data in the current fragment.
        // Fragment attaches the activity after the activity is created, it means
        // when we call initLoader or restartLoader in fragment's onCreate, the fragment
        // already has the valid begin date and end date.
        for (int i = 0; i < adapter.getCount(); ++i) {
            Fragment fragment = adapter.getItem(i);
            if (fragment instanceof AppFragment) {
                AppFragment appFragment = (AppFragment) fragment;
            }
        }

        // Switch to the current fragment
        mTabLayout.getTabAt(position).select();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTED_TAB, mViewPager.getCurrentItem());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_tracker, menu);

        // Cache the menu so we can configure the menu item in onOptionsItemSelected.
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
//            case R.id.action_refresh: {
//                item.setVisible(false);
//                mMenu.findItem(R.id.action_clear).setVisible(true);
//                return true;
//            }
            case R.id.action_about: {
                HelpUtils.showAbout(this);
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * This is the same as {@link #onSaveInstanceState} but is called for activities
     * created with the attribute {@link android.R.attr#persistableMode} set to
     * <code>persistAcrossReboots</code>. The {@link PersistableBundle} passed
     * in will be saved and presented in {@link #onCreate(Bundle, PersistableBundle)}
     * the first time that this activity is restarted following the next device reboot.
     *
     * @param outState           Bundle in which to place your saved state.
     * @param outPersistentState State which will be saved across reboots.
     * @see #onSaveInstanceState(Bundle)
     * @see #onCreate
     * @see #onRestoreInstanceState(Bundle, PersistableBundle)
     * @see #onPause
     */
    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
    }

    /**
     * Adding custom view to tab
     */
    private void populateTabViews(ViewPager viewPager) {
        mTabLayout.setupWithViewPager(viewPager);
        for (int i = 0; i < mTabLayout.getTabCount(); ++i) {
            TextView view = (TextView) LayoutInflater.from(this).inflate(R.layout.app_tab, null);
            view.setText(TAB_NAMES[i]);
            view.setCompoundDrawablesWithIntrinsicBounds(0, TAB_ICONS[i], 0, 0);
            mTabLayout.getTabAt(i).setCustomView(view);
        }
    }

    /**
     * Adding fragments to ViewPager
     */
    private void populateViewPager() {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getFragmentManager());
        adapter.addFragment(new PrayerFragment(), getString(TAB_NAMES[0]));
        adapter.addFragment(new PartnerFragment(), getString(TAB_NAMES[1]));
        adapter.addFragment(new HistoryFragment(), getString(TAB_NAMES[2]));
        mViewPager.setAdapter(adapter);
    }

    /**
     * Called when a tab enters the selected state.
     *
     * @param tab The tab that was selected
     */
    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        LOGD(TAG, String.format("tab %d is selected", tab.getPosition()));
        selectTab(tab);
    }

   /**
     * Called when a tab exits the selected state.
     *
     * @param tab The tab that was unselected
     */
    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
        LOGD(TAG, String.format("tab %d is unselected", tab.getPosition()));
    }

    /**
     * Called when a tab that is already selected is chosen again by the user. Some applications
     * may use this action to return to the top level of a category.
     *
     * @param tab The tab that was reselected.
     */
    @Override
    public void onTabReselected(TabLayout.Tab tab) {
        LOGD(TAG, String.format("tab %d is reselected", tab.getPosition()));
        selectTab(tab);
    }

    private void selectTab(TabLayout.Tab tab) {
        int colorAccent = ContextCompat.getColor(this, R.color.colorAccent);
        int colorOrigin = ContextCompat.getColor(this, R.color.tab_text_color);

        // Clear the color of the unselected tab views
        for (int i = 0; i < mTabLayout.getTabCount(); ++i) {
            if (i == tab.getPosition()) {
                continue;
            }
            TextView view = (TextView) mTabLayout.getTabAt(i).getCustomView();
            if (view != null) {
                view.setTextColor(colorOrigin);
                Drawable[] drawables = view.getCompoundDrawables();
                if (drawables[1] != null) {
                    drawables[1].setColorFilter(null);
                }
            }
        }

        // We can either select the page by scrolling the view pager or by clicking the tab view.
        // For the later case the event generates from the tab layout, so view pager must be notified.
        // Stop using smooth scroll is necessary otherwise onPageChanged will still be called during
        // the settling state even after the unselected tabs are cleared their color.
        mViewPager.setCurrentItem(tab.getPosition(), false);

        // Update the color of the selected tab view
        TextView view = (TextView) tab.getCustomView();
        view.setTextColor(colorAccent);
        Drawable[] drawables = view.getCompoundDrawables();
        if (drawables[1] != null) {
            drawables[1].setColorFilter(colorAccent, PorterDuff.Mode.SRC_ATOP);
        }

        ViewPagerAdapter adapter = (ViewPagerAdapter) mViewPager.getAdapter();
        Fragment fragment = adapter.getItem(tab.getPosition());
    }

    private static class MyOnPageChangeListener extends SimpleOnPageChangeListener {
        private WeakReference<TabLayout> mTabLayoutRef;
        private Bitmap mCachedBitmap;
        private Canvas mCachedCanvas;

        public MyOnPageChangeListener(TabLayout tabLayout) {
            mTabLayoutRef = new WeakReference<>(tabLayout);
            mCachedBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            mCachedCanvas = new Canvas(mCachedBitmap);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            final TabLayout tabLayout = mTabLayoutRef.get();
            if (tabLayout != null) {
                final TextView selectedView = (TextView) tabLayout.getTabAt(position).getCustomView();
                int colorNormal = ContextCompat.getColor(tabLayout.getContext(), R.color.tab_text_color);
                int colorAccent = ContextCompat.getColor(tabLayout.getContext(), R.color.colorAccent);

                if (selectedView != null && selectedView.getWidth() > 0) {

                    if (positionOffset > 0f && position < tabLayout.getTabCount() - 1) {
                        TextView nextView = (TextView) tabLayout.getTabAt(position + 1).getCustomView();

                        int selectedColor = ColorUtils.setAlphaComponent(colorAccent, Math.round(255 * (1 - positionOffset)));
                        int nextColor = ColorUtils.setAlphaComponent(colorAccent, Math.round(255 * positionOffset));

                        // Update text and color of the selected tab
                        selectedView.setTextColor(getFilteredTextColor(colorNormal, selectedColor));
                        nextView.setTextColor(getFilteredTextColor(colorNormal, nextColor));
                        Drawable[] drawables = selectedView.getCompoundDrawables();
                        drawables[1].setColorFilter(selectedColor, PorterDuff.Mode.SRC_ATOP);
                        drawables = nextView.getCompoundDrawables();
                        drawables[1].setColorFilter(nextColor, PorterDuff.Mode.SRC_ATOP);
                    }
                }
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            LOGD(TAG, "state = " + state);
        }

        // The crazy hack to apply color filter to the text color.
        private int getFilteredTextColor(int textColor, int filterColor) {
            mCachedCanvas.drawColor(textColor);
            mCachedCanvas.drawColor(filterColor, PorterDuff.Mode.SRC_ATOP);
            return mCachedBitmap.getPixel(0, 0);
        }
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragments = new ArrayList<>();
        private final List<String> mFragmentTitles = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragments.add(fragment);
            mFragmentTitles.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitles.get(position);
        }
    }
}
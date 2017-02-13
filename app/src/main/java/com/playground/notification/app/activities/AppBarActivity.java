package com.playground.notification.app.activities;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import com.playground.notification.R;
import com.playground.notification.databinding.AppBarLayoutBinding;
import com.playground.notification.sync.FavoriteManager;
import com.playground.notification.sync.MyLocationManager;
import com.playground.notification.sync.NearRingManager;


public abstract class AppBarActivity extends AppActivity {

	private static final @LayoutRes int LAYOUT = R.layout.activity_appbar;
	private AppBarLayoutBinding mBinding;
	private ActionBarDrawerToggle mDrawerToggle;


	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mBinding = DataBindingUtil.setContentView(this, LAYOUT);
		setupMain();
		initDrawer();
		setupContent(mBinding.appbarContent);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mDrawerToggle != null) {
			mDrawerToggle.syncState();
		}
	}

	/**
	 * Initialize the navigation drawer.
	 */
	private void initDrawer() {
		mDrawerToggle = new ActionBarDrawerToggle(this, mBinding.drawerLayout, R.string.application_name, R.string.app_name) {
			@Override
			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
//				updateDrawerMenuItem(R.id.action_favorite, R.string.action_favorite, FavoriteManager.getInstance());
//				updateDrawerMenuItem(R.id.action_near_ring, R.string.action_near_ring, NearRingManager.getInstance());
//				updateDrawerMenuItem(R.id.action_my_location_list, R.string.action_my_location_list, MyLocationManager.getInstance());
			}
		};
		mBinding.drawerLayout.addDrawerListener(mDrawerToggle);
	}



	private void setupMain() {
		setSupportActionBar(mBinding.appbar.toolbar);
		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayShowHomeEnabled(true);
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	protected abstract void setupContent(@NonNull FrameLayout contentLayout);

	protected final void setupFragment(@IdRes int container, @NonNull Fragment fragment) {
		getSupportFragmentManager().beginTransaction()
		                           .replace(container, fragment)
		                           .commit();
	}


	protected final void setupFragment(@NonNull Fragment fragment) {
		setupFragment(R.id.appbar_content, fragment);
	}


	protected void addViewToCoordinatorLayout(@NonNull View addView) {
		mBinding.errorContent.addView(addView);
	}

	protected AppBarLayoutBinding getBinding() {
		return mBinding;
	}

	protected void showShortSnackbar(@StringRes int message, @StringRes int buttonLabel, @NonNull View.OnClickListener clickListener) {
		Snackbar.make(mBinding.errorContent, message, Snackbar.LENGTH_SHORT)
		        .setAction(buttonLabel, clickListener)
		        .show();
	}

	protected void showShortSnackbar(@StringRes int message) {
		Snackbar.make(mBinding.errorContent, message, Snackbar.LENGTH_SHORT)
		        .show();
	}


	protected void showLongSnackbar(@StringRes int message, @StringRes int buttonLabel, @NonNull View.OnClickListener clickListener) {
		Snackbar.make(mBinding.errorContent, message, Snackbar.LENGTH_LONG)
		        .setAction(buttonLabel, clickListener)
		        .show();
	}

	protected void showLongSnackbar(@StringRes int message) {
		Snackbar.make(mBinding.errorContent, message, Snackbar.LENGTH_LONG)
		        .show();
	}


	protected void showIndefiniteSnackbar(@StringRes int message, @StringRes int buttonLabel, @NonNull View.OnClickListener clickListener) {
		Snackbar.make(mBinding.errorContent, message, Snackbar.LENGTH_INDEFINITE)
		        .setAction(buttonLabel, clickListener)
		        .show();
	}

	protected void showIndefiniteSnackbar(@StringRes int message) {
		Snackbar.make(mBinding.errorContent, message, Snackbar.LENGTH_LONG)
		        .show();
	}

}

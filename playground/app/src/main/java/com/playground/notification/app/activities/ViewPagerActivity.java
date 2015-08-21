package com.playground.notification.app.activities;

import java.io.Serializable;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.ViewGroup;

import com.google.android.gms.maps.model.LatLng;
import com.playground.notification.R;
import com.playground.notification.app.adapters.SyncPlaygroundsPagerAdapter;
import com.playground.notification.databinding.ViewPagerBinding;
import com.playground.notification.ds.sync.SyncPlayground;

/**
 * A viewpager showing all sync-grounds, it might be list of favorite or near-rings.
 *
 * @author Xinyue Zhao
 */
public final class ViewPagerActivity extends AppActivity implements OnPageChangeListener {

	/**
	 * Main layout for this component.
	 */
	private static final int LAYOUT = R.layout.activity_viewpager;

	private static final String EXTRAS_LAT = ViewPagerActivity.class.getName() + ".EXTRAS.lat";
	private static final String EXTRAS_LNG = ViewPagerActivity.class.getName() + ".EXTRAS.lng";
	private static final String EXTRAS_LIST = ViewPagerActivity.class.getName() + ".EXTRAS.list";

	/**
	 * Data-binding.
	 */
	private ViewPagerBinding mBinding;

	//From
	private double mLat;
	private double mLng;
	//List of data to show.
	private List<? extends SyncPlayground> mGrounds;

	/**
	 * Show single instance of {@link}
	 *
	 * @param cxt
	 * 		{@link Activity}.
	 * @param grounds
	 * 		Data to show.
	 */
	public static void showInstance(Activity cxt, double fromLat, double fromLng,
			List<? extends SyncPlayground> grounds) {
		Intent intent = new Intent(cxt, ViewPagerActivity.class);
		intent.putExtra(EXTRAS_LAT, fromLat);
		intent.putExtra(EXTRAS_LNG, fromLng);
		intent.putExtra(EXTRAS_LIST, (Serializable) grounds);
		intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		ActivityCompat.startActivity(cxt, intent, null);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Init data-binding.
		mBinding = DataBindingUtil.setContentView(this, LAYOUT);
		//Init application basic elements.
		setUpErrorHandling((ViewGroup) findViewById(R.id.error_content));

		//Init data.
		if (savedInstanceState == null) {
			Intent intent = getIntent();
			mLat = intent.getDoubleExtra(EXTRAS_LAT, 0f);
			mLng = intent.getDoubleExtra(EXTRAS_LNG, 0f);
			mGrounds = (List<? extends SyncPlayground>) intent.getSerializableExtra(EXTRAS_LIST);
		} else {
			mLat = savedInstanceState.getDouble(EXTRAS_LAT);
			mLng = savedInstanceState.getDouble(EXTRAS_LNG);
			mGrounds = (List<? extends SyncPlayground>) savedInstanceState.getSerializable(EXTRAS_LIST);
		}

		updateIndicator(0);
		mBinding.vp.setAdapter(new SyncPlaygroundsPagerAdapter(new LatLng(mLat, mLng), mGrounds,
				getSupportFragmentManager()));
		mBinding.vp.addOnPageChangeListener(this);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putDouble(EXTRAS_LAT, mLat);
		outState.putDouble(EXTRAS_LNG, mLng);
		outState.putSerializable(EXTRAS_LIST, (Serializable) mGrounds);
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

	}

	@Override
	public void onPageSelected(int position) {
		updateIndicator(position);
	}


	@Override
	public void onPageScrollStateChanged(int state) {

	}

	private void updateIndicator(int position) {
		mBinding.pagerIndicatorTv.setText(String.format("%d/%d", position + 1, mGrounds.size()));
	}
}
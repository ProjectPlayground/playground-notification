package com.playground.notification.app.activities;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;

import com.chopping.bus.CloseDrawerEvent;
import com.chopping.utils.Utils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationChangeListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.nineoldandroids.view.ViewHelper;
import com.nineoldandroids.view.ViewPropertyAnimator;
import com.playground.notification.R;
import com.playground.notification.api.Api;
import com.playground.notification.api.ApiNotInitializedException;
import com.playground.notification.app.App;
import com.playground.notification.app.fragments.AboutDialogFragment;
import com.playground.notification.app.fragments.AppListImpFragment;
import com.playground.notification.app.fragments.GPlusFragment;
import com.playground.notification.app.fragments.MyLocationFragment;
import com.playground.notification.app.fragments.PlaygroundDetailFragment;
import com.playground.notification.bus.EULAConfirmedEvent;
import com.playground.notification.bus.EULARejectEvent;
import com.playground.notification.databinding.ActivityMapsBinding;
import com.playground.notification.ds.grounds.Playground;
import com.playground.notification.ds.grounds.Playgrounds;
import com.playground.notification.ds.grounds.Request;
import com.playground.notification.ds.sync.Favorite;
import com.playground.notification.ds.sync.MyLocation;
import com.playground.notification.ds.sync.NearRing;
import com.playground.notification.ds.weather.Weather;
import com.playground.notification.ds.weather.WeatherDetail;
import com.playground.notification.sync.FavoriteManager;
import com.playground.notification.sync.MyLocationManager;
import com.playground.notification.sync.NearRingManager;
import com.playground.notification.sync.SyncManager;
import com.playground.notification.utils.Prefs;
import com.playground.notification.views.TouchableMapFragment;
import com.squareup.picasso.Picasso;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;

public class MapsActivity extends AppActivity implements LocationListener {
	public static final String EXTRAS_GROUND = MapsActivity.class.getName() + ".EXTRAS.ground";

	private static final int REQ = 0x98;

	/**
	 * Main layout for this component.
	 */
	private static final int LAYOUT = R.layout.activity_maps;
	/**
	 * View's menu.
	 */
	private static final int MENU = R.menu.menu_main;

	private GoogleMap mMap; // Might be null if Google Play services APK is not available.
	private TouchableMapFragment mMapFragment;
	/**
	 * {@code true}: force to load markers, ignore the touch&move effect of map. Default is {@code true}, because as
	 * initializing the map, the markers should be loaded.
	 */
	private volatile boolean mForcedToLoad = true;
	/**
	 * Use navigation-drawer for this fork.
	 */
	private ActionBarDrawerToggle mDrawerToggle;
	/**
	 * Navigation drawer.
	 */
	private DrawerLayout mDrawerLayout;
	/**
	 * Data-binding.
	 */
	private ActivityMapsBinding mBinding;
	/**
	 * Ask current location.
	 */
	private LocationRequest mLocationRequest;
	/**
	 * Connect to google-api.
	 */
	private GoogleApiClient mGoogleApiClient;
	/**
	 * Current shown markers.
	 */
	private Map<Marker, Playground> mMarkerList = new LinkedHashMap<>();
	/**
	 * {@code true} if the map is forground.
	 */
	private boolean mVisible;

	//------------------------------------------------
	//Subscribes, event-handlers
	//------------------------------------------------

	/**
	 * Handler for {@link com.chopping.bus.CloseDrawerEvent}.
	 *
	 * @param e
	 * 		Event {@link com.chopping.bus.CloseDrawerEvent}.
	 */
	public void onEvent(CloseDrawerEvent e) {
		mDrawerLayout.closeDrawers();
	}


	/**
	 * Handler for {@link  EULARejectEvent}.
	 *
	 * @param e
	 * 		Event {@link  EULARejectEvent}.
	 */
	public void onEvent(EULARejectEvent e) {
		finish();
	}

	/**
	 * Handler for {@link  EULAConfirmedEvent}.
	 *
	 * @param e
	 * 		Event {@link  EULAConfirmedEvent}.
	 */
	public void onEvent(EULAConfirmedEvent e) {
		ConnectGoogleActivity.showInstance(this);
	}


	//------------------------------------------------

	/**
	 * Show single instance of {@link MapsActivity}
	 *
	 * @param cxt {@link Activity}.
	 */
	public static void showInstance(Activity cxt, Playground ground ) {
		Intent intent = new Intent(cxt, MapsActivity.class);
		intent.putExtra(EXTRAS_GROUND, ground);
		intent.setFlags(FLAG_ACTIVITY_SINGLE_TOP | FLAG_ACTIVITY_CLEAR_TOP);
		ActivityCompat.startActivity(cxt, intent, null);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case ConnectGoogleActivity.REQ:
			if (resultCode == RESULT_OK) {
				//Return from google-login.
				//onYouCanUseApp();
			} else {
				ActivityCompat.finishAffinity(this);
			}
			break;
		case REQ:
			switch (resultCode) {
			case Activity.RESULT_OK:
				//				if (!mGoogleApiClient.isConnected()) {
				//					if (!mGoogleApiClient.isConnecting()) {
				//						mGoogleApiClient.connect();
				//					}
				//				}
				break;
			case Activity.RESULT_CANCELED:
				exitAppDialog();
				break;
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//Init data-binding.
		mBinding = DataBindingUtil.setContentView(this, LAYOUT);
		//Init application basic elements.
		setUpErrorHandling((ViewGroup) findViewById(R.id.error_content));

		initDrawer();
		initBoard();
		initAddFunctions();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
		if(intent.getSerializableExtra(EXTRAS_GROUND) != null) {
			Playground playground = (Playground) intent.getSerializableExtra(EXTRAS_GROUND);
			LatLng to = new LatLng(playground.getLatitude(),
					playground.getLongitude());
			CameraUpdate update = CameraUpdateFactory.newLatLngZoom(to, 16);
			if(playground instanceof MyLocation) {
				MarkerOptions options = new MarkerOptions().position(to);
				options.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_saved_ground));
				mMap.addMarker(options);
			} else if(playground instanceof Favorite) {
				MarkerOptions options = new MarkerOptions().position(to);
				options.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_favorited));
				mMap.addMarker(options);
			} else if(playground instanceof NearRing) {
				mMap.addCircle(new CircleOptions().center(new LatLng(playground.getLatitude(),
						playground.getLongitude())).radius(Prefs.getInstance().getAlarmArea()).strokeWidth(1)
						.strokeColor(Color.BLUE).fillColor(getResources().getColor(R.color.common_blue_50)));
				MarkerOptions options = new MarkerOptions().position(to);
				mMap.addMarker(options);
			}
			mMap.moveCamera(update);
		}
	}

	/**
	 * Define UI for add my-location.
	 */
	private void initAddFunctions() {
		mBinding.addPaneV.hide();
		mBinding.exitAddBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mBinding.currentBtn.setVisibility(View.GONE);
				mBinding.addBtn.show();
				mBinding.addPaneV.hide();
			}
		});
		mBinding.addBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mBinding.currentBtn.setVisibility(View.VISIBLE);
				mBinding.addBtn.hide();
				mBinding.addPaneV.show();
			}
		});
		mBinding.currentBtn.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				Location location = App.Instance.getCurrentLocation();
				if (location == null) {
					mBinding.currentBtn.setVisibility(View.GONE);
					mBinding.addBtn.show();
					mBinding.addPaneV.hide();
					Snackbar.make(mBinding.drawerLayout, R.string.lbl_no_current_location, Snackbar.LENGTH_LONG).show();
					return true;
				}
				final LatLng center = mMap.getProjection().getVisibleRegion().latLngBounds.getCenter();
				showDialogFragment(MyLocationFragment.newInstance(App.Instance, location.getLatitude(),
						location.getLongitude(), new Playground(center.latitude, center.longitude), false), null);
				return true;
			}
		});
	}

	/**
	 * Ready to use application.
	 */
	private void initUseApp() {
		//User that have used this application and done clear(logout), should go back to login-page.
		Prefs prefs = Prefs.getInstance();
		if (prefs.isEULAOnceConfirmed() && TextUtils.isEmpty(prefs.getGoogleId())) {
			ConnectGoogleActivity.showInstance(this);
		} else if (prefs.isEULAOnceConfirmed() && !TextUtils.isEmpty(prefs.getGoogleId())) {
			onYouCanUseApp();
		}
	}

	/**
	 * Callback for available using of application.
	 */
	private void onYouCanUseApp() {
		initGoogle();
		populateGrounds();
		FavoriteManager.getInstance().init();
		NearRingManager.getInstance().init();
		MyLocationManager.getInstance().init();
		initDrawerContent();
		//Ask weather
		if (App.Instance.getCurrentLocation() != null) {
			Location location = App.Instance.getCurrentLocation();
			try {
				Api.getWeather(location.getLatitude(), location.getLongitude(), Locale.getDefault().getLanguage(),
						"metric", new Callback<Weather>() {
							@Override
							public void success(Weather weather, Response response) {
								mBinding.boardVg.setVisibility(View.VISIBLE);
								WeatherDetail weatherDetail = weather.getDetails().get(0);
								String temp =  getString(R.string.lbl_c, weather.getTemperature().getValue());
								String weatherInfo = String.format("%s", temp );
								mBinding.boardTv.setText(weatherInfo);
								String url = String.format(Prefs.getInstance().getWeahterIconUrl(weatherDetail.getIcon()));
								Picasso.with(App.Instance).load(url).into(mBinding.boardIconIv);

								ViewPropertyAnimator animator = ViewPropertyAnimator.animate(mBinding.boardVg);
								float x = ViewHelper.getX(mBinding.boardVg);
								float y = ViewHelper.getY(mBinding.boardVg);
								ViewHelper.setPivotX(mBinding.boardVg, x / 2);
								ViewHelper.setPivotY(mBinding.boardVg, y / 2);
								animator.rotation(-5f).rotation(5f).setDuration(500) .start();
							}

							@Override
							public void failure(RetrofitError error) {

							}
						});
			} catch (ApiNotInitializedException e) {
				//Ignore this request.
			}
		}
	}

	/**
	 * Initialize information board.
	 */
	private void initBoard() {
		mBinding.boardVg.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				v.setVisibility(View.GONE);
			}
		});
	}

	/**
	 * Initialize all map infrastructures, location request etc.
	 */
	private void initGoogle() {
		setUpMapIfNeeded();
		if (mMap != null) {
			mapSettings();
		}

		//Location request.
		if (mLocationRequest == null) {
			mLocationRequest = LocationRequest.create();
			mLocationRequest.setInterval(AlarmManager.INTERVAL_HALF_HOUR);
			mLocationRequest.setFastestInterval(AlarmManager.INTERVAL_FIFTEEN_MINUTES);
			int ty = 0;
			switch (Prefs.getInstance().getBatteryLifeType()) {
			case "0":
				ty = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
				break;
			case "1":
				ty = LocationRequest.PRIORITY_HIGH_ACCURACY;
				break;
			case "2":
				ty = LocationRequest.PRIORITY_LOW_POWER;
				break;
			case "3":
				ty = LocationRequest.PRIORITY_NO_POWER;
				break;
			}
			mLocationRequest.setPriority(ty);
		}

		if (mGoogleApiClient == null) {
			mGoogleApiClient = new GoogleApiClient.Builder(App.Instance).addApi(LocationServices.API)
					.addConnectionCallbacks(new ConnectionCallbacks() {
						@Override
						public void onConnected(Bundle bundle) {
							startLocationUpdate();
						}

						@Override
						public void onConnectionSuspended(int i) {
							Utils.showShortToast(App.Instance, "onConnectionSuspended");

						}
					}).addOnConnectionFailedListener(new OnConnectionFailedListener() {
						@Override
						public void onConnectionFailed(ConnectionResult connectionResult) {
							Utils.showShortToast(App.Instance,
									"onConnectionFailed: " + connectionResult.getErrorCode());
						}
					}).build();

			mGoogleApiClient.connect();
		}

		//Setting turn/off location service of system.
		LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(
				mLocationRequest);
		builder.setAlwaysShow(true);
		builder.setNeedBle(true);
		PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(
				mGoogleApiClient, builder.build());
		result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
			@Override
			public void onResult(LocationSettingsResult result) {
				final Status status = result.getStatus();
				//				final LocationSettingsStates states = result.getLocationSettingsStates();
				switch (status.getStatusCode()) {
				case LocationSettingsStatusCodes.SUCCESS:
					break;
				case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
					try {
						status.startResolutionForResult(MapsActivity.this, REQ);
					} catch (SendIntentException e) {
						exitAppDialog();
					}
					break;
				case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
					exitAppDialog();
					break;
				}
			}
		});
	}

	/**
	 * Locating begin.
	 */
	private void startLocationUpdate() {
		if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
			LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
		}
	}


	@Override
	public void onLocationChanged(Location location) {
		App.Instance.setCurrentLocation(location);
		Log.d("pg:location", "method: onLocationChanged -> mCurrentLocation changed");
		movedToUpdatedLocation(location);
	}

	/**
	 * Stop locating.
	 */
	protected void stopLocationUpdates() {
		if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
			LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
		}
	}

	@Override
	protected void onDestroy() {
		stopLocationUpdates();
		if (mMap != null) {
			mMap.clear();
			mMarkerList.clear();
			mMap = null;
		}
		if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
			mGoogleApiClient.disconnect();
		}
		super.onDestroy();
	}

	private AlertDialog mExitAppDlg;

	/**
	 * Force to exit application for no location-service.
	 */
	private void exitAppDialog() {
		mExitAppDlg = new AlertDialog.Builder(MapsActivity.this).setCancelable(false).setTitle(R.string.application_name).setMessage(
				R.string.lbl_no_location_service).setPositiveButton(R.string.btn_confirm,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						ActivityCompat.finishAfterTransition(MapsActivity.this);
					}
				}).create();
		mExitAppDlg.show();
	}


	/**
	 * Initialize the navigation drawer.
	 */
	private void initDrawer() {
		setSupportActionBar(mBinding.toolbar);
		ActionBar actionBar = getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.application_name, R.string.app_name) {
			@Override
			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
				updateDrawerMenuItem(R.id.action_favorite, R.string.action_favorite, FavoriteManager.getInstance());
				updateDrawerMenuItem(R.id.action_near_ring, R.string.action_near_ring, NearRingManager.getInstance());
				updateDrawerMenuItem(R.id.action_my_location_list, R.string.action_my_location_list,
						MyLocationManager.getInstance());
			}
		};
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		//Navi-head
		getSupportFragmentManager().beginTransaction().replace(R.id.gplus_container, GPlusFragment.newInstance(
				getApplication())).commit();
	}

	/**
	 * Helper to update menu-titles on drawer.
	 */
	private void updateDrawerMenuItem(int itemResId, int itemTitleResId, SyncManager mgr) {
		if (mgr.isInit()) {
			mBinding.navView.getMenu().findItem(itemResId).setTitle(getString(itemTitleResId,
					mgr.getCachedList().size()));
		} else {
			mBinding.navView.getMenu().findItem(itemResId).setTitle(getString(itemTitleResId, 0));
		}
	}

	@Override
	protected void onResume() {
		if (mCfgLoadDlg != null && mCfgLoadDlg.isShowing()) {
			mCfgLoadDlg.dismiss();
		}
		mCfgLoadDlg = ProgressDialog.show(this, getString(R.string.application_name), getString(R.string.lbl_load_cfg));

		super.onResume();

		if (mDrawerToggle != null) {
			mDrawerToggle.syncState();
		}
		setUpMapIfNeeded();
		mVisible = true;
	}

	@Override
	protected void onPause() {
		mVisible = false;
		super.onPause();
	}

	/**
	 * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly installed) and the
	 * map has not already been instantiated.. This will ensure that we only ever call {@link #setUpMap()} once when
	 * {@link #mMap} is not null.
	 * <p/>
	 * If it isn't installed {@link SupportMapFragment} (and {@link com.google.android.gms.maps.MapView MapView}) will
	 * show a prompt for the user to install/update the Google Play services APK on their device.
	 * <p/>
	 * A user can return to this FragmentActivity after following the prompt and correctly installing/updating/enabling
	 * the Google Play services. Since the FragmentActivity may not have been completely destroyed during this process
	 * (it is likely that it would only be stopped or paused), {@link #onCreate(Bundle)} may not be called again so we
	 * should call this method in {@link #onResume()} to guarantee that it will be called.
	 */
	private void setUpMapIfNeeded() {
		// Do a null check to confirm that we have not already instantiated the map.
		if (mMap == null) {
			// Try to obtain the map from the SupportMapFragment.
			mMap = (mMapFragment = (TouchableMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
					.getMap();
			// Check if we were successful in obtaining the map.
			if (mMap != null) {
				setUpMap();
			}
		}
	}

	/**
	 * This is where we can add markers or lines, add listeners or move the camera. In this case, we just add a marker
	 * near Africa.
	 * <p/>
	 * This should only be called once and when we are sure that {@link #mMap} is not null.
	 */
	private void setUpMap() {
		mMap.setMyLocationEnabled(true);

		mMap.setIndoorEnabled(true);
		mMap.setBuildingsEnabled(true);

		UiSettings uiSettings = mMap.getUiSettings();
		//		uiSettings.setZoomControlsEnabled(true);
		uiSettings.setMyLocationButtonEnabled(true);
		uiSettings.setIndoorLevelPickerEnabled(true);
		uiSettings.setCompassEnabled(true);
		uiSettings.setAllGesturesEnabled(true);


		mMap.setPadding(0, getAppBarHeight(), 0, 0);
		mMap.setOnMyLocationButtonClickListener(new OnMyLocationButtonClickListener() {
			@Override
			public boolean onMyLocationButtonClick() {
				mForcedToLoad = true;
				return false;
			}
		});
		mMap.setOnCameraChangeListener(new OnCameraChangeListener() {
			@Override
			public void onCameraChange(CameraPosition cameraPosition) {
				populateGrounds();
			}
		});
		mMap.setOnMyLocationChangeListener(new OnMyLocationChangeListener() {
			@Override
			public void onMyLocationChange(Location location) {
				App.Instance.setCurrentLocation(location);
				Log.d("pg:location", "method: onMyLocationChange -> mCurrentLocation changed");
			}
		});
	}

	/**
	 * Extra settings on map.
	 */
	private void mapSettings() {
		Prefs prefs = Prefs.getInstance();
		mMap.setTrafficEnabled(prefs.isTrafficShowing());
		mMap.setMapType(prefs.getMapType().equals("0") ? GoogleMap.MAP_TYPE_NORMAL : GoogleMap.MAP_TYPE_SATELLITE);
	}

	/**
	 * Draw grounds on map.
	 */
	private void populateGrounds() {
		if (mForcedToLoad || mMapFragment.isTouchAndMove()) {
			mForcedToLoad = false;
			mBinding.loadPinPb.setVisibility(View.VISIBLE);

			LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
			List<String> filter = new ArrayList<>();
			filter.add("playground");
			Request request = new Request();
			request.setTimestamps(System.currentTimeMillis());
			request.setWidth(App.Instance.getScreenSize().Width);
			request.setHeight(App.Instance.getScreenSize().Height);
			request.setFilter(filter);
			request.setResult(new ArrayList<String>());
			request.setNorth(bounds.northeast.latitude);
			request.setEast(bounds.northeast.longitude);
			request.setSouth(bounds.southwest.latitude);
			request.setWest(bounds.southwest.longitude);

			try {
				Api.getPlaygrounds(Prefs.getInstance().getApiSearch(), request, new Callback<Playgrounds>() {
					@Override
					public void success(Playgrounds playgrounds, Response response) {
						mBinding.loadPinPb.setVisibility(View.GONE);
						Location location = App.Instance.getCurrentLocation();
						if (location  == null) {
							Snackbar.make(mBinding.drawerLayout, R.string.lbl_no_current_location, Snackbar.LENGTH_LONG)
									.show();
							return;
						}
						mMarkerList.clear();
						if (mMap != null) {
							mMap.clear();
							final LatLng currentLatLng = new LatLng(location.getLatitude(),
									location.getLongitude());
							List<Playground> grounds = playgrounds.getPlaygroundList();
							for (final Playground ground : grounds) {
								LatLng to = new LatLng(ground.getLatitude(), ground.getLongitude());
								//Draw different markers, for fav , for normal ground, for grounds in near-rings.
								MarkerOptions options = new MarkerOptions().position(to);
								FavoriteManager favMgr = FavoriteManager.getInstance();
								if (favMgr.isInit() && favMgr.isCached(ground)) {
									options.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_favorited));
								} else {
									com.playground.notification.utils.Utils.changeMarkerIcon(options, currentLatLng, to);
								}
								NearRingManager nearRingMgr = NearRingManager.getInstance();
								if (nearRingMgr.isInit() && nearRingMgr.isCached(ground)) {
									mMap.addCircle(new CircleOptions().center(new LatLng(ground.getLatitude(),
											ground.getLongitude())).radius(Prefs.getInstance().getAlarmArea()).strokeWidth(
											1).strokeColor(Color.BLUE).fillColor(getResources().getColor(
											R.color.common_blue_50)));
								}
								mMarkerList.put(mMap.addMarker(options), ground);
							}

							MyLocationManager myLocMgr = MyLocationManager.getInstance();
							if (myLocMgr.isInit()) {
								for (MyLocation myLoc : myLocMgr.getCachedList()) {
									LatLng to = new LatLng(myLoc.getLatitude(), myLoc.getLongitude());
									MarkerOptions options = new MarkerOptions().position(to);
									options.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_saved_ground));
									mMarkerList.put(mMap.addMarker(options), myLoc);
								}
							}

							mMap.setOnMarkerClickListener(new OnMarkerClickListener() {
								@Override
								public boolean onMarkerClick(Marker marker) {
									for (Marker m : mMarkerList.keySet()) {
										if (m.equals(marker)) {
											showDialogFragment(PlaygroundDetailFragment.newInstance(App.Instance,
													currentLatLng.latitude, currentLatLng.longitude, mMarkerList.get(m), false),
													null);
											break;
										}
									}
									return true;
								}
							});
						}
					}

					@Override
					public void failure(RetrofitError error) {
						mBinding.loadPinPb.setVisibility(View.GONE);
					}
				});
			} catch (ApiNotInitializedException e) {
				//Ignore this request.
				mBinding.loadPinPb.setVisibility(View.GONE);
			}
		}
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(MENU, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(final Menu menu) {
		//Share application.
		MenuItem menuAppShare = menu.findItem(R.id.action_share_app);
		android.support.v7.widget.ShareActionProvider provider =
				(android.support.v7.widget.ShareActionProvider) MenuItemCompat.getActionProvider(menuAppShare);
		String subject = getString(R.string.lbl_share_app_title);
		String text = getString(R.string.lbl_share_app_content, getString(R.string.application_name),
				Prefs.getInstance().getAppDownloadInfo());
		provider.setShareIntent(Utils.getDefaultShareIntent(provider, subject, text));
		return super.onPrepareOptionsMenu(menu);
	}


	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

		switch (item.getItemId()) {
		case R.id.action_about:
			showDialogFragment(AboutDialogFragment.newInstance(this), null);
			break;

		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * An indicator dialog for loading app-config
	 */
	private ProgressDialog mCfgLoadDlg;

	@Override
	protected void onAppConfigLoaded() {
		super.onAppConfigLoaded();
		cfgFinished();
	}

	@Override
	protected void onAppConfigIgnored() {
		super.onAppConfigIgnored();
		cfgFinished();
	}

	private void cfgFinished() {
		if (mCfgLoadDlg != null && mCfgLoadDlg.isShowing()) {
			mCfgLoadDlg.dismiss();
		}
		if (mExitAppDlg != null && mExitAppDlg.isShowing()) {
			mExitAppDlg.dismiss();
		}
		Prefs prefs = Prefs.getInstance();
		if (!TextUtils.isEmpty(prefs.getApiHost())) {
			showAppList();
			Api.initialize(App.Instance, prefs.getApiHost(), prefs.getWeatherApiHost());
			initUseApp();
		} else  {
			exitAppDialog();
		}
	}

	/**
	 * Show all external applications links.
	 */
	private void showAppList() {
		getSupportFragmentManager().beginTransaction().replace(R.id.app_list_fl, AppListImpFragment.newInstance(this))
				.commit();
	}


	/**
	 * Update current position on the map.
	 *
	 * @param location
	 * 		Current location.
	 */
	private void movedToUpdatedLocation(Location location) {
		if (mMap != null && mVisible) {
			CameraUpdate update = CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),
					location.getLongitude()), 16);
			mMap.moveCamera(update);
			Log.d("pg:location", "method: movedToUpdatedLocation");
		}
	}


	/**
	 * Set-up of navi-bar left.
	 */
	private void initDrawerContent() {
		mBinding.navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
			@Override
			public boolean onNavigationItemSelected(MenuItem menuItem) {
				mBinding.drawerLayout.closeDrawer(Gravity.LEFT);

				if (mMap != null) {
					Location location = App.Instance.getCurrentLocation();
					double lat = location.getLatitude();
					double lng =  location.getLongitude();
					switch (menuItem.getItemId()) {
					case R.id.action_favorite:
						FavoriteManager favoriteManager = FavoriteManager.getInstance();
						if (favoriteManager.getCachedList().size() > 0) {
							ViewPagerActivity.showInstance(MapsActivity.this, lat, lng,
									favoriteManager.getCachedList(), getString(R.string.lbl_favorite_list));
						}
						break;
					case R.id.action_near_ring:
						NearRingManager nearRingManager = NearRingManager.getInstance();
						if (nearRingManager.getCachedList().size() > 0) {
							ViewPagerActivity.showInstance(MapsActivity.this, lat, lng,
									nearRingManager.getCachedList(), getString(R.string.lbl_near_ring_list));
						}
						break;
					case R.id.action_my_location_list:
						MyLocationManager myLocationManager = MyLocationManager.getInstance();
						if (myLocationManager.getCachedList().size() > 0) {
							MyLocationListActivity.showInstance(MapsActivity.this);
						}
						break;
					case R.id.action_settings:
						SettingsActivity.showInstance(MapsActivity.this);
						break;
					case R.id.action_more_apps:
						mBinding.drawerLayout.openDrawer(Gravity.RIGHT);
						break;
					case R.id.action_radar:
						com.playground.notification.utils.Utils.openExternalBrowser(MapsActivity.this,
								"http://" + getString(R.string.support_spielplatz_radar));
						break;
					}
				}
				return true;
			}
		});
	}
}

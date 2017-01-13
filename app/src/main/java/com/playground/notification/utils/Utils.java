package com.playground.notification.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.AudioManager;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.playground.notification.R;

/**
 * Util-methods of application.
 *
 * @author Xinyue Zhao
 */
public final class Utils {
	private static final String TAG = Utils.class.getName();

	/**
	 * Show different marker-icon for different distance.
	 *
	 * @param options
	 * 		The the option of a marker.
	 * @param center
	 * 		Current center position.
	 * @param to
	 * 		The ground position.
	 */
	public static void changeMarkerIcon( MarkerOptions options, LatLng center, LatLng to ) {
		synchronized( TAG ) {
			float[] results = new float[ 1 ];
			android.location.Location.distanceBetween( center.latitude, center.longitude, to.latitude, to.longitude, results );
			float distance = results[ 0 ];
			if( results.length > 0 ) {
				if( distance <= 100 ) {
					options.icon( BitmapDescriptorFactory.fromResource( R.drawable.ic_pin_100 ) );
				} else if( distance <= 200 && distance > 100 ) {
					options.icon( BitmapDescriptorFactory.fromResource( R.drawable.ic_pin_200 ) );
				} else if( distance <= 300 && distance > 200 ) {
					options.icon( BitmapDescriptorFactory.fromResource( R.drawable.ic_pin_300 ) );
				} else if( distance <= 400 && distance > 300 ) {
					options.icon( BitmapDescriptorFactory.fromResource( R.drawable.ic_pin_400 ) );
				} else {
					options.icon( BitmapDescriptorFactory.fromResource( R.drawable.ic_pin_500 ) );
				}
			} else {
				options.icon( BitmapDescriptorFactory.fromResource( R.drawable.ic_pin_500 ) );
			}
		}
	}


	/**
	 * Open a html page with external browser.
	 *
	 * @param cxt
	 * 		The {@link Context}.
	 * @param url
	 * 		The url to the site.
	 */
	public static void openExternalBrowser( Activity cxt, String url ) {
		Intent i = new Intent( Intent.ACTION_VIEW );
		i.setData( Uri.parse( url ) );
		ActivityCompat.startActivity( cxt, i, null );
	}

	/**
	 * Open Google's map to show two points.
	 *
	 * @param fromLatLng
	 * 		From point.
	 * @param toLatLng
	 * 		To point
	 */
	public static Intent getMapWeb( LatLng fromLatLng, LatLng toLatLng ) {
		String q = new StringBuilder().append( "http://maps.google.com/maps?" ).append( "saddr=" ).append(
				fromLatLng.latitude + "," + fromLatLng.longitude ).append( "&daddr=" ).append( toLatLng.latitude + "," + toLatLng.longitude )
				.toString();
		Intent intent = new Intent( Intent.ACTION_VIEW, Uri.parse( q.trim() ) );
		intent.setFlags( Intent.FLAG_ACTIVITY_SINGLE_TOP );
		return intent;
	}


	/**
	 * Share information by calling standards of system.
	 */
	public static Intent getShareInformation( String subject, String body ) {
		Intent i = new Intent( Intent.ACTION_SEND );
		i.setType( "text/plain" );
		i.putExtra( android.content.Intent.EXTRA_SUBJECT, subject );
		i.putExtra( android.content.Intent.EXTRA_TEXT, body );
		return i;
	}

	/**
	 * Vibrate and make sound.
	 */
	public static void vibrateSound( Context cxt, android.support.v4.app.NotificationCompat.Builder notifyBuilder ) {
		AudioManager audioManager = (AudioManager) cxt.getSystemService( Context.AUDIO_SERVICE );
		if( audioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT ) {
			notifyBuilder.setVibrate( new long[] { 1000 , 1000 , 1000 , 1000 } );
			notifyBuilder.setSound( Uri.parse( String.format( "android.resource://%s/%s", cxt.getPackageName(), R.raw.signal ) ) );
		}
		notifyBuilder.setLights( cxt.getResources().getColor( R.color.primary_color ), 1000, 1000 );
	}

	/**
	 * Test whether the {@link String} is  valid value or not, if invalidate, shakes it.
	 */
	public static boolean validateStr( Context cxt, String s ) {
		boolean val;
		if( s.matches( ".*[/=():;].*" ) ) {
			val = false;
			com.chopping.utils.Utils.showLongToast( cxt, R.string.lbl_exclude_chars );
		} else {
			val = true;
		}
		return val;
	}

	public static Bitmap getResizedBitmap(Bitmap bm, float newWidth, float newHeight ) {
		int   width       = bm.getWidth();
		int   height      = bm.getHeight();
		float scaleWidth  = newWidth / width;
		float scaleHeight = newHeight / height;
		// CREATE A MATRIX FOR THE MANIPULATION
		Matrix matrix = new Matrix();
		// RESIZE THE BITMAP
		matrix.postScale( scaleWidth, scaleHeight );
		// "RECREATE" THE NEW BITMAP
		return Bitmap.createBitmap( bm, 0, 0, width, height, matrix, false );
	}
}

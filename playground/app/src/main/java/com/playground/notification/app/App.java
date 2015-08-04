/*
                   _ooOoo_
                  o8888888o
                  88" . "88
                  (| -_- |)
                  O\  =  /O
               ____/`---'\____
             .'  \\|     |//  `.
            /  \\|||  :  |||//  \
           /  _||||| -:- |||||-  \
           |   | \\\  -  /// |   |
           | \_|  ''\---/''  |   |
           \  .-\__  `-`  ___/-. /
         ___`. .'  /--.--\  `. . __
      ."" '<  `.___\_<|>_/___.'  >'"".
     | | :  `- \`.;`\ _ /`;.`/ - ` : | |
     \  \ `-.   \_ __\ /__ _/   .-` /  /
======`-.____`-.___\_____/___.-`____.-'======
                   `=---='
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
         佛祖保佑       永无BUG
*/
//          佛曰:
//                  写字楼里写字间，写字间里程序员；
//                  程序人员写程序，又拿程序换酒钱。
//                  酒醒只在网上坐，酒醉还来网下眠；
//                  酒醉酒醒日复日，网上网下年复年。
//                  但愿老死电脑间，不愿鞠躬老板前；
//                  奔驰宝马贵者趣，公交自行程序员。
//                  别人笑我忒疯癫，我笑自己命太贱。

package com.playground.notification.app;

import java.io.IOException;
import java.util.Properties;

import android.app.Application;
import android.text.TextUtils;

import com.chopping.net.TaskHelper;
import com.playground.notification.R;
import com.playground.notification.utils.Prefs;

import cn.bmob.v3.Bmob;
import retrofit.Callback;
import retrofit.RetrofitError;


/**
 * The app-object of the project.
 *
 * @author Xinyue Zhao
 */
public final class App extends Application {
	/**
	 * Application's instance.
	 */
	public static App Instance;

	{
		Instance = this;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		//		Stetho.initialize(Stetho.newInitializerBuilder(this).enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
		//				.enableWebKitInspector(Stetho.defaultInspectorModulesProvider(this)).build());

		Properties prop = new Properties();
		try {
			prop.load(getClassLoader().getResourceAsStream("app.properties"));
			Bmob.initialize(this, prop.getProperty("bmob_app"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		TaskHelper.init(getApplicationContext());
		Prefs.createInstance(this);

		//Short the link of app-download and make a download-info, store to preference.
		//You'll see text like
		//<code>
		//			Download: http://tinyurl/asdfasdf
		//</code>
		//in sharing text.
		String url = Prefs.getInstance().getAppDownloadInfo();
		if (TextUtils.isEmpty(url) || !url.contains("tinyurl")) {
			com.tinyurl4j.Api.getTinyUrl(getString(R.string.lbl_store_url, getPackageName()),
					new Callback<com.tinyurl4j.data.Response>() {
						@Override
						public void success(com.tinyurl4j.data.Response response, retrofit.client.Response response2) {
							Prefs.getInstance().setAppDownloadInfo(getString(R.string.lbl_share_download_app, getString(
									R.string.application_name), response.getResult()));
						}

						@Override
						public void failure(RetrofitError error) {
							Prefs.getInstance().setAppDownloadInfo(getString(R.string.lbl_share_download_app, getString(
									R.string.application_name), getString(R.string.lbl_store_url, getPackageName())));
						}
					});
		}
	}
}

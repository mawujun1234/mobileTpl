/**
 * 
 *@copyright :Copyright @2012, DNE ltd. All right reserved.
 *
 */
package com.mawujun.mobile.activity.com.mawujun.mobile.util;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;


public class SystemUtil {

	//上下文
	private static Context context;
	
	/**
	 * 返回应用上下文
	 * @return
	 */
	public static Context getContext(){
		return context;
	}

	/**
	 * 初始化一个上下文
	 * @param context
	 */
	public static void init(Context context) {
		if (Validator.isNull(context))
			return;

		if (Validator.isNull(SystemUtil.context)) {
			SystemUtil.context = context;
		} else
			SystemUtil.context = context;

		if (Validator.isNotNull(context.getApplicationContext()))
			SystemUtil.context = context.getApplicationContext();
		else
			SystemUtil.context = context;

	}

	/**
	 * 获取设备号，默认是设备号
	 * @return
	 */
	public static String getDeviceCode(){
		return SystemUtil.getSerialNumble();
	}

	/**
	 * 获取设备号，可以以无限网卡，有线网卡，cpu，设备，imei，随机，设备号等作为当前设备的号码
	 * @param uuid
	 * @return
	 */
	public static String getDeviceCode(String uuid){
		String deviceCode=null;
		if("wifi".equals(uuid)){
			//无线网卡地址
			deviceCode = SystemUtil.getWifiMacAddress();
		}else if("eth0".equals(uuid)){
			//有线网卡地址
			deviceCode = SystemUtil.getEth0MacAddress();
		}else if("cpu".equals(uuid)){
			//cpu的号码
			deviceCode = SystemUtil.getCpuAddress();
		}else if("imei".equals(uuid)){
			//imei号码
			deviceCode = SystemUtil.getImei();
		}else if("random".equals(uuid)){
			//imei号码
			deviceCode = SystemUtil.getRandomSn(context);
		}else{
			//设备号
			deviceCode = SystemUtil.getSerialNumble();
		}
		return deviceCode;
	}

	/**
	 * 获取apk的版本号 currentVersionCode
	 * 
	 * @return
	 */
	public static String getAppVersion() {
		PackageManager manager = context.getApplicationContext()
				.getPackageManager();
		try {
			PackageInfo info = manager.getPackageInfo(context
					.getApplicationContext().getPackageName(), 0);
			return info.versionName;
		} catch (PackageManager.NameNotFoundException e) {
			Log.e("mes-config", e.getMessage(), e);
		}
		return null;
	}

	/*
	 * public static String getPackageName() { if(Validator.isNull(packageName))
	 * packageName = context.getPackageName(); return packageName; }
	 */

	/**
	 * 设备序列号
	 * @return
	 */
	public static String getSerialNumble() {
		String serialNum = null;
		try {
			Class<?> c = Class.forName("android.os.SystemProperties");
			Method get = c.getMethod("get", String.class, String.class);
			serialNum = (String) (get.invoke(c, "ro.serialno", "unknown"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Log.e("mes-config", e.getMessage(), e);
		}
		return serialNum;
	}

	/**
	 * imei 是否非法(仅针对带sim卡设备）
	 * 
	 * @param imei
	 * @return
	 */
	public static boolean isInvalidatorImei(String imei) {
		if (Validator.isNull(imei))
			return true;
		if (imei.trim().equalsIgnoreCase("unknown"))// 对应研华bug
			return true;
		return false;
	}

	/**
	 * 获得设备SIM卡号地址
	 * @return
	 */
	public static String getImei() {

		String imei = null;

		TelephonyManager tm = (TelephonyManager) context
				.getSystemService(Activity.TELEPHONY_SERVICE);
		imei = tm.getDeviceId();// 1.imei

		/*
		 * while (imei.length() < 15) { imei += 0; }
		 */
		return imei;
	}
	
	/**
	 * 无线网卡地址
	 * @return
	 */
	public static String getWifiMacAddress(){
		
		String imei = null;
		
		WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

		WifiInfo info = wifi.getConnectionInfo();

		if (Validator.isNotNull(info)) {
			imei = info.getMacAddress();// 3.macAddress
		}
		if (Validator.isNotNull(imei))
			imei = imei.replaceAll(StringPool.COLON, StringPool.BLANK)
					.toLowerCase();
		
		return imei;
	}
	
	/**
	 * 有线网卡地址
	 * @return
	 */
	public static String getEth0MacAddress(){
		
		String imei = null;

		try {
			imei = loadFileAsString("/sys/class/net/eth0/address")
					.toUpperCase().substring(0, 17)
					.replaceAll(StringPool.COLON, StringPool.BLANK)
					.toLowerCase();
		} catch (IOException e) {
			Log.e("mes-config",e.getMessage(), e);
			return null;
		}
		
		return imei;
	}
	
	/**
	 * CPU地址
	 * @return
	 */
	public static String getCpuAddress(){
		
		String imei = null;

		try {
			imei = loadFileAsString("/sys/devices/platform/cpu/uuid")
					.toUpperCase().substring(0, 17)
					.replaceAll(StringPool.COLON, StringPool.BLANK)
					.toLowerCase();
		} catch (IOException e) {
			Log.e("mes-config",e.getMessage(), e);
			return null;
		}
		
		return imei;
	}
	
	/**
	 * 自定义ID
	 * @param activity
	 * @return
	 */
	public static String getRandomSn(Context activity){
		
		String sn = null;
		
		FileOutputStream fos = null;
		try {
			File snFile = activity.getFileStreamPath("sn.key");
			if(snFile.exists()){
				sn = loadFileAsString(snFile.getAbsolutePath()); 
				return sn;
			}else{
				sn = PwdGenerator.getPassword(10).toLowerCase();
				fos = activity.openFileOutput("sn.key", Context.MODE_PRIVATE);
				fos.write(sn.getBytes("UTF-8"));
			}
			
		} catch (FileNotFoundException ne){
			//Log.e(SNChecker.class.getName(), ne.getMessage(),ne);
			return sn;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.e("mes-config",e.getMessage(), e);
			return sn;
		} catch (NullPointerException ne){
			Log.e("mes-config",ne.getMessage(), ne);
			return sn;
		}finally{
			if(fos != null)
				try {
					fos.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
				}
		}
		
		return sn;
		
	}

	/**
	 *  BRAND手机品牌
	 * @return
	 */
	public static String getBrand() {
		return android.os.Build.BRAND;
	}

	/**
	 *  MODEL手机型号
	 * @return
	 */
	public static String getModel() {
		return android.os.Build.MODEL;
	}

	/**
	 * 读本地文件
	 * @param filePath
	 * @return
	 * @throws java.io.IOException
	 */
	public static String loadFileAsString(String filePath)
			throws java.io.IOException {
		StringBuffer fileData = new StringBuffer(1000);
		BufferedReader reader = new BufferedReader(new FileReader(filePath));
		char[] buf = new char[1024];
		int numRead = 0;
		while ((numRead = reader.read(buf)) != -1) {
			String readData = String.valueOf(buf, 0, numRead);
			fileData.append(readData);
		}
		reader.close();
		return fileData.toString();
	}

	/***
	 * 执行ping命令行，如果ping失败最多会有1秒的等待时间.
	 * 
	 * @param serverIp
	 * @return
	 * @author huxd
	 */
	public static boolean pingServer(String serverIp) {
		boolean pingResult = false;

		try {
			Process p = Runtime.getRuntime().exec("ping -c 1 -w 1 " + serverIp);
			int status = p.waitFor();
			if (status == 0) {
				pingResult = true;
			} else {
				pingResult = false;
			}
		} catch (IOException e) {
			pingResult = false;
		} catch (InterruptedException e) {
			pingResult = false;
		}
		return pingResult;
	}

	/**
	 * 当前是否有网络连接
	 * 
	 * @return
	 */
	public static boolean isNetAvailable() {
		if (isWifiConnected() || isMobileConnected() || isEthernetConnected()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 当前是否连接WIFI网络
	 * 
	 * @return
	 */
	public static boolean isWifiConnected() {
		/*
		 * WifiManager wifiManager = (WifiManager) context
		 * .getSystemService(Context.WIFI_SERVICE);
		 */

		ConnectivityManager connManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		// 获取WIFI网络连接状态
		State state = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
				.getState();
		// 判断是否正在使用WIFI网络
		if (State.CONNECTED.equals(state)) {
			return true;
		} else
			return false;

		/*
		 * if(!wifiManager.isWifiEnabled()) return false;
		 * 
		 * 
		 * WifiInfo info = wifiManager.getConnectionInfo(); String ssid =
		 * info.getSSID(); return wifiManager.isWifiEnabled();
		 */
	}

	/**
	 * 手机网络是否开启
	 * 
	 * @return
	 */
	public static boolean isMobileConnected() {
		boolean result = false;
		// 获得网络连接服务
		ConnectivityManager connManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		// State state = connManager.getActiveNetworkInfo().getState();
		// 获取GPRS网络连接状态
		NetworkInfo network = connManager
				.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		if (Validator.isNull(network))
			return false;
		State state = network.getState();
		// 判断是否正在使用GPRS网络
		if (State.CONNECTED == state) {
			result = true;
		}
		return result;
	}

	/**
	 * 手机以太网卡是否连接（就是有线）
	 * @return
	 */
	public static boolean isEthernetConnected() {
		boolean result = false;
		if (android.os.Build.VERSION.SDK_INT > 13) {
			// 获得网络连接服务
			ConnectivityManager connManager = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			// State state = connManager.getActiveNetworkInfo().getState();
			// 获取GPRS网络连接状态
			NetworkInfo network = connManager
					.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
			if (Validator.isNull(network))
				return false;
			State state = network.getState();
			// 判断是否正在使用GPRS网络
			if (State.CONNECTED == state) {
				result = true;
			}
		} else {
			result = false;
		}
		return result;

	}

	/**
	 * 获得本机IP
	 * 
	 * @return
	 */
	public static String getLocalIpAddress() {
		return getLocalIpAddress(false);
	}

	/**
	 * 获得本机IP
	 * @param refresh
	 * @return
	 */
	public static String getLocalIpAddress(boolean refresh) {

		String ip = null;

		if (Validator.isNotNull(ip) && !refresh)
			return ip;

		try {
			Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces();
			if (Validator.isNotNull(en)) {
				while (en.hasMoreElements()) {
					NetworkInterface intf = en.nextElement();
					for (Enumeration<InetAddress> enumIpAddr = intf
							.getInetAddresses(); enumIpAddr.hasMoreElements();) {
						InetAddress inetAddress = enumIpAddr.nextElement();
						if (!inetAddress.isLoopbackAddress()
								&& inetAddress instanceof Inet4Address) {
							ip = inetAddress.getHostAddress().toString();
							return ip;
						}
					}
				}
			}
		} catch (SocketException ex) {
			Log.e("mes-config", ex.getMessage(), ex);
		} catch (Exception ex) {
			Log.e("mes-config", ex.getMessage(), ex);
		}
		Log.w("mes-config", "Can't found the machine IP.");

		ip = null;

		return ip;
	}

	/**
	 * 查询本机安装的应用
	 * @param filter
	 *            过滤字符串
	 * @return
	 */
	public static String getInstallApp(String filter) {
		List<PackageInfo> packages = context.getPackageManager()
				.getInstalledPackages(0);
		StringBuffer sbf = new StringBuffer();

		if (filter != null && !filter.equalsIgnoreCase("")) {
			for (int i = 0; i < packages.size(); i++) {
				PackageInfo packageInfo = packages.get(i);
				if (packageInfo.packageName.contains(filter)) {
					String packageName = packageInfo.packageName;
					if (sbf.toString().length() == 0)
						sbf.append(packageName);
					else
						sbf.append("," + packageName);
				}
			}
		} else {
			for (int i = 0; i < packages.size(); i++) {
				PackageInfo packageInfo = packages.get(i);
				String packageName = packageInfo.packageName;
				if (i == 0)
					sbf.append(packageName);
				else
					sbf.append("," + packageName);
			}
		}

		return sbf.toString();
	}
}

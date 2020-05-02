package com.mawujun.mobile.activity;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.azhon.appupdate.config.UpdateConfiguration;
import com.azhon.appupdate.dialog.NumberProgressBar;
import com.azhon.appupdate.listener.OnButtonClickListener;
import com.azhon.appupdate.listener.OnDownloadListener;
import com.azhon.appupdate.manager.DownloadManager;
import com.azhon.appupdate.utils.ApkUtil;

import com.azhon.appupdate.utils.FileUtil;
import com.mawujun.mobile.R;

import com.mawujun.mobile.activity.com.mawujun.mobile.util.SystemUtil;
import com.mawujun.mobile.activity.scan.ScanFragment;
import com.wang.avi.AVLoadingIndicatorView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements OnButtonClickListener {
    private static final String TAG="MainActivity";

    private WebView webView;
    //private FrameLayout loadingLayout; //提示用户正在加载数据
    private AVLoadingIndicatorView avLoadingIndicatorView;
    //private RelativeLayout webParentView;


    private boolean videoScanInited=false;
    private ScanFragment scanFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        String deviceCode= SystemUtil.getDeviceCode();

        //禁止系统锁屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        super.onCreate(savedInstanceState);
        //this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        //获得控件
        webView = (WebView) findViewById(R.id.wv_webview);
        //会一直是这个颜色，即使加载正常页面，所以后面恢复白色
        webView.setBackgroundColor(Color.rgb(	255,20,147));

        //loadingLayout = (FrameLayout) findViewById(R.id.loading_layout);
        avLoadingIndicatorView= (AVLoadingIndicatorView) findViewById(R.id.load);

        // 初始化浏览器
        initWebView(webView);

        //访问网页
        //webView.loadUrl("http://www.baidu.com");
        //webView.loadUrl("file:///android_asset/html/loading.html");
        webView.loadUrl("http://192.168.2.101:8001");

        //启动视频
        //initQRcodeScan();

        //判断是否进行app升级
        updateAppCheck();
    }


    private void initQRcodeScan(){
        if(!videoScanInited){
            //为了增加视频扫描
            scanFragment = new ScanFragment();
            getFragmentManager().beginTransaction().replace(R.id.scan_fragment, scanFragment).commit();
            videoScanInited=true;
        }
    }

//    private NumberProgressBar progressBar;
    private DownloadManager manager;
    /**
     * 判断是否进行app升级
     * https://github.com/azhon/AppUpdate/#demo%E4%B8%8B%E8%BD%BD%E4%BD%93%E9%AA%8C
     */
    private void updateAppCheck() {
        //1：首先去后台取数，判断是否要进行升级
        //1.1获取当前app的版本，传递到后台
        //后台判断，是否要升级，并把结果返回给app，主要信息就是AppUpdateManager.Builder中要配置的内容

        //根据当前versionCode或versionName去后台判断是否需要升级
        String path = null;//后台检查的地址
        JSONObject jsonObject=null;
        try {
            URL url = new URL(path);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setRequestMethod("POST");
            int versionCode=ApkUtil.getVersionCode(this);//getVersionName()
            String versionName=ApkUtil.getVersionName(this);
            //数据准备
            String data = "versionCode="+versionCode+"&versionName="+versionName;
            //至少要设置的两个请求头
            connection.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", data.length()+"");

            //post的方式提交实际上是留的方式提交给服务器
            connection.setDoOutput(true);
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(data.getBytes());

            //获得结果码
            int responseCode = connection.getResponseCode();
            if(responseCode ==200){
                //请求成功
                InputStream inputStream = connection.getInputStream();
                StringBuilder sb = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                if(sb.length()==0){
                    Toast.makeText(this,"版本检查,服务器没有返回内容!",Toast.LENGTH_SHORT).show();
                    return;
                }
                jsonObject=new JSONObject(sb.toString());
            }else {
                //请求失败
                Log.e(TAG,"本检查,服务器响应失败:"+path);
                Toast.makeText(this,"版本检查,服务器响应失败!",Toast.LENGTH_SHORT).show();
                return;
            }
        }  catch (Exception e) {
            Log.e(TAG,"版本检查,请求服务器发生异常:"+path,e);
            Toast.makeText(this,"版本检查,请求服务器发生异常!",Toast.LENGTH_SHORT).show();
            return;
        }

        //后台返回的内容以数组的形式返回，然后添加换行符，从jsonObject获取内容
        String[] dialog_msges=null;//new String[]{"1:测试1。","2:测试2"};
        String dialog_msg="";
        if(dialog_msges!=null && dialog_msges.length>0){
            for (int i = 0, len = dialog_msges.length; i < len; i++) {
                dialog_msg += dialog_msges[i];
                if (i < len - 1) {
                    dialog_msg += "\r\n";
                }
            }
        }

        /*
         * 整个库允许配置的内容，//2.根据后台返回的内容，进行升级构建,这里，
         * 非必选
         */
        UpdateConfiguration configuration = new UpdateConfiguration()
                //输出错误日志
                .setEnableLog(true)
                //设置自定义的下载
                //.setHttpManager()
                //下载完成自动跳动安装页面
                .setJumpInstallPage(true)
                //设置对话框背景图片 (图片规范参照demo中的示例图)
                //.setDialogImage(R.drawable.ic_dialog)
                //设置按钮的颜色
                //.setDialogButtonColor(Color.parseColor("#E743DA"))
                //设置对话框强制更新时进度条和文字的颜色
                //.setDialogProgressBarColor(Color.parseColor("#E743DA"))
                //设置按钮的文字颜色
                .setDialogButtonTextColor(Color.WHITE)
                //设置是否显示通知栏进度
                .setShowNotification(true)
                //设置是否提示后台下载toast
                .setShowBgdToast(false)
                //设置强制更新
                .setForcedUpgrade(false)
                //设置对话框按钮的点击监听
                .setButtonClickListener(this);
                //设置下载过程的监听
                //.setOnDownloadListener(this);

        manager = DownloadManager.getInstance(this);
        manager.setApkName("ESFileExplorer.apk")
                .setApkUrl("http://www.aaa.com")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setShowNewerToast(true)
                .setConfiguration(configuration)
                .setApkVersionCode(2)
                .setApkVersionName("2.1.8")
                .setApkSize("20.4")//20.4M
                .setApkDescription(dialog_msg)
//                .setApkMD5("DC501F04BBAA458C9DC33008EFED5E7F")
                .download();

//        //2.根据后台返回的内容，进行升级构建,这里，没有弹出框就马上下载了
//        DownloadManager manager = DownloadManager.getInstance(this);
//        manager.setApkName("appupdate.apk")
//                .setApkUrl("https://raw.githubusercontent.com/azhon/AppUpdate/master/apk/appupdate.apk")
//                .setSmallIcon(R.mipmap.ic_launcher)
//                .setApkDescription("1:测试1。2：测试2")
//                .download();

    }

    //---------------------------退出开始---
    //有一个问题，任何一个界面上，点击退出，都是退出后了，注意要区分回退
    Set<String> quitURIes=new HashSet<String>();
    {{
        quitURIes.add("/#/home");
    } }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            String url=webView.getUrl();
            System.out.println(url);
            //要判断是history模式还是history模式
            //如果使用hash模式的时候，指定哪几个地址，点击后退的时候是退出
            if(url.indexOf("/#/")!=-1){
                for(String uri:quitURIes){
                    if(url.indexOf(uri)!=-1){
                        dialog();
                        return true;
                    }
                }
                //如果没有匹配的地址，那按后退按钮，既是后退，要调用js中的后退按钮
                webView.loadUrl("javascript:backHandler()");

            }

//            if (webView.canGoBack()) {
//                webView.goBack();
//                return true;
//            } else {
//                dialog();
//                return true;
//            }
        }
        return super.onKeyDown(keyCode, event);
    }
    protected void dialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("确定要退出吗?");
        builder.setTitle("提示");
        builder.setPositiveButton("确认",
                new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        //AccoutList.this.finish();
                        //System.exit(1);
                        android.os.Process.killProcess(android.os.Process.myPid());
                    }
                });
        builder.setNegativeButton("取消",
                new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builder.create().show();
    }

    //-----------------------------------退出结束

    /**
     * 初始化webView
     *
     * @param webView
     */
    private void initWebView(WebView webView) {
        // 设置支持javascript
        webView.getSettings().setJavaScriptEnabled(true);
        //webView.addJavascriptInterface(new CommonJsInterface(MainActivity.this, webView), "android");

        //系统默认会通过手机浏览器打开网页，为了能够直接通过WebView显示网页，则必须设置
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                //使用WebView加载显示url
                view.loadUrl(url);
                // 返回值是true的时候控制去WebView打开，为false调用系统浏览器或第三方浏览器
                return true;
            }
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                // TODO Auto-generated method stub
                super.onPageStarted(view, url, favicon);
                //loadingLayout.setVisibility(View.VISIBLE);
                avLoadingIndicatorView.show();//显示
                Log.i(TAG, "onPageStarted:页面开始加载");
            }
            /**
             * onPageFinished 当内核加载完当前页面时会通知我们的应用程序，这个函数只有在main
             * frame情况下才会被调用，当调用这个函数之后，渲染的图片不会被更新，如果需要获得新图片的通知可以使用@link
             * WebView.PictureListener#onNewPicture。 参数说明：
             *
             * @param view
             *            接收WebViewClient的那个实例，前面看到webView.setWebViewClient(new
             *            MyAndroidWebViewClient())，即是这个webview。
             * @param url
             *            即将要被加载的url
             */
            @Override
            public void onPageFinished(WebView view, String url) {
                // TODO Auto-generated method stub
                super.onPageFinished(view, url);
                //加载结束后，恢复白色
                view.setBackgroundColor(Color.WHITE);

                //loadingLayout.setVisibility(View.GONE);
                avLoadingIndicatorView.hide();
                Log.i(TAG, "onPageFinished:页面加载结束");
            }


            /**
             * 如果网络连接失败，就跳到失败的界面
             * @param view
             * @param errorCode
             * @param description
             * @param failingUrl
             */
            @Override
            public void onReceivedError(WebView view, int errorCode,
                                        String description, String failingUrl) {
                // TODO Auto-generated method stub
                super.onReceivedError(view, errorCode, description, failingUrl);
                showErrorPage();

                Log.i(TAG, "onReceivedError："+failingUrl);
            }
            public void onReceivedSslError(WebView view, SslErrorHandler handler,
                                           SslError error) {
                view.loadUrl("file:///android_asset/html/net_ssl_error.html");
                // TODO Auto-generated method stub
                super.onReceivedSslError(view, handler, error);
                Log.i(TAG, "onReceivedSslError");
            }

        });


//        webView.setWebChromeClient(new WebChromeClient() {
//            @Override
//            public boolean onJsAlert(WebView view, String url, String message,
//                                     JsResult result) {
//                return super.onJsAlert(view, url, message, result);
//            }
//        });
        webView.setWebChromeClient(new WebChromeClient() {
            /**
             * 设置加载进度，当加载进去100%的时候显示页面
             * @param view
             * @param newProgress
             */
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                Log.i(TAG, "onProgressChanged:----------->" + newProgress);
                if (newProgress == 100) {
                    //loadingLayout.setVisibility(View.GONE);
                    avLoadingIndicatorView.hide();
                }
            }


            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                Log.i(TAG, "onReceivedTitle:title ------>" + title);
                if (title.contains("404")){
                    Log.i(TAG, "onReceivedTitle:加载失败 ------>" + title);
                    showErrorPage();
                }
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        } else {
            try {
                Class<?> clazz = webView.getSettings().getClass();
                Method method = clazz.getMethod(
                        "setAllowUniversalAccessFromFileURLs", boolean.class);
                if (method != null) {
                    method.invoke(webView.getSettings(), true);
                }
            } catch (Exception e) {
                Log.e("mes-webview", e.getMessage(), e);
                e.printStackTrace();
            }
        }

//        webView.setBackgroundColor(ContextCompat.getColor(this,android.R.color.transparent));
//        webView.setBackgroundResource(R.color.);

        // 支持localstorage
        webView.getSettings().setDomStorageEnabled(true);
        //webView.getSettings().setAppCacheMaxSize(1024 * 1024 * 8);
        String appCachePath = getApplicationContext().getCacheDir()
                .getAbsolutePath();
        webView.getSettings().setAppCachePath(appCachePath);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAppCacheEnabled(true);
        // 设置 缓存模式
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);

        // 每次启动清除一下缓存，通过清除webview的缓存，让app每次进入该H5界面时都重新加载：
        webView.clearCache(true);
        webView.clearHistory();


        // 硬件加速渲染,去掉的原因是，开启之后不能播放视频
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        // 软件加速渲染
        //webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        //webParentView = (RelativeLayout) webView.getParent(); //获取父容器
        //webParentView.setLayerType(View.LAYER_TYPE_HARDWARE, null);//添加这段代码将会导致摄像头扫描二维码的时候是白屏
    }

    public void showErrorPage() {
        webView.loadUrl("file:///android_asset/html/net_error.html");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(scanFragment!=null){
            scanFragment.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(scanFragment!=null){
            scanFragment.onResume();
        }
    }

    @Override
    public void onButtonClick(int id) {
        Log.e("TAG", String.valueOf(id));
    }
}

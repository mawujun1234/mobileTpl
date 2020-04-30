package com.mawujun.mobile.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
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

import com.mawujun.mobile.R;

import com.mawujun.mobile.activity.com.mawujun.mobile.util.SystemUtil;
import com.mawujun.mobile.activity.scan.ScanFragment;

import java.lang.reflect.Method;

public class MainActivity extends AppCompatActivity {
    private static final String TAG="MainActivity";

    private WebView webView;
    private FrameLayout loadingLayout; //提示用户正在加载数据
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
        webView.setBackgroundColor(Color.rgb(169,169,169));

        loadingLayout = (FrameLayout) findViewById(R.id.loading_layout);

        // 初始化浏览器
        initWebView(webView);

        //访问网页
        //webView.loadUrl("http://www.baidu.com");
        //webView.loadUrl("file:///android_asset/html/loading.html");
        webView.loadUrl("http://192.168.1.107:8001");

        //启动视频
        //initQRcodeScan();
    }


    private void initQRcodeScan(){
        if(!videoScanInited){
            //为了增加视频扫描
            scanFragment = new ScanFragment();
            getFragmentManager().beginTransaction().replace(R.id.scan_fragment, scanFragment).commit();
            videoScanInited=true;
        }
    }

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
                loadingLayout.setVisibility(View.VISIBLE);
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

                loadingLayout.setVisibility(View.GONE);
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

                Log.i(TAG, "onReceivedError");
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
                    loadingLayout.setVisibility(View.GONE);

                }
            }


            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                Log.i(TAG, "onReceivedTitle:title ------>" + title);
                if (title.contains("404")){
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
}

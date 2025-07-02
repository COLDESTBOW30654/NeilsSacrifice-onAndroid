package dpdns.hengduan.neilssacrifice;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;
    private ViewGroup decorViewGroup;
    private LinearLayout privacyDialog;
    private TextView privacyContent;
    private Button btnAgree, btnDisagree;
    private View privacyDialogBackground;

    private static final String PREFS_NAME = "AppPreferences";
    private static final String PRIVACY_ACCEPTED = "privacy_accepted";
    private static final String UPDATE_CHECK_URL = "https://hengduan.dpdns.org/game/NeilsSacrifice/update.txt";
    private static final String RELEASES_URL = "https://github.com/COLDESTBOW30654/NeilsSacrifice/releases";

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化视图
        decorViewGroup = (ViewGroup) getWindow().getDecorView();
        progressBar = findViewById(R.id.progressBar);
        webView = findViewById(R.id.webview);
        privacyDialog = findViewById(R.id.privacyDialog);
        privacyContent = findViewById(R.id.privacyContent);
        btnAgree = findViewById(R.id.btnAgree);
        btnDisagree = findViewById(R.id.btnDisagree);
        privacyDialogBackground = findViewById(R.id.privacyDialogBackground);

        // 设置隐私政策内容 - 使用ClickableSpan代替HTML链接
        String fullText = "欢迎使用\"尼尔的冒险\"！请仔细阅读以下协议和政策：\n\n" +
                "在使用本应用前，请您务必审慎阅读、充分理解";

        String privacyLink = "《用户隐私政策》";
        String licenseLink = "《软件许可协议》";

        String endText = "的各项条款。\n\n您点击\"同意\"即视为您已阅读并同意全部条款。";

        SpannableString spannable = new SpannableString(fullText + privacyLink + "和" + licenseLink + endText);

        // 为隐私政策添加点击事件
        ClickableSpan privacySpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                showPrivacyPolicy();
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(Color.parseColor("#4ECDC4")); // 使用主题色
                ds.setUnderlineText(true); // 添加下划线
            }
        };

        // 为许可协议添加点击事件
        ClickableSpan licenseSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                showLicenseAgreement();
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(Color.parseColor("#4ECDC4")); // 使用主题色
                ds.setUnderlineText(true); // 添加下划线
            }
        };

        // 设置点击范围
        int privacyStart = fullText.length();
        int privacyEnd = privacyStart + privacyLink.length();
        spannable.setSpan(privacySpan, privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        int licenseStart = privacyEnd + 1; // 加上"和"字
        int licenseEnd = licenseStart + licenseLink.length();
        spannable.setSpan(licenseSpan, licenseStart, licenseEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // 设置文本和链接行为
        privacyContent.setText(spannable);
        privacyContent.setMovementMethod(LinkMovementMethod.getInstance());
        privacyContent.setHighlightColor(Color.TRANSPARENT); // 移除点击高亮

        // 设置按钮点击事件
        btnAgree.setOnClickListener(v -> acceptPrivacyPolicy());
        btnDisagree.setOnClickListener(v -> exitApp());

        // 检查用户是否已接受隐私政策
        if (!isPrivacyAccepted()) {
            showPrivacyDialog();
        } else {
            initializeApp();
        }
    }

    private boolean isPrivacyAccepted() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(PRIVACY_ACCEPTED, false);
    }

    private void showPrivacyDialog() {
        privacyDialog.setVisibility(View.VISIBLE);
        privacyDialogBackground.setVisibility(View.VISIBLE);
    }

    private void acceptPrivacyPolicy() {
        // 保存用户选择
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(PRIVACY_ACCEPTED, true);
        editor.apply();

        // 隐藏对话框
        privacyDialog.setVisibility(View.GONE);
        privacyDialogBackground.setVisibility(View.GONE);

        // 初始化应用
        initializeApp();
    }

    private void exitApp() {
        finishAffinity(); // 完全退出应用
    }

    private void initializeApp() {
        configureWebView();
        loadGame();
        setupImmersiveMode();

        // 检查更新（每次启动都检查）
        checkForUpdates();
    }

    private void configureWebView() {
        WebSettings webSettings = webView.getSettings();

        // 核心设置
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);

        // 文件访问权限
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setAllowContentAccess(true);

        // 渲染加速
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);
        } else {
            webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
        }

        // 媒体支持
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // 使用现代缓存机制
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // Cookie 管理
        CookieManager cookieManager = CookieManager.getInstance();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }
        cookieManager.setAcceptCookie(true);

        // WebView客户端
        webView.setWebViewClient(new GameWebViewClient());
        webView.setWebChromeClient(new GameWebChromeClient());
    }

    private void loadGame() {
        progressBar.setVisibility(View.VISIBLE);
        webView.loadUrl("file:///android_asset/game/index.html");
    }

    private void setupImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            decorViewGroup.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

    // 检查应用更新（无频率控制）
    private void checkForUpdates() {
        // 在后台线程执行网络请求
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                URL url = new URL(UPDATE_CHECK_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000); // 5秒超时
                connection.setReadTimeout(5000); // 5秒读取超时

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String serverVersion = reader.readLine().trim();
                    reader.close();

                    // 获取当前版本
                    String currentVersion = getCurrentVersionName();

                    // 比较版本
                    if (!serverVersion.equals(currentVersion)) {
                        handler.post(() -> showUpdateDialog(serverVersion));
                    }
                }
            } catch (Exception e) {
                Log.e("UpdateCheck", "检查更新失败: " + e.getMessage());
            }
        });
    }

    // 获取当前版本名称
    private String getCurrentVersionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "v.0.0.2.alpha";
        }
    }

    // 显示更新对话框
    private void showUpdateDialog(String serverVersion) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("发现新版本");
        builder.setMessage("检测到新版本 " + serverVersion + " 可用！\n请更新到最新版本以获得更好的游戏体验。");
        builder.setPositiveButton("前往下载", (dialog, which) -> {
            // 打开浏览器前往下载页面
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_URL));
            startActivity(intent);
        });
        builder.setNegativeButton("取消", null);
        builder.setCancelable(false);

        // 创建并显示对话框
        AlertDialog dialog = builder.create();
        dialog.show();

        // 设置按钮颜色
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setTextColor(Color.parseColor("#4ECDC4"));

        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        negativeButton.setTextColor(Color.parseColor("#FF6B6B"));
    }

    private class GameWebViewClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.GONE);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            view.loadUrl("file:///android_asset/game/error.html");
        }
    }

    private class GameWebChromeClient extends WebChromeClient {
        private View customView;
        private CustomViewCallback customViewCallback;

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            progressBar.setProgress(newProgress);
        }

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            super.onShowCustomView(view, callback);

            if (customView != null) {
                callback.onCustomViewHidden();
                return;
            }

            customView = view;
            customViewCallback = callback;

            decorViewGroup.addView(customView);
            setupFullscreenMode();
        }

        @Override
        public void onHideCustomView() {
            super.onHideCustomView();

            if (customView == null) return;

            decorViewGroup.removeView(customView);
            customViewCallback.onCustomViewHidden();
            customView = null;
            customViewCallback = null;

            setupImmersiveMode();
        }

        private void setupFullscreenMode() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                decorViewGroup.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
            webView.resumeTimers();
            webView.evaluateJavascript("if(window.gameResume) gameResume();", null);
        }
        setupImmersiveMode();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
            webView.pauseTimers();
            webView.evaluateJavascript("if(window.gamePause) gamePause();", null);
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            setupImmersiveMode();
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    // 显示隐私政策详情
    private void showPrivacyPolicy() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("用户隐私政策");

        WebView webView = new WebView(this);
        webView.loadUrl("file:///android_asset/privacy_policy.html");

        builder.setView(webView);
        builder.setPositiveButton("关闭", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // 设置按钮颜色
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setTextColor(Color.parseColor("#4ECDC4"));
    }

    // 显示软件许可协议
    private void showLicenseAgreement() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("软件许可协议");

        WebView webView = new WebView(this);
        webView.loadUrl("file:///android_asset/license_agreement.html");

        builder.setView(webView);
        builder.setPositiveButton("关闭", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // 设置按钮颜色
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setTextColor(Color.parseColor("#4ECDC4"));
    }
}
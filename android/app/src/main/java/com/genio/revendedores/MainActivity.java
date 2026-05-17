package com.genio.revendedores;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private SwipeRefreshLayout swipeRefresh;
    private static final String SITE_URL = "https://nlmhm6zb57mku.kimi.place/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE},
                    100);
        }

        swipeRefresh = findViewById(R.id.swipeRefresh);
        webView = findViewById(R.id.webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setUserAgentString(settings.getUserAgentString() + " GenioApp/1.0");
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        // Interceptar URLs - AMBAS versiones para compatibilidad
        webView.setWebViewClient(new WebViewClient() {
            // Version NUEVA (Android 7+)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                if (url.equals("genio://download-pdf")) {
                    downloadPdfFromPage();
                    return true;
                }
                if (url.contains("wa.me") || url.contains("whatsapp.com")) {
                    openWhatsApp(url);
                    return true;
                }
                return false;
            }

            // Version VIEJA (Android viejo)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.equals("genio://download-pdf")) {
                    downloadPdfFromPage();
                    return true;
                }
                if (url.contains("wa.me") || url.contains("whatsapp.com")) {
                    openWhatsApp(url);
                    return true;
                }
                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient());
        swipeRefresh.setOnRefreshListener(() -> webView.reload());
        webView.loadUrl(SITE_URL);
    }

    private void downloadPdfFromPage() {
        Toast.makeText(this, "Guardando PDF...", Toast.LENGTH_SHORT).show();

        webView.evaluateJavascript("window._lastPdfBase64 || ''", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                if (value == null || value.isEmpty() || value.equals("null")) {
                    Toast.makeText(MainActivity.this, "PDF no disponible", Toast.LENGTH_SHORT).show();
                    return;
                }
                String dataUrl = value.replace("\"", "");
                savePdf(dataUrl);
            }
        });
    }

    private void savePdf(String dataUrl) {
        try {
            int commaIndex = dataUrl.indexOf(",");
            if (commaIndex == -1) {
                Toast.makeText(this, "Formato invalido", Toast.LENGTH_SHORT).show();
                return;
            }
            String base64 = dataUrl.substring(commaIndex + 1);
            byte[] pdfBytes = Base64.decode(base64, Base64.DEFAULT);

            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) downloadsDir.mkdirs();

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            String finalName = "pedido_genio_" + timestamp + ".pdf";

            File pdfFile = new File(downloadsDir, finalName);
            FileOutputStream fos = new FileOutputStream(pdfFile);
            fos.write(pdfBytes);
            fos.close();

            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            dm.addCompletedDownload(finalName, "Pedido Genio de la Lampara",
                    true, "application/pdf", pdfFile.getAbsolutePath(), pdfBytes.length, true);

            Toast.makeText(this, "PDF guardado: " + finalName, Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void openWhatsApp(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.setPackage("com.whatsapp");
            startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.setPackage("com.whatsapp.w4b");
                startActivity(intent);
            } catch (Exception e2) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            }
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
}

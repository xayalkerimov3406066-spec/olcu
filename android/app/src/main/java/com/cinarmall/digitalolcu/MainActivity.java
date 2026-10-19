package com.cinarmall.digitalolcu;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.core.content.FileProvider;
import androidx.webkit.WebViewAssetLoader;
import java.io.File;

public class MainActivity extends Activity {
  private static final int REQ_FILE = 1, REQ_CAM_PERM_PAGE = 2, REQ_CAM_PERM_CAPTURE = 3;
  private WebView web;
  private ValueCallback<Uri[]> fileCallback;
  private PermissionRequest pendingPermission;
  private Uri photoUri;

  @Override protected void onCreate(Bundle b) {
    super.onCreate(b);
    web = new WebView(this);
    setContentView(web);
    final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
        .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this)).build();
    WebSettings s = web.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setDatabaseEnabled(true);
    s.setMediaPlaybackRequiresUserGesture(false);
    s.setAllowFileAccess(false);
    s.setAllowContentAccess(true);
    web.setWebViewClient(new WebViewClient() {
      @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
        return loader.shouldInterceptRequest(r.getUrl());
      }
      @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
        Uri u = r.getUrl();
        if ("appassets.androidplatform.net".equals(u.getHost())) return false;
        try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) {}
        return true;
      }
    });
    web.setWebChromeClient(new WebChromeClient() {
      @Override public void onPermissionRequest(PermissionRequest request) {
        runOnUiThread(() -> {
          boolean wantsCam = false;
          for (String r : request.getResources()) if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(r)) wantsCam = true;
          if (!wantsCam) { request.deny(); return; }
          if (hasCam()) request.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
          else { pendingPermission = request; requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAM_PERM_PAGE); }
        });
      }
      @Override public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
        if (fileCallback != null) fileCallback.onReceiveValue(null);
        fileCallback = cb;
        if (p.isCaptureEnabled()) {
          if (hasCam()) openCamera();
          else requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAM_PERM_CAPTURE);
        } else {
          Intent i = new Intent(Intent.ACTION_GET_CONTENT);
          i.addCategory(Intent.CATEGORY_OPENABLE);
          i.setType("image/*");
          try { startActivityForResult(Intent.createChooser(i, "Şəkil seçin"), REQ_FILE); }
          catch (Exception e) { fileCallback.onReceiveValue(null); fileCallback = null; return false; }
        }
        return true;
      }
    });
    if (b != null) web.restoreState(b);
    else web.loadUrl("https://appassets.androidplatform.net/assets/www/index.html?app=1");
  }

  private boolean hasCam() { return checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED; }

  private void openCamera() {
    try {
      File dir = new File(getCacheDir(), "images"); dir.mkdirs();
      File f = new File(dir, "olcu_" + System.currentTimeMillis() + ".jpg");
      photoUri = FileProvider.getUriForFile(this, "com.cinarmall.digitalolcu.fileprovider", f);
      Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
      i.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
      i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
      startActivityForResult(i, REQ_FILE);
    } catch (Exception e) {
      photoUri = null;
      if (fileCallback != null) { fileCallback.onReceiveValue(null); fileCallback = null; }
    }
  }

  @Override public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
    boolean ok = res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED;
    if (code == REQ_CAM_PERM_PAGE && pendingPermission != null) {
      if (ok) pendingPermission.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
      else pendingPermission.deny();
      pendingPermission = null;
    } else if (code == REQ_CAM_PERM_CAPTURE) {
      if (ok) openCamera();
      else if (fileCallback != null) { fileCallback.onReceiveValue(null); fileCallback = null; }
    }
  }

  @Override protected void onActivityResult(int code, int result, Intent data) {
    super.onActivityResult(code, result, data);
    if (code != REQ_FILE || fileCallback == null) return;
    Uri[] out = null;
    if (result == RESULT_OK) {
      if (data != null && data.getData() != null) out = new Uri[]{data.getData()};
      else if (photoUri != null) out = new Uri[]{photoUri};
    }
    fileCallback.onReceiveValue(out);
    fileCallback = null; photoUri = null;
  }

  @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); web.saveState(out); }

  @Override public void onBackPressed() {
    web.evaluateJavascript("(function(){var b=document.getElementById('backBtn');var c=document.getElementById('cam');" +
        "if(c&&!c.hidden){document.getElementById('camClose').click();return 'h';}" +
        "if(b&&!b.disabled){b.click();return 'h';}return 'x';})()", v -> { if (v == null || !v.contains("h")) finish(); });
  }
}

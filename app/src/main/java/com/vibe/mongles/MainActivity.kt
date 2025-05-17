package com.vibe.mongles

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.vibe.mongles.ui.theme.MonglesTheme
import android.view.ViewGroup
import android.widget.FrameLayout

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 뒤로가기 버튼 처리
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (::webView.isInitialized && webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
        
        setContent {
            MonglesTheme {
                WebViewScreen(
                    url = "https://mongles.com",
                    onWebViewCreated = { webView -> this.webView = webView }
                )
            }
        }
    }
}

@Composable
fun WebViewScreen(
    url: String, 
    modifier: Modifier = Modifier,
    onWebViewCreated: (WebView) -> Unit = {}
) {
    val context = LocalContext.current
    val shouldRefreshOnce = remember { mutableStateOf(true) }
    
    // 모바일 User-Agent 설정
    val mobileUserAgent = "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.91 Mobile Safari/537.36"
    
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                // 레이아웃 파라미터 설정
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val requestUrl = request?.url?.toString() ?: ""
                        
                        // 외부 브라우저로 열어야 할 URL 패턴 확인
                        if (requestUrl.contains("pf.kakao.com") || 
                            requestUrl.contains("www.instagram.com") ||
                            requestUrl.contains("instagram.com")) {
                            // 외부 브라우저로 URL 열기
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(requestUrl))
                            context.startActivity(intent)
                            return true
                        }
                        
                        // 그 외의 URL은 WebView 내에서 처리
                        view?.loadUrl(requestUrl)
                        return true
                    }
                    
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        
                        view?.let { webView ->
                            // 스크롤 위치를 상단으로 설정
                            webView.scrollTo(0, 0)
                            
                            // 페이지 콘텐츠 조정을 위한 JavaScript 실행
                            val js = """
                                (function() {
                                    // 메타 태그 설정
                                    var meta = document.querySelector('meta[name="viewport"]');
                                    if (!meta) {
                                        meta = document.createElement('meta');
                                        meta.name = 'viewport';
                                        document.head.appendChild(meta);
                                    }
                                    meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no';
                                    
                                    // 100ms 후 스크롤 재조정 (레이아웃 완료 후)
                                    setTimeout(function() {
                                        window.scrollTo(0, 0);
                                    }, 100);
                                    
                                    // 카카오 링크 및 인스타그램 링크 처리
                                    var externalLinks = document.querySelectorAll('a[href*="pf.kakao.com"], a[href*="instagram.com"]');
                                    for (var i = 0; i < externalLinks.length; i++) {
                                        var link = externalLinks[i];
                                        link.setAttribute('target', '_blank');
                                    }
                                })();
                            """.trimIndent()
                            webView.evaluateJavascript(js, null)
                            
                            // 페이지가 처음 로드된 후 한 번만 새로고침
                            if (shouldRefreshOnce.value) {
                                shouldRefreshOnce.value = false
                                webView.post {
                                    webView.reload()
                                }
                            }
                        }
                    }
                }
                
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    setSupportZoom(true)
                    
                    // 모바일 버전 사이트 표시를 위한 추가 설정
                    userAgentString = mobileUserAgent
                    layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
                    textZoom = 100
                    
                    // 메타 태그의 viewport 설정 사용
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    
                    // 콘텐츠 크기 자동 조정을 위한 설정
                    setSupportMultipleWindows(false)
                    defaultTextEncodingName = "UTF-8"
                    allowFileAccess = true
                    allowContentAccess = true
                    loadsImagesAutomatically = true
                    mediaPlaybackRequiresUserGesture = false
                    
                    // 웹뷰 스크롤 관련 설정
                    builtInZoomControls = false
                    displayZoomControls = false
                }
                
                // 초기 스크롤 위치 설정
                scrollY = 0
                
                // 웹사이트 로드
                loadUrl(url)
                onWebViewCreated(this)
            }
        }
    )
}
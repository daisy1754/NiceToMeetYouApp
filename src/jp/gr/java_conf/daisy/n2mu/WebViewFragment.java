package jp.gr.java_conf.daisy.n2mu;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

/**
 * Webview for showing Linkedin/Twitter information
 * TODO: implemented native view.
 */
public class WebViewFragment extends Fragment {
    public static final String VIEW_TYPE_LINKEDIN = "Linkedin";
    public static final String VIEW_TYPE_TWITTER = "Twitter";
    private static final String KEY_TYPE = "WebViewFragment:Type";
    private WebView mWebView;
    private String mType;

    public static WebViewFragment newInstance(String type) {
        WebViewFragment fragment = new WebViewFragment();
        fragment.mType = type;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if ((savedInstanceState != null) && savedInstanceState.containsKey(KEY_TYPE)) {
            mType = savedInstanceState.getString(KEY_TYPE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.fragment_web_view, container, false);
        mWebView = (WebView) rootView.findViewById(R.id.webView);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(getActivity(), description, Toast.LENGTH_SHORT).show();
            }
        });
        mWebView.loadUrl("http://www.google.com");
        return rootView;
    }
}

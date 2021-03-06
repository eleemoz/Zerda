/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import org.mozilla.focus.R;
import org.mozilla.focus.locale.LocaleAwareFragment;
import org.mozilla.focus.web.IWebView;

/**
 * Base implementation for fragments that use an IWebView instance. Based on Android's WebViewFragment.
 */
public abstract class WebFragment extends LocaleAwareFragment {
    private IWebView webView;
    private boolean isWebViewAvailable;

    private Bundle webViewState;

    /* If fragment exists but no WebView to use, store url here if there is any loadUrl requirement */
    protected String pendingUrl = null;

    /**
     * Inflate a layout for this fragment. The layout needs to contain a view implementing IWebView
     * with the id set to "webview".
     */
    @NonNull
    public abstract View inflateLayout(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState);

    public abstract IWebView.Callback createCallback();

    /**
     * Get the initial URL to load after the view has been created.
     */
    @Nullable
    public abstract String getInitialUrl();

    @Override
    public final View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflateLayout(inflater, container, savedInstanceState);

        webView = (IWebView) view.findViewById(R.id.webview);
        isWebViewAvailable = true;
        webView.setCallback(createCallback());

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // restore WebView state
        if (savedInstanceState == null) {
            // in two cases we won't have saved-state: fragment just created, or fragment re-attached.
            // if fragment was detached before, we will have webViewState.
            // per difference case, we should load initial url or pending url(if any).
            if (webViewState != null) {
                webView.restoreWebviewState(webViewState);
            }

            final String url = (webViewState == null) ? getInitialUrl() : pendingUrl;
            pendingUrl = null; // clear pending url
            if (!TextUtils.isEmpty(url)) {
                webView.loadUrl(url);
            }
        } else {
            // Fragment was destroyed
            webView.restoreWebviewState(savedInstanceState);
        }
    }

    @Override
    public void applyLocale() {
        // We create and destroy a new WebView here to force the internal state of WebView to know
        // about the new language. See issue #666.
        final WebView unneeded = new WebView(getContext());
        unneeded.destroy();
    }

    @Override
    public void onPause() {
        webView.onPause();

        super.onPause();
    }

    @Override
    public void onResume() {
        webView.onResume();

        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        webView.onSaveInstanceState(outState);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        if (webView != null) {
            webView.setCallback(null);
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        isWebViewAvailable = false;

        // If Fragment is detached from Activity but not be destroyed, onSaveInstanceState won't be
        // called. In this case we must store webView-state manually, to retain browsing history.
        webViewState = new Bundle();
        webView.onSaveInstanceState(webViewState);
        super.onDestroyView();
    }

    @Nullable
    protected IWebView getWebView() {
        return isWebViewAvailable ? webView : null;
    }

}

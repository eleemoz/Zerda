/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import org.mozilla.focus.locale.Locales;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

public class SupportUtils {
    public static final String HELP_URL = "https://support.mozilla.org/kb/what-firefox-focus-android";
    public static final String BLOCKING_HELP_URL = "";
    public static final String DEFAULT_BROWSER_URL = "https://support.mozilla.org/kb/set-firefox-focus-default-browser-android";
    public static final String YOUR_RIGHTS_URI = "file:///android_asset/rights-focus.html";
    public static final String PRIVACY_URL = "https://www.mozilla.org/privacy/firefox-rocket/";
    public static final String ABOUT_URI = "file:///android_res/raw/about.html";

    public static String getSumoURLForTopic(final Context context, final String topic) {
        String escapedTopic;
        try {
            escapedTopic = URLEncoder.encode(topic, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("utf-8 should always be available", e);
        }

        final String appVersion;
        try {
            appVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            // This should be impossible - we should always be able to get information about ourselves:
            throw new IllegalStateException("Unable find package details for Focus", e);
        }

        final String osTarget = "Android";
        final String langTag = Locales.getLanguageTag(Locale.getDefault());

        return "https://support.mozilla.org/1/mobile/" + appVersion + "/" + osTarget + "/" + langTag + "/" + escapedTopic;
    }

    public static String getManifestoURL() {
        final String langTag = Locales.getLanguageTag(Locale.getDefault());
        return "https://www.mozilla.org/" + langTag + "/about/manifesto/";
    }

    public static String getPrivacyURL() {
        return PRIVACY_URL;
    }

    public static String getSupportURL() {
        return HELP_URL;
    }

    public static String getYourRightsURI() {
        return YOUR_RIGHTS_URI;
    }

    public static String getAboutURI() {
        return ABOUT_URI;
    }
}

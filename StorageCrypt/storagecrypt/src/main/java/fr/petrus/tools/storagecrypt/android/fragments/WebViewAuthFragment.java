/*
 *  Copyright Pierre Sagne (12 december 2014)
 *
 * petrus.dev.fr@gmail.com
 *
 * This software is a computer program whose purpose is to encrypt and
 * synchronize files on the cloud.
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 *
 */

package fr.petrus.tools.storagecrypt.android.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.HashMap;
import java.util.Map;

import fr.petrus.lib.core.StorageType;
import fr.petrus.tools.storagecrypt.R;

/**
 * This fragment displays the logon page of a remote account to link.
 *
 * @author Pierre Sagne
 * @since 13.12.2014
 */
public class WebViewAuthFragment extends Fragment {
    /**
     * The constant TAG used for logging and the fragment manager.
     */
    public static final String TAG = "WebViewAuthFragment";

    private FragmentListener fragmentListener;

    /**
     * The interface used by this fragment to communicate with the Activity.
     */
    public interface FragmentListener {

        /**
         * Links a new account of the given {@code storageType}, from the given OAuth2
         * {@code responseParameters}.
         *
         * @param storageType        the type of the account to link
         * @param keyAlias           the alias of the default key to encrypt the documents in this
         *                           account with
         * @param responseParameters the OAuth2 "authorize call" response parameters
         */
        void onAccessCode(StorageType storageType, String keyAlias, Map<String, String> responseParameters);

        /**
         * Displays an error message because the authentication failed.
         *
         * @param storageType        the type of the account for which the authentication failed
         * @param responseParameters the OAuth2 "authorize call" error response parameters
         */
        void onAuthFailed(StorageType storageType, Map<String, String> responseParameters);
    }

    private WebView authWebView = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @SuppressWarnings( "deprecation" )
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof FragmentListener) {
            fragmentListener = (FragmentListener) activity;
        } else {
            throw new ClassCastException(activity.toString()
                    + " must implement "+ FragmentListener.class.getName());
        }
    }

    @Override
    public void onDetach() {
        fragmentListener = null;
        super.onDetach();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        String oauthUrl = getArguments().getString("oauth_url", null);
        String clientId = getArguments().getString("client_id", null);
        final String redirectUri = getArguments().getString("redirect_uri", null);
        String responseType = getArguments().getString("response_type", null);
        String scope = getArguments().getString("scope", null);
        String state = getArguments().getString("state", null);

        String url = oauthUrl+"?client_id="+clientId+"&redirect_uri="+redirectUri+"&response_type="+responseType;
        if (null!=scope) {
            url += "&scope="+scope;
        }
        if (null!=state) {
            url += "&state="+state;
        }

        final String storageType = getArguments().getString("storageType", null);
        final String keyAlias = getArguments().getString("keyAlias", null);

        removeAllCookies();

        View view = inflater.inflate(R.layout.fragment_webview_auth, container, false);
        authWebView = (WebView) view.findViewById(R.id.web_view);
        authWebView.getSettings().setJavaScriptEnabled(true);
        authWebView.loadUrl(url);
        authWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon){
                super.onPageStarted(view, url, favicon);
                Log.d(TAG, "URL = "+url);
                if (url.startsWith(redirectUri)) {
                    Uri uri = Uri.parse(url);
                    String error = uri.getQueryParameter("error");
                    if (null != error) {
                        HashMap<String, String> responseParameters = new HashMap<>();
                        for (String parameterName : uri.getQueryParameterNames()) {
                            responseParameters.put(parameterName, uri.getQueryParameter(parameterName));
                        }
                        fragmentListener.onAuthFailed(StorageType.valueOf(storageType),
                                responseParameters);
                    } else {
                        HashMap<String, String> responseParameters = new HashMap<>();
                        for (String parameterName : uri.getQueryParameterNames()) {
                            responseParameters.put(parameterName, uri.getQueryParameter(parameterName));
                        }
                        fragmentListener.onAccessCode(StorageType.valueOf(storageType),
                                keyAlias, responseParameters);
                    }
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                //Log.d(TAG, "finish URL : " + url);
            }
        });
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.menu_webview_auth_fragment, menu);
    }

    @SuppressWarnings("deprecation")
    private static void removeAllCookies() {
        CookieManager cookieManager = CookieManager.getInstance();
        if (Build.VERSION.SDK_INT>=21) {
            cookieManager.removeAllCookies(null);
        } else {
            cookieManager.removeAllCookie();
        }
    }
}

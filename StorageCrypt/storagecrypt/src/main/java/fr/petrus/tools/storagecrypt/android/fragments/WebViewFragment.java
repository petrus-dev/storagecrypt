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

import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import org.markdownj.MarkdownProcessor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import fr.petrus.tools.storagecrypt.R;

/**
 * This fragment displays a HTML page in an Android WebView.
 *
 * <p>The content of the page can be defined as an HTML or MarkDown file or string.
 *
 * <p>A HTML anchor can be provided to scroll at a certain location in the page.
 *
 * @author Pierre Sagne
 * @since 13.12.2014
 */
public class WebViewFragment extends Fragment {
    /**
     * The constant TAG used for logging and the fragment manager.
     */
    public static final String TAG = "WebViewFragment";

    /**
     * The argument used to pass the Android resource id of the MarkDown page to display.
     */
    public static final String BUNDLE_MARKDOWN_TEXT_FILE_ID = "markdown_text_file_id";

    /**
     * The argument used to pass the Android resource id of the HTML page to display.
     */
    public static final String BUNDLE_HTML_TEXT_FILE_ID = "html_text_file_id";

    /**
     * The argument used to pass the text of the MarkDown page to display.
     */
    public static final String BUNDLE_MARKDOWN_TEXT = "markdown_text";

    /**
     * The argument used to pass the text of the HTML page to display.
     */
    public static final String BUNDLE_HTML_TEXT = "html_text";

    /**
     * The argument used to pass HTML anchor to point to.
     */
    public static final String BUNDLE_HTML_ANCHOR = "html_anchor";

    private WebView webView = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        int markdownTextFileId = getArguments().getInt(BUNDLE_MARKDOWN_TEXT_FILE_ID);
        int htmlTextFileId = getArguments().getInt(BUNDLE_HTML_TEXT_FILE_ID);
        String markdownText = getArguments().getString(BUNDLE_MARKDOWN_TEXT, null);
        String htmlText = getArguments().getString(BUNDLE_HTML_TEXT, null);
        String htmlAnchor = getArguments().getString(BUNDLE_HTML_ANCHOR, null);

        if (0!=markdownTextFileId) {
            markdownText = readText(markdownTextFileId);
        }
        if (null!=markdownText) {
            MarkdownProcessor m = new MarkdownProcessor();
            htmlText = m.markdown(markdownText);
            Log.d(TAG, htmlText);
        }

        View view = inflater.inflate(R.layout.fragment_webview, container, false);
        webView = (WebView) view.findViewById(R.id.web_view);
        webView.getSettings().setJavaScriptEnabled(false);
        if (0!=htmlTextFileId) {
            String url = resourceToFileUrl(getActivity(), htmlTextFileId, htmlAnchor);
            webView.loadUrl(url);
            Log.d(TAG, "url="+url);
        } else {
            webView.loadData(htmlText, "text/html; charset=UTF-8", null);
        }
        return view;
    }

    private static Uri resourceToUri (Context context, int resID) {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                context.getResources().getResourcePackageName(resID) + '/' + resID);
    }

    private static String resourceToFileUrl (Context context, int resID, String anchor) {
        String url = resourceToFileUrl(context, resID);
        if (null!=anchor) {
            url += "#" + anchor;
        }
        return url;
    }

    private static String resourceToFileUrl (Context context, int resID) {
        return ContentResolver.SCHEME_FILE + ":///android_res/" +
                context.getResources().getResourceTypeName(resID) + '/' +
                context.getResources().getResourceEntryName(resID);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.menu_webview_fragment, menu);
    }

    private String readText(int fileResId) {
        String result = "";
        try {
            String str;
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(getResources().openRawResource(fileResId), StandardCharsets.UTF_8));
            while ((str = in.readLine()) != null) {
                result += str + "\n";
            }
            in.close();
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Error when reading text file", e);
        } catch (IOException e) {
            Log.e(TAG, "Error when reading text file", e);
        } catch (Exception e) {
            Log.e(TAG, "Error when reading text file", e);
        }
        return result;
    }
}

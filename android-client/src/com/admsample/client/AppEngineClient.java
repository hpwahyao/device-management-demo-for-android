/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.admsample.client;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import com.admsample.client.ui.SetupActivity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * AppEngine client. Handles authentication.
 */
public class AppEngineClient {
    private static final String TAG = "AppEngineClient";

    private static final String AUTH_URL = App.SERVER_BASE_URL + "/_ah/login";
    private static final String AUTH_TOKEN_TYPE = "ah";

    private final Context mContext;
    private final String mAccountName;

    public AppEngineClient(Context context, String accountName) {
        this.mContext = context;
        this.mAccountName = accountName;
    }

    public HttpResponse makeRequest(String urlPath, List<NameValuePair> params) throws Exception {
        HttpResponse res = makeRequestNoRetry(urlPath, params, false);
        if (res.getStatusLine().getStatusCode() == 500) {
            res = makeRequestNoRetry(urlPath, params, true);
        }
        return res;
    }

    private HttpResponse makeRequestNoRetry(String urlPath, List<NameValuePair> params, boolean newToken)
            throws Exception {

        // Get auth token for a Google account.
        Account account = new Account(mAccountName, "com.google");
        String authToken = getAuthToken(mContext, account);
        if (authToken == null) throw new PendingAuthException(mAccountName);
        if (newToken) {  // invalidate the cached token
            AccountManager accountManager = AccountManager.get(mContext);
            accountManager.invalidateAuthToken(account.type, authToken);
            authToken = getAuthToken(mContext, account);
        }

        // Get ACSID cookie
        DefaultHttpClient client = new DefaultHttpClient();
        String continueURL = App.SERVER_BASE_URL;
        URI uri = new URI(AUTH_URL + "?continue=" +
                URLEncoder.encode(continueURL, "UTF-8") +
                "&auth=" + authToken);
        HttpGet method = new HttpGet(uri);
        final HttpParams getParams = new BasicHttpParams();
        HttpClientParams.setRedirecting(getParams, false);  // continue is not used
        method.setParams(getParams);

        HttpResponse res = client.execute(method);
        Header[] headers = res.getHeaders("Set-Cookie");
        if (res.getStatusLine().getStatusCode() != 302 ||
                headers.length == 0) {
            return res;
        }

        String ascidCookie = null;
        for (Header header: headers) {
            if (header.getValue().indexOf("ACSID=") >=0) {
                // let's parse it
                String value = header.getValue();
                String[] pairs = value.split(";");
                ascidCookie = pairs[0];
            }
        }

        // Make POST request
        uri = new URI(App.SERVER_BASE_URL + urlPath);
        HttpPost post = new HttpPost(uri);
        if (params != null) {
            UrlEncodedFormEntity entity =
                new UrlEncodedFormEntity(params, "UTF-8");
            post.setEntity(entity);
        }
        post.setHeader("Cookie", ascidCookie);
        post.setHeader("X-Same-Domain", "1");  // XSRF
        res = client.execute(post);
        return res;
    }

    private String getAuthToken(Context context, Account account) {
        String authToken = null;
        AccountManager accountManager = AccountManager.get(context);
        try {
            AccountManagerFuture<Bundle> future =
                    accountManager.getAuthToken (account, AUTH_TOKEN_TYPE, false, null, null);
            Bundle bundle = future.getResult();
            authToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
            // User will be asked for "App Engine" permission.
            if (authToken == null) {
                // No auth token - will need to ask permission from user.
                Intent intent = new Intent(SetupActivity.AUTH_PERMISSION_ACTION);
                intent.putExtra("AccountManagerBundle", bundle);
                context.sendBroadcast(intent);
            }
        } catch (OperationCanceledException e) {
            Log.w(TAG, e.getMessage());
        } catch (AuthenticatorException e) {
            Log.w(TAG, e.getMessage());
        } catch (IOException e) {
            Log.w(TAG, e.getMessage());
        }
        return authToken;
    }

    public class PendingAuthException extends Exception {
        private static final long serialVersionUID = 1L;
        public PendingAuthException(String message) {
            super(message);
        }
    }
}

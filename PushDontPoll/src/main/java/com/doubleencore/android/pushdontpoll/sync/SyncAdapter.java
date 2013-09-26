package com.doubleencore.android.pushdontpoll.sync;

import android.accounts.Account;
import android.annotation.TargetApi;
import android.content.*;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;
import com.doubleencore.android.pushdontpoll.CloudEndpointUtils;
import com.doubleencore.android.pushdontpoll.Constants;
import com.doubleencore.android.pushdontpoll.dateendpoint.Dateendpoint;
import com.doubleencore.android.pushdontpoll.dateendpoint.model.CurrentDate;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.jackson.JacksonFactory;

import java.io.IOException;
import java.util.Date;

/**
 * Handle the transfer of data between a server and an
 * app, using the Android sync adapter framework.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {
    // Global variables
    // Define a variable to contain a content resolver instance
    ContentResolver mContentResolver;
    /**
     * Set up the sync adapter
     */
    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    /**
     * Set up the sync adapter. This form of the
     * constructor maintains compatibility with Android 3.0
     * and later platform versions
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);

    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {

        Dateendpoint.Builder endpointBuilder = new Dateendpoint.Builder(
                AndroidHttp.newCompatibleTransport(),
                new JacksonFactory(),
                new HttpRequestInitializer() {
                    @Override
                    public void initialize(HttpRequest httpRequest) throws IOException {
                    }
                });

        Dateendpoint endpoint = CloudEndpointUtils.updateBuilder(endpointBuilder).build();
        try {
            CurrentDate date = endpoint.currentDate().execute();
            long time = date.getDate();

            SharedPreferences.Editor editor = getContext().getSharedPreferences(Constants.PREFS_NAME, 0).edit();
            editor.putLong(Constants.PREFS_KEY_TIME, time).apply();

            Intent updateIntent = new Intent(Constants.UPDATE_UI_INTENT_ACTION);
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(updateIntent);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

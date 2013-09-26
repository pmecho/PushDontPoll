package com.doubleencore.android.pushdontpoll;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("h:mm:ss a");

    private BroadcastReceiver mReceiver;
    private TextView mTimeText;
    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTimeText = (TextView) findViewById(R.id.time);

        mPrefs = getSharedPreferences(Constants.PREFS_NAME, 0);

        // Always try to add the dummy account in case it is deleted
        Account account = new Account(Constants.ACCOUNT, Constants.ACCOUNT_TYPE);
        AccountManager accountManager = AccountManager.get(getApplicationContext());
        accountManager.addAccountExplicitly(account, null, null);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateText();
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Register for the update UI broadcast that could be sent from the GCMIntentService or the SyncAdapter
        LocalBroadcastManager.getInstance(this).registerReceiver((mReceiver), new IntentFilter(Constants.UPDATE_UI_INTENT_ACTION));
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateText();
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_register) {
            startActivity(new Intent(this, RegisterActivity.class));
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Update the text view with the time saved in shared prefs
     */
    private void updateText() {
        long time = mPrefs.getLong(Constants.PREFS_KEY_TIME, 0L);
        if (time == 0L) {
            mTimeText.setText(R.string.unknown);
        } else {
            mTimeText.setText(DATE_FORMAT.format(new Date(time)));
        }
    }

}

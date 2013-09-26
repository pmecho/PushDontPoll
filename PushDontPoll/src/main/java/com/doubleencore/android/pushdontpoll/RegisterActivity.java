package com.doubleencore.android.pushdontpoll;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;

/**
 * An activity that communicates with your App Engine backend via Cloud Endpoints.
 * <p/>
 * When the user hits the "Register" button, a message is sent to the backend (over endpoints) indicating that the
 * device would like to receive broadcast messages from it. Clicking "Register" also has the effect of registering this
 * device for Google Cloud Messaging (GCM). Whenever the backend wants to broadcast a message, it does it via GCM, so
 * that the device does not need to keep polling the backend for messages.
 * <p/>
 * If you've generated an App Engine backend for an existing Android project, this activity will not be hooked in to
 * your main activity as yet. You can easily do so by adding the following lines to your main activity:
 * <p/>
 * Intent intent = new Intent(this, RegisterActivity.class); startActivity(intent);
 * <p/>
 * To make the sample run, you need to set your PROJECT_NUMBER in GCMIntentService.java. If you're going to be running a
 * local version of the App Engine backend (using the DevAppServer), you'll need to toggle the LOCAL_ANDROID_RUN flag in
 * CloudEndpointUtils.java. See the javadoc in these classes for more details.
 * <p/>
 * For a comprehensive walkthrough, check out the documentation at http://developers.google.com/eclipse/docs/cloud_endpoints
 */
public class RegisterActivity extends Activity {

    enum State {
        REGISTERED, REGISTERING, UNREGISTERED, UNREGISTERING
    }

    private State curState = State.UNREGISTERED;
    private OnTouchListener registerListener = null;
    private OnTouchListener unregisterListener = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        Button regButton = (Button) findViewById(R.id.regButton);

        registerListener = new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        if (GCMIntentService.PROJECT_NUMBER == null
                                || GCMIntentService.PROJECT_NUMBER.length() == 0) {
                            showDialog("Unable to register for Google Cloud Messaging. "
                                    + "Your application's PROJECT_NUMBER field is unset! You can change "
                                    + "it in GCMIntentService.java");
                        } else {
                            updateState(State.REGISTERING);
                            try {
                                GCMIntentService.register(getApplicationContext());
                            } catch (Exception e) {
                                Log.e(RegisterActivity.class.getName(),
                                        "Exception received when attempting to register for Google Cloud "
                                                + "Messaging. Perhaps you need to set your virtual device's "
                                                + " target to Google APIs? "
                                                + "See https://developers.google.com/eclipse/docs/cloud_endpoints_android"
                                                + " for more information.", e);
                                showDialog("There was a problem when attempting to register for "
                                        + "Google Cloud Messaging. If you're running in the emulator, "
                                        + "is the target of your virtual device set to 'Google APIs?' "
                                        + "See the Android log for more details.");
                                updateState(State.UNREGISTERED);
                            }
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        return true;
                    default:
                        return false;
                }
            }
        };

        unregisterListener = new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        updateState(State.UNREGISTERING);
                        GCMIntentService.unregister(getApplicationContext());
                        return true;
                    case MotionEvent.ACTION_UP:
                        return true;
                    default:
                        return false;
                }
            }
        };

        regButton.setOnTouchListener(registerListener);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

    /*
     * If we are dealing with an intent generated by the GCMIntentService
     * class, then display the provided message.
     */
        if (intent.getBooleanExtra("gcmIntentServiceMessage", false)) {

            showDialog(intent.getStringExtra("message"));

            if (intent.getBooleanExtra("registrationMessage", false)) {

                if (intent.getBooleanExtra("error", false)) {
          /*
           * If we get a registration/unregistration-related error,
           * and we're in the process of registering, then we move
           * back to the unregistered state. If we're in the process
           * of unregistering, then we move back to the registered
           * state.
           */
                    if (curState == State.REGISTERING) {
                        updateState(State.UNREGISTERED);
                    } else {
                        updateState(State.REGISTERED);
                    }
                } else {
          /*
           * If we get a registration/unregistration-related success,
           * and we're in the process of registering, then we move to
           * the registered state. If we're in the process of
           * unregistering, the we move back to the unregistered
           * state.
           */
                    if (curState == State.REGISTERING) {
                        updateState(State.REGISTERED);
                    } else {
                        updateState(State.UNREGISTERED);
                    }
                }
            }
        }
    }

    private void updateState(State newState) {
        Button registerButton = (Button) findViewById(R.id.regButton);
        switch (newState) {
            case REGISTERED:
                registerButton.setText("Unregister");
                registerButton.setOnTouchListener(unregisterListener);
                registerButton.setEnabled(true);
                break;

            case REGISTERING:
                registerButton.setText("Registering...");
                registerButton.setEnabled(false);
                break;

            case UNREGISTERED:
                registerButton.setText("Register");
                registerButton.setOnTouchListener(registerListener);
                registerButton.setEnabled(true);
                break;

            case UNREGISTERING:
                registerButton.setText("Unregistering...");
                registerButton.setEnabled(false);
                break;
        }
        curState = newState;
    }

    private void showDialog(String message) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        }).show();
    }
}

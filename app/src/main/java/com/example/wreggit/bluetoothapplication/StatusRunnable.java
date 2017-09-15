package com.example.wreggit.bluetoothapplication;

import android.app.Activity;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

public class StatusRunnable implements Runnable {

    private Activity mActivity;
    private Boolean mMessageVisible;
    private String mMessage;
    private Boolean mSpinnerVisible;

    public StatusRunnable(Activity activity, Boolean messageVisible, String message, Boolean spinnerVisible) {
        mActivity = activity;
        mMessageVisible = messageVisible;
        mMessage = message;
        mSpinnerVisible = spinnerVisible;
    }

    @Override
    public void run() {
        if(mMessageVisible != null && mMessage != null) {
            updateConnectionUI(mMessageVisible, mMessage);
        }

        if(mSpinnerVisible != null) {
            updateSpinnerUI(mSpinnerVisible);
        }
    }

    public void updateConnectionUI(boolean visible, String message) {
        TextView connection = (TextView) mActivity.findViewById(R.id.connection);
        if(visible) {
            connection.setText(message);
            connection.setVisibility(View.VISIBLE);
        } else {
            connection.setVisibility(View.INVISIBLE);
        }
    }

    public void updateSpinnerUI(boolean visible) {
        ProgressBar spinner = (ProgressBar) mActivity.findViewById(R.id.progressBar);
        if(visible) {
            spinner.setVisibility(View.VISIBLE);
        } else {
            spinner.setVisibility(View.GONE);
        }
    }
}

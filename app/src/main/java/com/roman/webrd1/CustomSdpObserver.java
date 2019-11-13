package com.roman.webrd1;

import android.util.Log;

import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

class CustomSdpObserver implements SdpObserver {
    private final String tag;

    CustomSdpObserver(String logTag) {
        //tag = this.getClass().getCanonicalName();
        this.tag = "CustomSdpObserver22 " + logTag;
    }


    @Override
    public void onCreateSuccess(SessionDescription sessionDescription) {
        Log.d(tag, "onCreateSuccess() called with: sessionDescription = [" + sessionDescription + "]");
       // Log.d(tag, "onCreateSuccess() called with: sessionDescription=xxxx");
    }

    @Override
    public void onSetSuccess() {
        Log.d(tag, "onSetSuccess() called");
    }

    @Override
    public void onCreateFailure(String s) {
        Log.d(tag, "onCreateFailure() called with: s = [" + s + "]");
    }

    @Override
    public void onSetFailure(String s) {
        Log.d(tag, "onSetFailure() called with: s = [" + s + "]");
    }
}

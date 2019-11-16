package com.roman.webrd1;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.Logging;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

class RdWebSocketListener extends WebSocketListener {

    private static final String TAG = "RdWebSocketListener22";
    private MainActivity mainActivity;


    //public RdWebSocketListener(MainActivity mainActivity, String remoteUser) {
    public RdWebSocketListener(MainActivity mainActivity) {
        super();
        this.mainActivity = mainActivity;
//        this.remoteUser = remoteUser;
    }


    @Override
    public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        super.onClosed(webSocket, code, reason);
        Logging.d(TAG, "Closed :"  + code + " / " + reason );
    }

    @Override
    public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        super.onClosing(webSocket, code, reason);
        Logging.d(TAG, "Closing : " + code + " / " + reason);
    }

    @Override
    public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
        super.onFailure(webSocket, t, response);
        Logging.d(TAG, "Error : " + t.getMessage());
    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
        super.onMessage(webSocket, text);
        Logging.d(TAG, "onMessage text : " + text);
        try {

            String typeOfMessage = new JSONObject(text).getString("type");

            if (!typeOfMessage.equalsIgnoreCase("login")) {


                JSONObject json = new JSONObject(new JSONObject(text).getString(typeOfMessage));
                if (typeOfMessage.equals("candidate")) {
                    //received ICE candidates from a remote peer
                    mainActivity.saveIceCandidate(json);
                } else {
                    if (typeOfMessage.equals("offer")) {
                        Logging.d(TAG, "Save Offer and Answer text : " + text);
                        mainActivity.saveOfferAndAnswer(json);
                        //saveOfferAndAnswer(json);

                    } else if (typeOfMessage.equals("answer")) {
                        mainActivity.saveAnswer(json);
                    } else {
                        Logging.d(TAG, "Else : " + text);
                    }
                }
                }
            } catch(JSONException e){
                e.printStackTrace();
            }

    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
        super.onMessage(webSocket, bytes);
        Logging.d(TAG, "on Message - receiving bytes : " + bytes.hex() );
    }

    @Override
    public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
        super.onOpen(webSocket, response);
        Logging.d(TAG, "Open : " + response );
        //Logging.d(TAG, "Open" );
    }





}

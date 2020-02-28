package com.roman.pawelm;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.Logging;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

import static com.roman.pawelm.MainActivity.APP_ROLE_CAMERA;

class RdWebSocketListener extends WebSocketListener {

    private static final String TAG = "RdWebSocketListener22";
    private MainActivity mainActivity;


    //public RdWebSocketListener(MainActivity mainActivity, String remoteUser) {
    public RdWebSocketListener(MainActivity mainActivity) {
        super();
        this.mainActivity = mainActivity;
    }


    @Override
    public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        super.onClosed(webSocket, code, reason);
        mainActivity.isWebSocketConnected=false;
        Logging.d(TAG, "Closed :"  + code + " / " + reason );
    }

    @Override
    public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        super.onClosing(webSocket, code, reason);
        mainActivity.sendLeave();
        Logging.d(TAG, "Closing : " + code + " / " + reason);
    }

    @Override
    public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
        super.onFailure(webSocket, t, response);
        Logging.d(TAG, "Error onFailure : " + t.getMessage());
        mainActivity.isWebSocketConnected=false;
        if (!mainActivity.isTryingReconnectWebSocket) {
            mainActivity.logToServer();
        }

    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
        super.onMessage(webSocket, text);
        Logging.d(TAG, "onMessage text : " + text);
//        try {
//
//            String typeOfMessage = new JSONObject(text).getString("type");
//            if (typeOfMessage.equalsIgnoreCase("login"))
//
//            {
//                String jsonString = new JSONObject(text).getString("success");
//                if(jsonString.equalsIgnoreCase("false")) {
//                    Logging.d(TAG, "Login failed");
//                } else {
//                    jsonString = new JSONObject(text).getString("ice");
//                    //JSONObject json = new JSONObject(new JSONArray(text).getJSONArray("ice"));
//                    mainActivity.addIceServers(jsonString);
//
//                    //mainActivity.isInitiator=true;
//                    mainActivity.tryToStart(500);
//
////                    jsonString = new JSONObject(text).getString("action");
////                    Logging.d(TAG, "nextAction:" + jsonString);
////                    if(jsonString.equalsIgnoreCase("startCall")){
////                        Logging.d(TAG, "isInitiator: true");
////                        mainActivity.isInitiator=true;
////                        mainActivity.tryToStart(500);
////                    }
//                }
//
//            }
//            else {
//
//
//                JSONObject json = new JSONObject(new JSONObject(text).getString(typeOfMessage));
//                if (typeOfMessage.equals("candidate")) {
//                    //received ICE candidates from a remote peer
//                    mainActivity.saveIceCandidate(json);
//                } else {
//                    if (typeOfMessage.equals("offer")) {
//                        Logging.d(TAG, "Save Offer and Answer text : " + text);
//                        mainActivity.saveOfferAndAnswer(json);
//                        //saveOfferAndAnswer(json);
//
//
//                    } else if (typeOfMessage.equals("answer")) {
//                        mainActivity.saveAnswer(json);
//                    } else {
//                        Logging.d(TAG, "Else : " + text);
//                    }
//                }
//                }
//            } catch(JSONException e){
//                e.printStackTrace();
//            }








        try {

            String typeOfMessage = new JSONObject(text).getString("type");
            if (typeOfMessage.equalsIgnoreCase("login")) {
                String jsonString = new JSONObject(text).getString("success");
                if(jsonString.equalsIgnoreCase("false")) {
                    Logging.d(TAG, "Login failed");
                } else {
                    jsonString = new JSONObject(text).getString("ice");
                    //JSONObject json = new JSONObject(new JSONArray(text).getJSONArray("ice"));
                    mainActivity.addIceServers(jsonString);
                }

            }

            else if (typeOfMessage.equals("action")) {

                String jsonString = new JSONObject(text).getString("action");
                if(jsonString.equalsIgnoreCase("startCall")) {
                    Logging.d(TAG, "Action: startCall");
                    //mainActivity.isInitiator=true;
                    if (mainActivity.localAppRole.equals(APP_ROLE_CAMERA)) {
                        mainActivity.tryToStart(500);
                    }
                } else {
                    Logging.d(TAG, "Action: wait");
                }
            }

            else if (typeOfMessage.equals("leave")){
//                mainActivity.sendLeave();
//                mainActivity.hangup();
//                mainActivity.tryToStart(1000);
                Logging.d(TAG, "Message: leave");
            }
            else {
                JSONObject json = new JSONObject(new JSONObject(text).getString(typeOfMessage));
                if (typeOfMessage.equals("candidate")) {
                    //received ICE candidates from a remote peer
                    mainActivity.saveIceCandidate(json);
                }
                else if (typeOfMessage.equals("offer")) {
                    Logging.d(TAG, "Save Offer and Answer text : " + text);
                    mainActivity.saveOfferAndAnswer(json);
                    //saveOfferAndAnswer(json);
                }

                else if (typeOfMessage.equals("answer")) {
                        mainActivity.saveAnswer(json);
                }


                else {
                        Logging.d(TAG, "Else : " + text);
                    }
                }
            }
         catch(JSONException e){
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
        mainActivity.isWebSocketConnected=true;
        Logging.d(TAG, "Open : " + response );
        //Logging.d(TAG, "Open" );
    }





}

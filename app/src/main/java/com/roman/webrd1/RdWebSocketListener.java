package com.roman.webrd1;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public final class RdWebSocketListener extends WebSocketListener {

    private static final String TAG = "RdWebSocketListener";
    private MainActivity mainActivity;
    private PeerConnection peerConnection;
    private WebSocket webSocket;

    private RdWebSocketListener(MainActivity mainActivity) {
        super();
        this.mainActivity = mainActivity;
    }

    public void setWebSocket(WebSocket webSocket) {
        this.webSocket = webSocket;
    }
    public void setPeerConnection(PeerConnection peerConnection) {
        this.peerConnection = peerConnection;
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
            JSONObject json = new JSONObject(new JSONObject(text).getString("utf8Data"));
            if (json.getString("type").equals("candidate")) {
                saveIceCandidate(json);
            } else {
                if (json.getString("type").equals("OFFER")) {
                    System.out.println("..................................CUSTOMLISTENER####### SaveandAnswer text : " + text);
                    saveOfferAndAnswer(json);
                } else if (json.getString("type").equals("ANSWER")) {
                    saveAnswer(json);
                }
            }
        } catch (JSONException e) {
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
    }

    void saveOfferAndAnswer(JSONObject json) throws JSONException {
        SessionDescription sessionDescription = new SessionDescription(SessionDescription.Type.OFFER, json.getString("sdp"));
        peerConnection.setRemoteDescription(new CustomSdpObserver("##### remoteSetRemoteDesc"), sessionDescription);

        peerConnection.createAnswer(new CustomSdpObserver("##### remoteCreateOffer") {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                peerConnection.setLocalDescription(new CustomSdpObserver("#### remoteSetLocalDesc"), sessionDescription);
                try {
                    JSONObject json = new JSONObject();
                    json.put("type", sessionDescription.type);
                    json.put("sdp", sessionDescription.description);
                    webSocket.send(json.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new MediaConstraints());
    }

    void saveAnswer(JSONObject json) throws JSONException {
        SessionDescription sessionDescription;
        sessionDescription = new SessionDescription(SessionDescription.Type.ANSWER, json.getString("sdp"));
        mainActivity.setRemoteDescription(sessionDescription);
        //mainActivity.setRemoteDescription(sessionDescription);
    }
    void saveIceCandidate(JSONObject json) throws JSONException {
        IceCandidate iceCandidate = new IceCandidate(json.getString("id"),Integer.parseInt(json.getString("label")),json.getString("candidate"));
        peerConnection.addIceCandidate(iceCandidate);
    }


}

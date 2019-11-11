package com.roman.webrd1;


import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
//import org.webrtc.CapturerObserver;
//import org.webrtc.VideoRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoTrack;
import org.webrtc.VideoSource;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    private String socketAddress = "http://10.0.2.2:9090";
    //private String socketAddress = "http://192.168.0.106:9090";
    //private String socketAddress = "http://ec2-18-188-37-20.us-east-2.compute.amazonaws.com:1337";

    private String remoteUser = "rd2";

    private OkHttpClient webSocket;
    private WebSocket wsListener;

    private PeerConnectionFactory peerConnectionFactory;
    MediaConstraints audioConstraints;
    MediaConstraints videoConstraints;
    MediaConstraints sdpConstraints;
    VideoSource videoSource;
    VideoTrack localVideoTrack;
    AudioSource audioSource;
    AudioTrack localAudioTrack;
    SurfaceViewRenderer localVideoView;
    SurfaceViewRenderer remoteVideoView;
    SurfaceTextureHelper textureHelper;

    Button hangup;
    PeerConnection localPeer;
    //List<PeerConnection.IceServer> iceServers;
    EglBase rootEglBase;

    boolean gotUserMedia;
    List<PeerConnection.IceServer> peerIceServers = new ArrayList<>();

    private static final String TAG = "MainActivityr22";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        askForPermissions();

        setContentView(R.layout.activity_main);
        initViews();
        initVideos();
        getIceServers();

        start();
    }

    public void askForPermissions() {
        //ask permissions
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // request runtime permissions
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA,Manifest.permission.CHANGE_NETWORK_STATE,Manifest.permission.MODIFY_AUDIO_SETTINGS,Manifest.permission.RECORD_AUDIO,Manifest.permission.BLUETOOTH,Manifest.permission.INTERNET,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.ACCESS_NETWORK_STATE},
                    100);
        }
    }
    private void initViews() {
        hangup = (Button)findViewById(R.id.end_call);
        localVideoView =  findViewById(R.id.local_gl_surface_view);
        remoteVideoView = (SurfaceViewRenderer)findViewById(R.id.remote_gl_surface_view);
        hangup.setOnClickListener((View.OnClickListener) this);
    }

    private void initVideos() {

        rootEglBase = EglBase.create();
        localVideoView.init(rootEglBase.getEglBaseContext(), null);
        remoteVideoView.init(rootEglBase.getEglBaseContext(), null);
        localVideoView.setZOrderMediaOverlay(true);
        remoteVideoView.setZOrderMediaOverlay(true);
    }

    //#############################################################################################################








    public void setRemoteDescription(SessionDescription sessionDescription) {

        localPeer.setRemoteDescription(new CustomSdpObserver("r22localSetRemoteDesc"), sessionDescription);

    }



    public void saveOfferAndAnswer(JSONObject json) throws JSONException {
        SessionDescription sessionDescription = new SessionDescription(SessionDescription.Type.OFFER, json.getString("sdp"));
        localPeer.setRemoteDescription(new CustomSdpObserver("r22remoteSetRemoteDesc"), sessionDescription);

        localPeer.createAnswer(new CustomSdpObserver("r22 remoteCreateOffer") {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                localPeer.setLocalDescription(new CustomSdpObserver("r22 remoteSetLocalDesc"), sessionDescription);
                try {
                    JSONObject json = new JSONObject();
                    json.put("type", sessionDescription.type);
                    json.put("sdp", sessionDescription.description);
                    json.put("name", remoteUser);
                    wsListener.send(json.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new MediaConstraints());
    }


    public void saveAnswer(JSONObject json) throws JSONException {
        SessionDescription sessionDescription;
        sessionDescription = new SessionDescription(SessionDescription.Type.ANSWER, json.getString("sdp"));
        setRemoteDescription(sessionDescription);
        Logging.d(TAG, "Saving Answer ");
        //mainActivity.setRemoteDescription(sessionDescription);
    }


    public void saveIceCandidate(JSONObject json) throws JSONException {
        IceCandidate iceCandidate = new IceCandidate(json.getString("id"),Integer.parseInt(json.getString("label")),json.getString("candidate"));
        localPeer.addIceCandidate(iceCandidate);
        Logging.d(TAG, "Saving iceCandidate");
    }




    //##############################################################################################################



    private void getIceServers() {


        PeerConnection.IceServer iceServer = PeerConnection.IceServer.builder("stun:u1.xirsys.com")
                .setTlsCertPolicy(PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK)
                .createIceServer();

        peerIceServers.add(iceServer);
        //rd

        iceServer = PeerConnection.IceServer.builder("turn:u1.xirsys.com:80?transport=udp")
                .setUsername("6F9otw7OYvpJ49xRRNXXrLbZlmfdFnsEqEpFtmMpi-WtAF4XMzRD687O4xsAGHDVAAAAAF18GL9yb21hbmRtbw==")
                .setPassword("39839660-d676-11e9-8c3e-f676af1e4042")
                .setTlsCertPolicy(PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK)
                .createIceServer();
        peerIceServers.add(iceServer);

        iceServer = PeerConnection.IceServer.builder("turn:u1.xirsys.com:80?transport=tcp")
                .setUsername("6F9otw7OYvpJ49xRRNXXrLbZlmfdFnsEqEpFtmMpi-WtAF4XMzRD687O4xsAGHDVAAAAAF18GL9yb21hbmRtbw==")
                .setPassword("39839660-d676-11e9-8c3e-f676af1e4042")
                .setTlsCertPolicy(PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK)
                .createIceServer();
        peerIceServers.add(iceServer);

    }


    public void start() {


        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(this)
                        .setEnableVideoHwAcceleration(true)
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);


        PeerConnectionFactory.Options options= new PeerConnectionFactory.Options();

        DefaultVideoEncoderFactory defaultVideoEncoderFactory = new DefaultVideoEncoderFactory(
                rootEglBase.getEglBaseContext(),  /* enableIntelVp8Encoder */true,  /* enableH264HighProfile */true);
        DefaultVideoDecoderFactory defaultVideoDecoderFactory = new DefaultVideoDecoderFactory(rootEglBase.getEglBaseContext());



        peerConnectionFactory=PeerConnectionFactory.builder()
                //.setOptions(options)
                .setVideoEncoderFactory(defaultVideoEncoderFactory)
                .setVideoDecoderFactory(defaultVideoDecoderFactory)
                .createPeerConnectionFactory();


        VideoCapturer videoCapturer=createCameraCapturer();

        //Create MediaConstraints - Will be useful for specifying video and audio constraints.
        audioConstraints = new MediaConstraints();
        videoConstraints = new MediaConstraints();

        //suggestion from GitHub

        sdpConstraints = new MediaConstraints(); //was missing




        //VideoSource videoSource=peerConnectionFactory.createVideoSource(false);
        VideoSource videoSource=peerConnectionFactory.createVideoSource(videoCapturer);
        localVideoTrack=peerConnectionFactory.createVideoTrack("100",videoSource);
        //localVideoTrack.setEnabled(true);
        audioSource=peerConnectionFactory.createAudioSource(audioConstraints);
        localAudioTrack=peerConnectionFactory.createAudioTrack("101",audioSource);
        //localAudioTrack.setEnabled(true);

//        MediaStream localMediaStream = peerConnectionFactory.createLocalMediaStream("105");
//        localMediaStream.addTrack(localAudioTrack);
//        localMediaStream.addTrack(localVideoTrack);
//
//        VideoRenderer localVideoRenderer =

        textureHelper=SurfaceTextureHelper.create(Thread.currentThread().getName(),rootEglBase.getEglBaseContext());
        videoCapturer.initialize(textureHelper,this,videoSource.getCapturerObserver());




        //videoCapturer.startCapture(1024,720,30);//capture in HD
        //videoCapturer.startCapture(640,480,30);//capture in SD
        videoCapturer.startCapture(320,240,30);//capture in LD

        localVideoView.setVisibility(View.VISIBLE);
        localVideoView.setMirror(true);
        //localVideoView.init(rootEglBase.getEglBaseContext(),null);


        localVideoTrack.addSink(localVideoView);


        createPeerConnection();
        createLocalSocket();




        //addStreamToLocalPeer();
        //doCall();

        //Logging.enableTracing("logcat:", EnumSet.of(Logging.TraceLevel.TRACE_DEFAULT));
        Logging.enableLogToDebugOutput(Logging.Severity.LS_VERBOSE);
        //org.webrtcLogging.enableLogToDebugOutput(Logging.Severity.LS_VERBOSE);
    }


    public void createLocalSocket() {
        Request request = new Request.Builder().url(socketAddress).build();
        RdWebSocketListener listener = new RdWebSocketListener(this);
        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder();
        webSocket = okHttpClientBuilder.build();
        wsListener = webSocket.newWebSocket(request, listener);
        webSocket.dispatcher().executorService().shutdown();
        Log.d("r22createLocalSocket->", "DONE");
    }



    /**
     * Creating the local peerconnection instance
     */
    private void createPeerConnection() {
        PeerConnection.RTCConfiguration rtcConfig =
                new PeerConnection.RTCConfiguration(peerIceServers);
        // TCP candidates are only useful when connecting to a server that supports
        // ICE-TCP.
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
        //rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXCOMPAT;
        //rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.NEGOTIATE;
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        // Use ECDSA encryption.

        rtcConfig.keyType = PeerConnection.KeyType.ECDSA;
        //rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;





        localPeer = peerConnectionFactory.createPeerConnection(rtcConfig, new RdPeerConnectionObserver("localPeerCreation") {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                Log.d("Mainr22", "onIceCandidate");
                super.onIceCandidate(iceCandidate);
                try {
                    JSONObject json = new JSONObject();
                    json.put("type", "candidate");
                    json.put("label", iceCandidate.sdpMLineIndex);
                    json.put("id", iceCandidate.sdpMid);
                    json.put("candidate", iceCandidate.sdp);
                    json.put("name", remoteUser);
                    wsListener.send(json.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

//                super.onIceCandidate(iceCandidate);
//                Map<String, String> iceCandidateParams = new HashMap<>();
//                iceCandidateParams.put("sdpMid", iceCandidate.sdpMid);
//                iceCandidateParams.put("sdpMLineIndex", Integer.toString(iceCandidate.sdpMLineIndex));
//                iceCandidateParams.put("candidate", iceCandidate.sdp);
//                if (webSocketAdapter.getUserId() != null) {
//                    iceCandidateParams.put("endpointName", webSocketAdapter.getUserId());
//                    webSocketAdapter.sendJson(webSocket, "onIceCandidate", iceCandidateParams);
//                } else {
//                    webSocketAdapter.addIceCandidate(iceCandidateParams);
//                }


            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                //showToast("Received Remote stream");
                super.onAddStream(mediaStream);
                final VideoTrack videoTrack = mediaStream.videoTracks.get(0);
                try {
                    remoteVideoView.setVisibility(View.VISIBLE);
                    videoTrack.addSink(remoteVideoView);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                super.onIceGatheringChange(iceGatheringState);

            }
        });
        Log.d("createPeerConnection->", "DONE");
    }





    /**
     * Adding the stream to the localpeer
     */
    private void addStreamToLocalPeer() {
        //creating local mediastream
        MediaStream stream = peerConnectionFactory.createLocalMediaStream("102");
        stream.addTrack(localAudioTrack);
        stream.addTrack(localVideoTrack);
//        try {
            localPeer.addStream(stream);
//        }
//        catch (Throwable e) {
//            Log.e("TestApplication", "Uncaught exception is: ", e);
//        }
       //
    }

    /**
     * This method is called when the app is initiator - We generate the offer and send it over through socket
     * to remote peer
     */
    private void doCall() {

        sendLogin("rd1");
        //sdpConstraints = new MediaConstraints();
        sdpConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"));
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "offerToReceiveVideo", "true"));



//        CustomSdpObserver testSdpObserver = new CustomSdpObserver("localCreateOffer") {
//            @Override
//            public void onCreateSuccess(SessionDescription sessionDescription) {
//                super.onCreateSuccess(sessionDescription);
//                //Log.d("onCreateSuccess", "Creating and sending offer to: ");
//                Log.d("r22onCreateOfferMain", "onCreateSuccess() called with: sessionDescription = [" + sessionDescription + "]");
//                localPeer.setLocalDescription(new CustomSdpObserver("localSetLocalDesc"), sessionDescription);
//                //localPeer.setLocalDescription();
//                testCreateOffer(sessionDescription);
////                //Sending the offer
////                try {
////                    JSONObject json = new JSONObject();
////                    json.put("type", sessionDescription.type);
////                    json.put("sdp", sessionDescription.description);
////                    json.put("name", remoteUser);
////                    wsListener.send(json.toString());
////
////                } catch (Throwable e) {
////                    Log.e("TestApplication", "Uncaught exception is: ", e);
////                }
//            }
//        };
//
//        localPeer.createOffer(testSdpObserver, sdpConstraints);


        MediaStream stream = peerConnectionFactory.createLocalMediaStream("102");
        stream.addTrack(localAudioTrack);
        stream.addTrack(localVideoTrack);
        localPeer.addStream(stream);

        localPeer.createOffer(new CustomSdpObserver("r22localCreateOffer")
        {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                //Log.d("onCreateSuccess", "Creating and sending offer to: ");

                localPeer.setLocalDescription(new CustomSdpObserver("localSetLocalDesc"), sessionDescription);
                Log.d("Mainr22", "localPeer.createOffer setLocalDescription = [" + sessionDescription + "]");
                sendOffer(sessionDescription);
            }
        }
        , sdpConstraints);

        Log.d("Mainr22", "localPeer.createOffer DONE");
        Log.d("AFTERcreateOffer->", "DONE");

    }

    void sendLogin (String myName) {
        try {
            JSONObject json = new JSONObject();

            json.put("type", "login");
            json.put("name", myName);
            wsListener.send(json.toString());

        } catch (Throwable e) {
            Log.e("SendLogin", "Uncaught exception is: ", e);
        }
    }
    void sendOffer (SessionDescription sessionDescription) {
        //Sending the offer
        try {
            JSONObject json = new JSONObject();
            JSONObject offer = new JSONObject();
            //offer.put("type", sessionDescription.type);
            offer.put("type", "offer");
            offer.put("sdp", sessionDescription.description);

            json.put("type", "offer");
            json.put("offer", offer);
            json.put("name", remoteUser);
            wsListener.send(json.toString());
            Log.d("Mainr22", "sendOFFER=" + json.toString());

        } catch (Throwable e) {
            Log.e("sendOffer", "Uncaught exception is: ", e);
        }
    }

    void testCreateOffer2 () {
        //Sending the offer
        try {
            JSONObject json = new JSONObject();
            json.put("type", "OFFER");
            json.put("sdp", "Description");
            json.put("name", remoteUser);
            wsListener.send(json.toString());

        } catch (Throwable e) {
            Log.e("TestApplication", "Uncaught exception is: ", e);
        }
    }




    /**
     * Received remote peer's media stream. we will get the first video track and render it
     */
//    private void gotRemoteStream(MediaStream stream) {
//        //we have remote video stream. add to the renderer.
//        final VideoTrack videoTrack = stream.videoTracks.get(0);
//        runOnUiThread(() -> {
//            try {
//                remoteVideoView.setVisibility(View.VISIBLE);
//                videoTrack.addSink(remoteVideoView);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        });
//
//    }


    /**
     * Received local ice candidate. Send it to remote peer through signalling for negotiation
     */
//    public void onIceCandidateReceived(IceCandidate iceCandidate) {
//        //we have received ice candidate. We can set it to the other peer.
////        SignallingClient.getInstance().emitIceCandidate(iceCandidate);
//    }

    /**
     * SignallingCallback - called when the room is created - i.e. you are the initiator
     */
//    @Override
//    public void onCreatedRoom() {
//        showToast("You created the room " + gotUserMedia);
//        if (gotUserMedia) {
//            SignallingClient.getInstance().emitMessage("got user media");
//        }
//    }

    /**
     * SignallingCallback - called when you join the room - you are a participant
     */
//    @Override
//    public void onJoinedRoom() {
//        showToast("You joined the room " + gotUserMedia);
//        if (gotUserMedia) {
//            SignallingClient.getInstance().emitMessage("got user media");
//        }
//    }

//    @Override
//    public void onNewPeerJoined() {
//        showToast("Remote Peer Joined");
//    }
//
//    @Override
//    public void onRemoteHangUp(String msg) {
//        showToast("Remote Peer hungup");
//        runOnUiThread(this::hangup);
//    }
//
//    /**
//     * SignallingCallback - Called when remote peer sends offer
//     */
//    @Override
//    public void onOfferReceived(final JSONObject data) {
//        showToast("Received Offer");
//        runOnUiThread(() -> {
//            if (!SignallingClient.getInstance().isInitiator && !SignallingClient.getInstance().isStarted) {
//                onTryToStart();
//            }
//
//            try {
//                localPeer.setRemoteDescription(new CustomSdpObserver("localSetRemote"), new SessionDescription(SessionDescription.Type.OFFER, data.getString("sdp")));
//                doAnswer();
//                updateVideoViews(true);
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//        });
//    }

    private void doAnswer() {
        localPeer.createAnswer(new CustomSdpObserver("localCreateAns") {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                localPeer.setLocalDescription(new CustomSdpObserver("localSetLocal"), sessionDescription);
//                SignallingClient.getInstance().emitMessage(sessionDescription);
            }
        }, new MediaConstraints());
    }

    /**
     * SignallingCallback - Called when remote peer sends answer to your offer
     */

//    @Override
//    public void onAnswerReceived(JSONObject data) {
//        showToast("Received Answer");
//        try {
//            localPeer.setRemoteDescription(new CustomSdpObserver("localSetRemote"), new SessionDescription(SessionDescription.Type.fromCanonicalForm(data.getString("type").toLowerCase()), data.getString("sdp")));
//            updateVideoViews(true);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * Remote IceCandidate received
//     */
//    @Override
//    public void onIceCandidateReceived(JSONObject data) {
//        try {
//            localPeer.addIceCandidate(new IceCandidate(data.getString("id"), data.getInt("label"), data.getString("candidate")));
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//    }
//
//
//    private void updateVideoViews(final boolean remoteVisible) {
//        runOnUiThread(() -> {
//            ViewGroup.LayoutParams params = localVideoView.getLayoutParams();
//            if (remoteVisible) {
//                params.height = dpToPx(100);
//                params.width = dpToPx(100);
//            } else {
//                params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
//            }
//            localVideoView.setLayoutParams(params);
//        });
//
//    }


    /**
     * Closing up - normal hangup and app destroye
     */

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.end_call: {
                //hangup();
                doCall();
                //sendLogin("rd1");
                //testCreateOffer2();
                break;
            }
        }
    }

    void doTestOffer() {

    }

//    private void hangup() {
//        try {
//            localPeer.close();
//            localPeer = null;
//            //SignallingClient.getInstance().close();
//            updateVideoViews(false);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//    }

//    @Override
//    protected void onDestroy() {
////        SignallingClient.getInstance().close();
//        super.onDestroy();
//    }

    /**
     * Util Methods
     */
    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public void showToast(final String msg) {
        //runOnUiThread(() -> Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show());
    }

    private VideoCapturer createCameraCapturer() {
    //private CapturerObserver createCameraCapturer(){

    CameraEnumerator enumerator;

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP ){
            enumerator = new Camera2Enumerator(this);
        } else {
            enumerator = new Camera1Enumerator(true);
        }

        Logging.d(TAG, "Looking for front facing cameras.");

        String[] devicenames = enumerator.getDeviceNames();

        for (String dn : devicenames) {
            if (enumerator.isFrontFacing(dn)) {
                Logging.d(TAG, "Creating front facing camera capturer.");
                return enumerator.createCapturer(dn, null);

            }
        }
        //failed to get front facing cam
        Logging.d(TAG, "Looking for other cameras.");
        for (String dn : devicenames) {
            if (!enumerator.isFrontFacing(dn)) {
                Logging.d(TAG, "Creating other camera capturer.");
                return enumerator.createCapturer(dn, null);
            }
        }
        //failed to find both
        Logging.d(TAG, "Failed to find any camera.");
        return null;

    }
}

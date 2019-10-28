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
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoTrack;
import org.webrtc.VideoSource;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    private String socketAddress = "http://10.0.2.2:1337";
    //private String socketAddress = "http://192.168.0.108:1337";
    //private String socketAddress = "http://ec2-18-188-37-20.us-east-2.compute.amazonaws.com:1337";

    private OkHttpClient webSocket;
    private WebSocket wsListener;

    PeerConnectionFactory peerConnectionFactory;
    MediaConstraints audioConstraints;
    MediaConstraints videoConstraints;
    MediaConstraints sdpConstraints;
    VideoSource videoSource;
    VideoTrack localVideoTrack;
    AudioSource audioSource;
    AudioTrack localAudioTrack;
    SurfaceViewRenderer localVideoView;
    SurfaceViewRenderer remoteVideoView;

    Button hangup;
    PeerConnection localPeer;
    //List<PeerConnection.IceServer> iceServers;
    EglBase rootEglBase;

    boolean gotUserMedia;
    List<PeerConnection.IceServer> peerIceServers = new ArrayList<>();

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        askForPermissions();

        setContentView(R.layout.activity_main);




        initViews();
        initVideos();
        getIceServers();


        //SignallingClient.getInstance().init(this);

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
        localVideoView.setMirror(true);
        remoteVideoView.setMirror(false);
        rootEglBase = EglBase.create();
        localVideoView.init(rootEglBase.getEglBaseContext(), null);
        remoteVideoView.init(rootEglBase.getEglBaseContext(), null);
        localVideoView.setZOrderMediaOverlay(true);
        //remoteVideoView.setZOrderMediaOverlay(true);
    }

    public void setRemoteDescription(SessionDescription sessionDescription) {
        localPeer.setRemoteDescription(new CustomSdpObserver("localSetRemoteDesc"), sessionDescription);
    }

    private void getIceServers() {


        PeerConnection.IceServer iceServer = PeerConnection.IceServer.builder("stun:u1.xirsys.com").createIceServer();

        peerIceServers.add(iceServer);
        //rd

        iceServer = PeerConnection.IceServer.builder("\"turn:u1.xirsys.com:80?transport=udp\"")
                .setUsername("6F9otw7OYvpJ49xRRNXXrLbZlmfdFnsEqEpFtmMpi-WtAF4XMzRD687O4xsAGHDVAAAAAF18GL9yb21hbmRtbw==")
                .setPassword("39839660-d676-11e9-8c3e-f676af1e4042")
                .createIceServer();
        peerIceServers.add(iceServer);

        iceServer = PeerConnection.IceServer.builder("\"turn:u1.xirsys.com:80?transport=tcp\"")
                .setUsername("6F9otw7OYvpJ49xRRNXXrLbZlmfdFnsEqEpFtmMpi-WtAF4XMzRD687O4xsAGHDVAAAAAF18GL9yb21hbmRtbw==")
                .setPassword("39839660-d676-11e9-8c3e-f676af1e4042")
                .createIceServer();
        peerIceServers.add(iceServer);


//        //get Ice servers using xirsys
//        byte[] data = new byte[0];
//        try {
//            data = ("<xirsys_ident>:<xirsys_secret>").getBytes("UTF-8");
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
//        String authToken = "Basic " + Base64.encodeToString(data, Base64.NO_WRAP);
//        Utils.getInstance().getRetrofitInstance().getIceCandidates(authToken).enqueue(new Callback<TurnServerPojo>() {
//            @Override
//            public void onResponse(@NonNull Call<TurnServerPojo> call, @NonNull Response<TurnServerPojo> response) {
//                TurnServerPojo body = response.body();
//                if (body != null) {
//                    iceServers = body.iceServerList.iceServers;
//                }
//                for (PeerConnection.IceServer iceServer : iceServers) {
//                    if (iceServer.credential == null) {
//                        PeerConnection.IceServer peerIceServer = PeerConnection.IceServer.builder(iceServer.url).createIceServer();
//                        peerIceServers.add(peerIceServer);
//                    } else {
//                        PeerConnection.IceServer peerIceServer = PeerConnection.IceServer.builder(iceServer.url)
//                                .setUsername(iceServer.username)
//                                .setPassword(iceServer.credential)
//                                .createIceServer();
//                        peerIceServers.add(peerIceServer);
//                    }
//                }
//                Log.d("onApiResponse", "IceServers\n" + iceServers.toString());
//            }
//
//            @Override
//            public void onFailure(@NonNull Call<TurnServerPojo> call, @NonNull Throwable t) {
//                t.printStackTrace();
//            }
//        });
    }


    public void start() {
        //Initialize PeerConnectionFactory globals.
        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(this)
                        .setEnableVideoHwAcceleration(true)
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);

        //Create a new PeerConnectionFactory instance - using Hardware encoder and decoder.
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();

        DefaultVideoEncoderFactory defaultVideoEncoderFactory = new DefaultVideoEncoderFactory(
                rootEglBase.getEglBaseContext(),  /* enableIntelVp8Encoder */true,  /* enableH264HighProfile */true);

        DefaultVideoDecoderFactory defaultVideoDecoderFactory = new DefaultVideoDecoderFactory(rootEglBase.getEglBaseContext());
        //peerConnectionFactory = new PeerConnectionFactory(options, defaultVideoEncoderFactory, defaultVideoDecoderFactory);

        PeerConnectionFactory peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
//                .setVideoDecoderFactory(defaultVideoDecoderFactory)
//                .setVideoEncoderFactory(defaultVideoEncoderFactory)
                .createPeerConnectionFactory();

        //Now create a VideoCapturer instance.
        VideoCapturer videoCapturerAndroid;
        videoCapturerAndroid = createCameraCapturer();

        MediaConstraints constraints=new MediaConstraints();

//        //Create MediaConstraints - Will be useful for specifying video and audio constraints.
//        audioConstraints = new MediaConstraints();
//        videoConstraints = new MediaConstraints();
//
//        //suggestion from GitHub
//        sdpConstraints = new MediaConstraints(); //was missing

        //Create a VideoSource instance
        if (videoCapturerAndroid != null) {
            videoSource = peerConnectionFactory.createVideoSource(videoCapturerAndroid);
        }

        SurfaceTextureHelper textureHelper=SurfaceTextureHelper.create(Thread.currentThread().getName(),rootEglBase.getEglBaseContext());

        videoCapturerAndroid.initialize(textureHelper,this,videoSource.getCapturerObserver());

            videoCapturerAndroid.startCapture(640, 480, 30);




        localVideoTrack = peerConnectionFactory.createVideoTrack("100", videoSource);
        localVideoTrack.setEnabled(true);


        //create an AudioSource instance
        audioSource = peerConnectionFactory.createAudioSource(constraints);
        localAudioTrack = peerConnectionFactory.createAudioTrack("101", audioSource);

        //videoCapturer.startCapture(1024,720,30);//capture in HD
        //videoCapturer.startCapture(640,480,30);//capture in SD



        localVideoView.setVisibility(View.VISIBLE);
        // And finally, with our VideoRenderer ready, we
        // can add our renderer to the VideoTrack.

        localVideoTrack.addSink(localVideoView);

//        final VideoRenderer localRenderer = new VideoRenderer(localVideoView);
//        localVideoTrack.addRenderer(localRenderer);

        gotUserMedia = true;
//        if (SignallingClient.getInstance().isInitiator) {
//            onTryToStart();
//        }
    }
    public void createLocalSocket() {
        Request request = new Request.Builder().url(socketAddress).build();
        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder();
        RdWebSocketListener listener = new RdWebSocketListener(this);
        listener.setPeerConnection(localPeer);
        webSocket = okHttpClientBuilder.build();
        wsListener = webSocket.newWebSocket(request, listener);
        listener.setWebSocket(wsListener);
        webSocket.dispatcher().executorService().shutdown();
    }

    /**
     * This method will be called directly by the app when it is the initiator and has got the local media
     * or when the remote peer sends a message through socket that it is ready to transmit AV data
     */
//    @Override
//    public void onTryToStart() {
//        runOnUiThread(() -> {
//            if (!SignallingClient.getInstance().isStarted && localVideoTrack != null && SignallingClient.getInstance().isChannelReady) {
//                createPeerConnection();
//                SignallingClient.getInstance().isStarted = true;
//                if (SignallingClient.getInstance().isInitiator) {
//                    doCall();
//                }
//            }
//        });
//    }


    /**
     * Creating the local peerconnection instance
     */
    private void createPeerConnection() {
        PeerConnection.RTCConfiguration rtcConfig =
                new PeerConnection.RTCConfiguration(peerIceServers);
        // TCP candidates are only useful when connecting to a server that supports
        // ICE-TCP.
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        // Use ECDSA encryption.
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA;
        localPeer = peerConnectionFactory.createPeerConnection(rtcConfig, new CustomPeerConnectionObserver("localPeerCreation") {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
//                onIceCandidateReceived(iceCandidate);
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                showToast("Received Remote stream");
                super.onAddStream(mediaStream);
                gotRemoteStream(mediaStream);
            }
        });

        addStreamToLocalPeer();
    }

    /**
     * Adding the stream to the localpeer
     */
    private void addStreamToLocalPeer() {
        //creating local mediastream
        MediaStream stream = peerConnectionFactory.createLocalMediaStream("102");
        stream.addTrack(localAudioTrack);
        stream.addTrack(localVideoTrack);
        localPeer.addStream(stream);
    }

    /**
     * This method is called when the app is initiator - We generate the offer and send it over through socket
     * to remote peer
     */
    private void doCall() {
        sdpConstraints = new MediaConstraints();
        sdpConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveVideo", "true"));
        localPeer.createOffer(new CustomSdpObserver("localCreateOffer") {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                localPeer.setLocalDescription(new CustomSdpObserver("localSetLocalDesc"), sessionDescription);
                Log.d("onCreateSuccess", "SignallingClient emit ");
                //SignallingClient.getInstance().emitMessage(sessionDescription);
            }
        }, sdpConstraints);
    }

    /**
     * Received remote peer's media stream. we will get the first video track and render it
     */
    private void gotRemoteStream(MediaStream stream) {
        //we have remote video stream. add to the renderer.
        final VideoTrack videoTrack = stream.videoTracks.get(0);
        runOnUiThread(() -> {
            try {
                remoteVideoView.setVisibility(View.VISIBLE);
                videoTrack.addSink(remoteVideoView);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }


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
    private void updateVideoViews(final boolean remoteVisible) {
        runOnUiThread(() -> {
            ViewGroup.LayoutParams params = localVideoView.getLayoutParams();
            if (remoteVisible) {
                params.height = dpToPx(100);
                params.width = dpToPx(100);
            } else {
                params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            }
            localVideoView.setLayoutParams(params);
        });

    }


    /**
     * Closing up - normal hangup and app destroye
     */

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.end_call: {
                hangup();
                break;
            }
        }
    }

    private void hangup() {
        try {
            localPeer.close();
            localPeer = null;
            //SignallingClient.getInstance().close();
            updateVideoViews(false);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onDestroy() {
//        SignallingClient.getInstance().close();
        super.onDestroy();
    }

    /**
     * Util Methods
     */
    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public void showToast(final String msg) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show());
    }

    private VideoCapturer createCameraCapturer() {




        CameraEnumerator enumerator;
        //if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP)
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

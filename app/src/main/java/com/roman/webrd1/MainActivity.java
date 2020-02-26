package com.roman.webrd1;


import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
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
import org.webrtc.VideoTrack;
import org.webrtc.VideoSource;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;

//public class MainActivity extends AppCompatActivity implements View.OnClickListener {
public class MainActivity extends AppCompatActivity {
    private String socketAddress = "http://10.0.2.2:9090";

    //private String socketAddress = "http://192.168.0.104:9090";
    //private String socketAddress = "http://ec2-18-188-37-20.us-east-2.compute.amazonaws.com:1337";


    private WebSocket wsListener;

    private PeerConnectionFactory peerConnectionFactory;
    private MediaConstraints audioConstraints;
    private MediaConstraints videoConstraints;
    private MediaConstraints sdpConstraints;
    //VideoSource videoSource;
    private VideoTrack localVideoTrack;
    private AudioSource audioSource;
    private AudioTrack localAudioTrack;
    private SurfaceViewRenderer localVideoView;
    private SurfaceViewRenderer remoteVideoView;
    private SurfaceTextureHelper textureHelper;

    private TextView statusTextView;

//    private Button hangup;
//    private Button startCall;
    private FloatingActionButton watchIndicator;
    private PeerConnection localPeer;
    private EglBase rootEglBase;

    //boolean gotUserMedia;
    public boolean isInitiator;
    public boolean isWebSocketConnected;
    public boolean isTryingReconnect;
    public boolean isTryingReconnectWebSocket;
    private List<PeerConnection.IceServer> peerIceServers = new ArrayList<>();

    private static final String TAG = "MainActivityr22";



    /* Constant values for the names of each respective lifecycle callback */
    private static final String ON_CREATE = "onCreate";
    private static final String ON_START = "onStart";
    private static final String ON_RESUME = "onResume";
    private static final String ON_PAUSE = "onPause";
    private static final String ON_STOP = "onStop";
    private static final String ON_RESTART = "onRestart";
    private static final String ON_DESTROY = "onDestroy";
    private static final String ON_SAVE_INSTANCE_STATE = "onSaveInstanceState";




    public static final String APP_ROLE_DISPLAY = "d";
    public static final String APP_ROLE_CAMERA = "c";


    public String localAppRole = APP_ROLE_CAMERA;
    private String localUserName = "rd1";

    private String localUserLogin = localUserName + "_" + localAppRole;
    private String localUserPassword = "pas4";


    private String remoteAppRole = "";
    private String remoteUser = ""; //localUserName + "_" + remoteAppRole; //"rd1_c";


// TODO reconnect after web socket failed, this also helps to initialize 'monitor' after 'camera'

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        Toolbar myToolbar = findViewById(R.id.my_toolbar);
//        setSupportActionBar(myToolbar);


        isInitiator = false;    //default value
        isWebSocketConnected=false;
        isTryingReconnect=false;
        isTryingReconnectWebSocket=false;
        sdpConstraints = new MediaConstraints(); //was missing

        if (localAppRole.equals(APP_ROLE_DISPLAY)){
            remoteAppRole=APP_ROLE_CAMERA;
        } else {
            remoteAppRole = APP_ROLE_DISPLAY;
        }

        remoteUser = localUserName + "_" + remoteAppRole;


        Logging.d(TAG, ON_CREATE);

        askForPermissions();

        setContentView(R.layout.activity_main);
        initViews();
        initVideos();
        //getIceServers();


        createFactories();

        if (localAppRole==APP_ROLE_CAMERA){
            startCamera();
            localVideoTrack.addSink(localVideoView);
        }


//        if (!isWebSocketConnected) {
//            createLocalSocket();
//            sendLogin(localUserLogin, localUserPassword, remoteUser);
//        }

        //tryToStart(); call this from WebSocketListener


        // https://developer.android.com/training/system-ui/navigation
        View decorView = getWindow().getDecorView();
        // Hide both the navigation bar and the status bar.
        // SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher, but as
        // a general rule, you should design your app to hide the status bar whenever you
        // hide the navigation bar.
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN ;
        decorView.setSystemUiVisibility(uiOptions);

        //setVideoViews();

        visibleWatchIndicator(false);
        if (localAppRole.equals(APP_ROLE_CAMERA)){
            showLocalVideo();
        }
        else {
            showStatusTextView();
        }
        logToServer();


    }

    @Override
    protected void onStart() {
        super.onStart();

        Logging.d(TAG, ON_START);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Logging.d(TAG,ON_RESUME);
    }

    @Override
    protected void onPause() {
        super.onPause();

        Logging.d(TAG,ON_PAUSE);
    }

    @Override
    protected void onStop() {

        super.onStop();

        Logging.d(TAG,ON_STOP);
    }


    @Override
    protected void onRestart() {
        super.onRestart();

        Logging.d(TAG,ON_RESTART);
    }

    @Override
    protected void onDestroy() {
        hangup();
        super.onDestroy();
        Logging.d(TAG,ON_DESTROY);


    }



    public void logToServer(){


        //check if we have a connection


        Thread thread = new Thread() {

            @Override
            public void run() {
                while (!isWebSocketConnected) {
                    // Block this thread for 2 seconds.
                    isTryingReconnectWebSocket=true;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                    createLocalSocket();
                }
                isTryingReconnectWebSocket=false;
                sendLogin(localUserLogin, localUserPassword, remoteUser);

            }

        };

        // start the thread.
        thread.start();
    }





    private void askForPermissions() {
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
//        hangup = findViewById(R.id.end_call);
//        startCall = findViewById(R.id.start_call);

       watchIndicator = findViewById(R.id.watching_indicator);


        //TODO clean the code
//        if (localAppRole.equals(APP_ROLE_CAMERA)){
            localVideoView =  findViewById(R.id.local_gl_surface_view);
//        } else {
            remoteVideoView = findViewById(R.id.remote_gl_surface_view);
//        }

        statusTextView = findViewById(R.id.text_status);


//        hangup.setOnClickListener(this);
//        startCall.setOnClickListener(this);
    }

    private void initVideos() {

        rootEglBase = EglBase.create();

        if(localAppRole.equals(APP_ROLE_CAMERA)){
            localVideoView.init(rootEglBase.getEglBaseContext(), null);
            //localVideoView.setZOrderMediaOverlay(true);
        }else {
            remoteVideoView.init(rootEglBase.getEglBaseContext(), null);
            //remoteVideoView.setZOrderMediaOverlay(true);
        }



    }



//    public static void setCameraDisplayOrientation(Activity activity,
//                                                   int cameraId, android.hardware.Camera camera) {
//        android.hardware.Camera.CameraInfo info =
//                new android.hardware.Camera.CameraInfo();
//        android.hardware.Camera.getCameraInfo(cameraId, info);
//        int rotation = activity.getWindowManager().getDefaultDisplay()
//                .getRotation();
//        int degrees = 0;
//        switch (rotation) {
//            case Surface.ROTATION_0: degrees = 0; break;
//            case Surface.ROTATION_90: degrees = 90; break;
//            case Surface.ROTATION_180: degrees = 180; break;
//            case Surface.ROTATION_270: degrees = 270; break;
//        }
//
//        int result;
//        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
//            result = (info.orientation + degrees) % 360;
//            result = (360 - result) % 360;  // compensate the mirror
//        } else {  // back-facing
//            result = (info.orientation - degrees + 360) % 360;
//        }
//        camera.setDisplayOrientation(result);
//    }

    //#############################################################################################################





    public void saveOfferAndAnswer(JSONObject json) throws JSONException {

        isInitiator=false;
        //tryToStart(500);

        createPeerConnection();



        //suggestion from GitHub



        if (localAppRole==APP_ROLE_CAMERA){
            sdpConstraints.mandatory.add(
                    new MediaConstraints.KeyValuePair("offerToReceiveAudio", "false"));
            sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                    "offerToReceiveVideo", "false"));

            MediaStream stream = peerConnectionFactory.createLocalMediaStream("102");
            stream.addTrack(localAudioTrack);
            stream.addTrack(localVideoTrack);
            localPeer.addStream(stream);

        } else {
            sdpConstraints.mandatory.add(
                    new MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"));
            sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                    "offerToReceiveVideo", "true"));
        }


        SessionDescription sessionDescription = new SessionDescription(SessionDescription.Type.OFFER, json.getString("sdp"));
        Logging.d(TAG, "before SET REMOTE DESCRIPTION inside saveOfferAndAnswer");
        localPeer.setRemoteDescription(new CustomSdpObserver("r22localPeer.SetRemoteDesc"), sessionDescription);
        Logging.d(TAG, "SET REMOTE DESCRIPTION inside saveOfferAndAnswer");

        //updateVideoViews(true);
        //setVideoViews();

        localPeer.createAnswer(new CustomSdpObserver("r22 remoteCreateOffer") {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                localPeer.setLocalDescription(new CustomSdpObserver("r22SetLocalDesc"), sessionDescription);
                Logging.d(TAG, "SET LOCAL DESCRIPTION after created answer");
                try {
                    JSONObject json = new JSONObject();
                    JSONObject answer = new JSONObject();
                    //json.put("type", sessionDescription.type);

                    answer.put("type", "answer");
                    answer.put("sdp", sessionDescription.description);

                    json.put("type", "answer");
                    json.put("answer",answer);
                    json.put("name", remoteUser);
                    wsListener.send(json.toString());
                    Logging.d(TAG, "Sending answer: " + json.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new MediaConstraints());

        Logging.d(TAG, "localPeer Answer created");
    }


    public void saveAnswer(JSONObject json) throws JSONException {
        SessionDescription sessionDescription;
        sessionDescription = new SessionDescription(SessionDescription.Type.ANSWER, json.getString("sdp"));
        //setRemoteDescription(sessionDescription);
        localPeer.setRemoteDescription(new CustomSdpObserver("r22localSetRemoteDesc"), sessionDescription);
        Logging.d(TAG, "SET REMOTE DESCRIPTION saveAnswer");

        //setVideoViews();
        //updateVideoViews(true);


        Logging.d(TAG, "Saving Answer ");
    }


    public void saveIceCandidate(JSONObject json) throws JSONException {
        IceCandidate iceCandidate = new IceCandidate(json.getString("sdpMid"),Integer.parseInt(json.getString("sdpMLineIndex")),json.getString("candidate"));

        localPeer.addIceCandidate(iceCandidate);
        Logging.d(TAG, "Saving iceCandidate");
    }



    public void addIceServers(String jsonString) throws JSONException{
        JSONArray myListsAll = new JSONArray(jsonString);

        if (myListsAll.length() < 1 ) return;

        PeerConnection.IceServer iceServer;


        for (int i = 0; i < myListsAll.length(); i++) {
            JSONObject jsonobject = (JSONObject) myListsAll.get(i);
            String urlIce = jsonobject.optString("url");
            String userIce = jsonobject.optString("username");
            String passwordIce = jsonobject.optString("password");

            if (userIce.isEmpty()) {
                iceServer = PeerConnection.IceServer.builder(urlIce)
                        .createIceServer();
            } else {
                iceServer = PeerConnection.IceServer.builder(urlIce)
                        .setUsername(userIce)
                        .setPassword(passwordIce)
                        //.setTlsCertPolicy(PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK)
                        .createIceServer();
            }
            peerIceServers.add(iceServer);
        }
    }



    private void createFactories() {
        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(this)
                        .setEnableVideoHwAcceleration(true)
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);

        //TODO check if we can remove it
        PeerConnectionFactory.Options options= new PeerConnectionFactory.Options();

        DefaultVideoEncoderFactory defaultVideoEncoderFactory = new DefaultVideoEncoderFactory(
                rootEglBase.getEglBaseContext(),  /* enableIntelVp8Encoder */true,  /* enableH264HighProfile */true);
        DefaultVideoDecoderFactory defaultVideoDecoderFactory = new DefaultVideoDecoderFactory(rootEglBase.getEglBaseContext());



        peerConnectionFactory=PeerConnectionFactory.builder()
                .setVideoEncoderFactory(defaultVideoEncoderFactory)
                .setVideoDecoderFactory(defaultVideoDecoderFactory)
                .createPeerConnectionFactory();
    }



    private void startCamera() {

        //Create MediaConstraints - Will be useful for specifying video and audio constraints.
        audioConstraints = new MediaConstraints();
        videoConstraints = new MediaConstraints();
        VideoCapturer videoCapturer=createCameraCapturer();
        //VideoSource videoSource=peerConnectionFactory.createVideoSource(false);
        VideoSource videoSource=peerConnectionFactory.createVideoSource(videoCapturer);
        localVideoTrack=peerConnectionFactory.createVideoTrack("100",videoSource);
        //localVideoTrack.setEnabled(true);
        audioSource=peerConnectionFactory.createAudioSource(audioConstraints);
        localAudioTrack=peerConnectionFactory.createAudioTrack("101",audioSource);
        //localAudioTrack.setEnabled(true);

        textureHelper=SurfaceTextureHelper.create(Thread.currentThread().getName(),rootEglBase.getEglBaseContext());
        videoCapturer.initialize(textureHelper,this,videoSource.getCapturerObserver());

        //videoCapturer.startCapture(1024,720,30);//capture in HD
        videoCapturer.startCapture(640,480,15);//capture in SD
        //videoCapturer.startCapture(320,240,30);//capture in LD


        //Logging.enableTracing("logcat:", EnumSet.of(Logging.TraceLevel.TRACE_DEFAULT));
        //Logging.enableLogToDebugOutput(Logging.Severity.LS_VERBOSE);
        //org.webrtcLogging.enableLogToDebugOutput(Logging.Severity.LS_VERBOSE);
    }


    private void createLocalSocket() {

        Request request = new Request.Builder().url(socketAddress).build();
        RdWebSocketListener listener = new RdWebSocketListener(this);
        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder();
        OkHttpClient webSocket = okHttpClientBuilder.build();
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
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
        //rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXCOMPAT;
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
        //rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.NEGOTIATE;
        //rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_ONCE;


        // Use ECDSA encryption.
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA;


        localPeer = peerConnectionFactory.createPeerConnection(rtcConfig, new RdPeerConnectionObserver("localPeerCreation") {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                Log.d("Mainr22", "onIceCandidate");
                super.onIceCandidate(iceCandidate);
                try {
                    JSONObject candidate = new JSONObject();
                    JSONObject json = new JSONObject();
                    json.put("type", "candidate");

                    candidate.put("candidate", iceCandidate.sdp);
                    candidate.put("sdpMid", iceCandidate.sdpMid);
                    candidate.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);

                    json.put("candidate", candidate);
                    json.put("name", remoteUser);
                    wsListener.send(json.toString());
                    //Log.d("createPeer22Connection->", "sent onIceCandidate=" + json.toString() );
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }


            @Override
            public void onAddStream(MediaStream mediaStream) {
                //("Received Remote stream");
                Log.d("createPeer22Connection->", "on AddStreamReceived Remote stream" );
                super.onAddStream(mediaStream);

                if (localAppRole==APP_ROLE_DISPLAY){
                    final VideoTrack videoTrack = mediaStream.videoTracks.get(0);
                    runOnUiThread(() -> {
                        try {
                            //remoteVideoView.setVisibility(View.VISIBLE);
                            videoTrack.addSink(remoteVideoView);
                            //setVideoViews();
                            showRemoteVideo();

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }


            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                super.onIceGatheringChange(iceGatheringState);

            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                super.onIceConnectionChange(iceConnectionState);
                /* TODO fix multiple reconnecting
                    reconnect when PeerConnection.IceConnectionState == DISCONNECTED or FAILED
                * */
//                TODO reconnect after websocket failed
//                TODO fix error after RdWebSocketListener22: onMessage text : {"type":"leave"}
//                TODO in display mode there is no reconnection after change the orientation of a phone

                //iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED ||
                if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED){

                    if (localAppRole==APP_ROLE_DISPLAY){
                        showStatusTextView();
                    }
                    else {
                        showLocalVideo();
                    }

                    visibleWatchIndicator(false);

                }

                if (iceConnectionState == PeerConnection.IceConnectionState.CONNECTED){
                    if (localAppRole==APP_ROLE_CAMERA){
                        showLocalVideo();
                        visibleWatchIndicator(true);
                    }
                    else {
                        showRemoteVideo();
                    }
                }

                if (iceConnectionState == PeerConnection.IceConnectionState.FAILED){
//                        hangup();
//                        createLocalSocket();
//                        sendLogin(localUserLogin, localUserPassword, remoteUser);
                }
            }
        });
        Log.d("createPeer22Connection:", "DONE");
    }



    //@Override

    public void tryToStart(Integer delay_ms) {
        Log.d("TryToStartr22", "delay=" + delay_ms.toString());
        showToast("Connecting...");


//        Handler handler = new Handler(Looper.getMainLooper()) {
//
//            @Override
//            public void handleMessage(Message inputMessage) {
//
//                if (localVideoTrack != null) {
//                    createPeerConnection();
////                SignallingClient.getInstance().isStarted = true;
//                    if (isInitiator) {
//                        doCall();
//                    }
//                }
//
//
//            }


//        new CountDownTimer(delay_ms, 1000) {
//
//            public void onTick(long millisUntilFinished) {
//                //mTextField.setText("seconds remaining: " + millisUntilFinished / 1000);
//            }
//
//            public void onFinish() {
//                runOnUiThread(() -> {
//                    if (localVideoTrack != null ) {
//                        createPeerConnection();
////                SignallingClient.getInstance().isStarted = true;
//                        if (isInitiator) {
//                            doCall();
//                        }
//                    }
//                });
//            }
//        }.start();


        Thread thread = new Thread() {

            @Override
            public void run() {

                // Block this thread for 2 seconds.
                try {
                    Thread.sleep(delay_ms);
                } catch (InterruptedException e) {
                }


                // After sleep finished blocking, create a Runnable to run on the UI Thread.
                runOnUiThread(() -> {
//                    if (localVideoTrack != null) {
                        createPeerConnection();
                        // TODO check if we can remove 'isInitiator' variable
                        //if (isInitiator) {
                        if (localAppRole.equals(APP_ROLE_CAMERA)) {
                            doCall();
                        }
                        isTryingReconnect=false;
//                    }
                });

            }

        };

// Don't forget to start the thread.
        thread.start();


    }


    /**
     * This method is called when the app is initiator - We generate the offer and send it over through socket
     * to remote peer
     */
    private void doCall() {

        //sendLogin("rd1");
        //sdpConstraints = new MediaConstraints();



        if (localAppRole==APP_ROLE_CAMERA){
            sdpConstraints.mandatory.add(
                    new MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"));
            sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                    "offerToReceiveVideo", "true"));
            MediaStream stream = peerConnectionFactory.createLocalMediaStream("102");
            stream.addTrack(localAudioTrack);
            stream.addTrack(localVideoTrack);
            localPeer.addStream(stream);

        } else  {
            sdpConstraints.mandatory.add(
                    new MediaConstraints.KeyValuePair("offerToReceiveAudio", "false"));
            sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                    "offerToReceiveVideo", "false"));
            MediaStream stream = peerConnectionFactory.createLocalMediaStream("102");
            localPeer.addStream(stream);
        }




        localPeer.createOffer(new CustomSdpObserver("r22localCreateOffer")
        {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                localPeer.setLocalDescription(new CustomSdpObserver("localSetLocalDescr22####"), sessionDescription);
                Log.d("Mainr22", "localPeer.createOffer SET LOCAL DESCRIPTION = [" + sessionDescription + "]");
                sendOffer(sessionDescription);
            }
        }
        , sdpConstraints);

        //Log.d("Mainr22", "localPeer.createOffer DONE");
        //Log.d("AFTERcreateOffer->", "DONE");

    }

    @SuppressWarnings("SameParameterValue")
    private void sendLogin(String myName, String myPassword, String otherUser) {
        try {
            JSONObject json = new JSONObject();

            json.put("type", "login");
            json.put("name", myName);
            json.put("password",myPassword);
            json.put("otherUser",otherUser);
            wsListener.send(json.toString());

        } catch (Throwable e) {
            Log.e("SendLogin", "Uncaught exception is: ", e);
        }
    }

    public void sendLeave() {
        try {
            JSONObject json = new JSONObject();
            json.put("type", "leave");
            json.put("name", localUserLogin);
            wsListener.send(json.toString());

        } catch (Throwable e) {
            Log.e("SendLogin", "Uncaught exception is: ", e);
        }
    }
    private void sendOffer(SessionDescription sessionDescription) {
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

    private void showLocalVideo(){

        visibleLocalVideoView(true);
        visibleRemoteVideoView(false);
        visibleStatusTextView(false);

        localVideoView.setMirror(false);
    }

    private void showRemoteVideo(){
        visibleLocalVideoView(false);
        visibleRemoteVideoView(true);
        visibleStatusTextView(false);
        remoteVideoView.setMirror(false);
    }


    private void showStatusTextView(){
        visibleLocalVideoView(false);
        visibleRemoteVideoView(false);
        visibleStatusTextView(true);
    }

//    private void setVideoViews(){
//
//        runOnUiThread(() -> {
//            if (localAppRole.equals(APP_ROLE_CAMERA)) {
//                    visibleLocalVideoView(true);
//                    visibleRemoteVideoView(false);
//                    visibleStatusTextView(false);
//                    localVideoTrack.addSink(localVideoView);
//                    localVideoView.setMirror(false);
//            } else {
//                    visibleLocalVideoView(false);
//                    visibleRemoteVideoView(true);
//                    visibleStatusTextView(false);
//                    remoteVideoView.setMirror(false);
//            }
//        });





//        runOnUiThread(() -> {
//            if (localAppRole.equals(APP_ROLE_CAMERA)) {
//                remoteVideoView.setVisibility((View.INVISIBLE));
//                statusTextView.setVisibility(View.INVISIBLE);
//                localVideoView.setVisibility(View.VISIBLE);
//                localVideoTrack.addSink(localVideoView);
//                localVideoView.setMirror(false);
//            } else {
//                localVideoView.setVisibility(View.INVISIBLE);
//                statusTextView.setVisibility(View.VISIBLE);
//                remoteVideoView.setVisibility((View.INVISIBLE));
////            remoteVideoView.setZOrderOnTop(true);   // !caused no video visible !
//                remoteVideoView.setMirror(false);
//            }
//        });

//    }

    private void visibleRemoteVideoView(final boolean viewVisible) {
        runOnUiThread(() -> {
            ViewGroup.LayoutParams params = remoteVideoView.getLayoutParams();
            if (viewVisible) {
                params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

            } else {
                params.height = 0;
            }
            remoteVideoView.setLayoutParams(params);

        });
        Log.d("Mainr22", "VisibleRemoteVideoView=" + viewVisible );
    }




    private void visibleLocalVideoView(final boolean viewVisible) {
        runOnUiThread(() -> {
            ViewGroup.LayoutParams params = localVideoView.getLayoutParams();
            if (viewVisible) {
                params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

            } else {
                params.height = 0;
            }
            localVideoView.setLayoutParams(params);

        });
        Log.d("Mainr22", "VisibleLocalVideoView=" + viewVisible );
    }


    private void visibleStatusTextView(final boolean viewVisible) {
        runOnUiThread(() -> {

            ViewGroup.LayoutParams params = statusTextView.getLayoutParams();
            if (viewVisible) {
                params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

            } else {
                params.height = 0;
            }
            statusTextView.setLayoutParams(params);

        });
        Log.d("Mainr22", "VisibleTextView=" + viewVisible );
    }


    private void visibleWatchIndicator(final boolean indicatorVisible){
        runOnUiThread(() -> {

            ViewGroup.LayoutParams params = watchIndicator.getLayoutParams();
            if (indicatorVisible) {
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;

            } else {
                params.height = 0;
            }
            watchIndicator.setLayoutParams(params);

        });
    }


    private void OLD_updateVideoViews(final boolean remoteVisible) {
        runOnUiThread(() -> {
            ViewGroup.LayoutParams params = localVideoView.getLayoutParams();
            if (remoteVisible) {
                params.height = dpToPx(100);
                params.width = dpToPx(100);
            } else {
                params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            }
            localVideoView.setLayoutParams(params);
        });

    }


    /**
     * Closing up - normal hangup and app destroye
     */

//    @Override
//    public void onClick(View v) {
//        switch (v.getId()) {
//            case R.id.end_call: {
//                //sendLeave();
//                hangup();
//                break;
//            }
//            case R.id.start_call: {
//                //doCall();
//                //sendLogin("rd1");
//                //testCreateOffer2();
//
//                isInitiator=true;
//                if (!isWebSocketConnected) {
//                    createLocalSocket();
//                    sendLogin(localUserLogin, localUserPassword, remoteUser);
//                } else {
//                    tryToStart(0);
//                }
//
//                break;
//            }
//        }
//    }



    public void hangup() {
        try {
            if (localPeer != null) {
                localPeer.close();
                localPeer = null;
            }
            sendLeave();
            wsListener.close(1000, null);
            //isWebSocketConnected=false;
            //updateVideoViews(false);
            //setVideoViews();
            showStatusTextView();



        } catch (Exception e) {
            e.printStackTrace();
        }

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
    //private CapturerObserver createCameraCapturer(){

    CameraEnumerator enumerator;

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP ){
            enumerator = new Camera2Enumerator(this);
        } else {
            enumerator = new Camera1Enumerator(true);
        }

        Logging.d(TAG, "Looking for back facing cameras.");

        String[] devicenames = enumerator.getDeviceNames();

        for (String dn : devicenames) {
            if (enumerator.isBackFacing(dn)) {
                Logging.d(TAG, "Creating back facing camera capturer.");
                return enumerator.createCapturer(dn, null);

            }
        }
        //failed to get front facing cam
        Logging.d(TAG, "Looking for other cameras.");
        for (String dn : devicenames) {
            if (!enumerator.isBackFacing(dn)) {
                Logging.d(TAG, "Creating other camera capturer.");
                return enumerator.createCapturer(dn, null);
            }
        }
        //failed to find both
        Logging.d(TAG, "Failed to find any camera.");
        return null;

    }
}

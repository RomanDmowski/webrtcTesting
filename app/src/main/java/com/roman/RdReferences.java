package com.roman;

import okhttp3.WebSocket;

public class RdReferences {
    private static final RdReferences ourInstance = new RdReferences();



    private static WebSocket webSocket;

    public static synchronized RdReferences getInstance() {
        return ourInstance;
    }


    private RdReferences() {
    }


//    public static void setWebSocket(WebSocket _webSocket){
//        webSocket=_webSocket;
//    }

    public static synchronized WebSocket getWebSocket() {
        return webSocket;
    }



}

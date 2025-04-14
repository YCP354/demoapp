package com.example.socket;


import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class WebSocketServerImpl extends WebSocketServer {

    private static final String TAG = "WebSocketServerImpl";
    private final ServerListener listener;
    private final Set<WebSocket> clients = Collections.synchronizedSet(new HashSet<>());

    public WebSocketServerImpl(int port, ServerListener listener) {
        super(new InetSocketAddress(port));
        this.listener = listener;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        clients.add(conn);
        if (listener != null) {
            listener.onClientConnected(conn);
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        clients.remove(conn);
        if (listener != null) {
            listener.onClientDisconnected(conn);
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        if (listener != null) {
            listener.onReceiverClientMessage(conn, message);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (conn != null) {
            clients.remove(conn);
        }
        Log.d(TAG, "onError() called with: conn = [" + conn + "], ex = [" + ex + "]");
    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart() called");
    }

    public void broadcastMessage(String message) {
        for (WebSocket socket : clients) {
            socket.send(message);
        }
    }

    public int getClientCount() {
        return clients.size();
    }

    public interface ServerListener {
        void onClientConnected(WebSocket conn);
        void onReceiverClientMessage(WebSocket conn, String message);
        void onClientDisconnected(WebSocket conn);
    }
}
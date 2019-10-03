package samples.easychatroom2.client;

import org.gwtproject.rpc.websockets.shared.Callback;
import org.teavm.jso.browser.Location;
import org.teavm.jso.browser.Window;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Int8Array;
import org.teavm.jso.websocket.WebSocket;
import samples.easychatroom2.shared.ChatClient;
import samples.easychatroom2.shared.ChatServer;
import samples.easychatroom2.shared.ChatServer_Impl;

import java.nio.ByteBuffer;

public class SampleClient {
    public static void main(String[] args) {

        StringBuilder url = new StringBuilder();
        if (Location.current().getProtocol().equals("http:")) {
            url.append("ws://");
        } else if (Location.current().getProtocol().equals("https:")) {
            url.append("wss://");
        }
        url.append(Location.current().getHost());
        url.append("/chat");

        ChatServer server = ServerBuilder.of(ChatServer_Impl::new)
                .setConnectionErrorHandler(err -> {
                })
                .setUrl(url.toString())
                .start(new ChatClient() {
                    private ChatServer server;

                    @Override
                    public void onOpen() {
                        System.out.println("onOpen");

                        server.login("teavm user", Callback.of(success -> {
                            server.say("Hello, world!");
                        }));
                    }

                    @Override
                    public void onClose(int closeCode, String closeMessage) {
                        System.out.println("onClose(" + closeCode + ", \"" + closeMessage + "\")");
                    }

                    @Override
                    public void onError(Throwable error) {
                        error.printStackTrace();
                    }

                    @Override
                    public void setServer(ChatServer server) {
                        this.server = server;
                    }

                    @Override
                    public ChatServer getServer() {
                        return server;
                    }

                    @Override
                    public void say(String username, String message) {
                        System.out.println(username + ": " + message);
                    }

                    @Override
                    public void join(String username) {
                        System.out.println(username + " has joined the chat");
                    }

                    @Override
                    public void part(String username) {
                        System.out.println(username + " has left the chat");
                    }

                    @Override
                    public void ping(Callback<Void, Void> callback) {
                        callback.onSuccess(null);
                    }
                });
    }
}

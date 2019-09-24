package samples.easychatroom2.server;

import org.gwtproject.rpc.vertx.server.websocket.AbstractServerImpl;
import org.gwtproject.rpc.websockets.shared.Callback;
import samples.easychatroom2.shared.ChatClient;
import samples.easychatroom2.shared.ChatServer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ChatServerImpl extends AbstractServerImpl<ChatServer, ChatClient> implements ChatServer {
    private static final Map<ChatClient, String> loggedIn = Collections.synchronizedMap(new HashMap<>());

    @Override
    public void onClose(Connection connection, ChatClient client) {
        String userName = loggedIn.remove(client);
        if (userName == null) {
            return;
        }

        for (ChatClient connected : loggedIn.keySet()) {
            connected.part(userName);
        }
    }

    @Override
    public void login(String username, Callback<Void, String> callback) {
        System.out.println("login: " + username);
        if (username == null || username.length() == 0) {
            callback.onFailure("Non-empty username required");
        }

        ChatClient c = getClient();
        if (loggedIn.containsKey(c)) {
            callback.onFailure("Already logged in");
        }
        for (String name : loggedIn.values()) {
            if (name.equals(username)) {
                callback.onFailure("Username already in use");
            }
        }
        for (ChatClient connected : loggedIn.keySet()) {
            connected.join(username);
        }
        loggedIn.put(c, username);
        callback.onSuccess(null);

        final long start = System.nanoTime();
        getClient().ping(new Callback<Void, Void>() {
            @Override
            public void onFailure(Void reason) {
                System.err.println("failed login ping");
            }

            @Override
            public void onSuccess(Void result) {
                System.out.println("login ping in " + (System.nanoTime() - start) / 1000000.0 + "milliseconds");
            }
        });
    }

    @Override
    public void say(String message) {
        System.out.println("say: " + message);
        ChatClient c = getClient();
        String userName = loggedIn.get(c);

        for (ChatClient connected : loggedIn.keySet()) {
            connected.say(userName, message);
        }
    }

    @Override
    public void onError(Throwable error) {
        error.printStackTrace();
    }

}

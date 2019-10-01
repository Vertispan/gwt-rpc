package samples.easychatroom2.cli;

import org.gwtproject.rpc.websockets.okhttp.ServerBuilder;
import org.gwtproject.rpc.api.Callback;
import samples.easychatroom2.shared.ChatClient;
import samples.easychatroom2.shared.ChatServer;
import samples.easychatroom2.shared.ChatServer_Impl;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        //two args, the url of the server, and the username to use
        String url = args[0];
        String username = args[1];

        //build a client instance so we can print details for the user
        ChatClient client = new ChatClient() {
            private ChatServer server;
            @Override
            public void say(String username, String message) {

                System.out.printf("[%s]: %s\n", username, message);
            }

            @Override
            public void join(String username) {
                System.out.println(username + " has entered the chat");
            }

            @Override
            public void part(String username) {
                System.out.println(username + " has left the chat");
            }

            @Override
            public void ping(Callback<Void, Void> callback) {
                callback.onSuccess(null);
            }

            @Override
            public void onOpen() {
                System.out.println("-- connection open --");
            }

            @Override
            public void onClose(int closeCode, String closeReason) {
                System.out.println("-- connection closed, " + closeCode + ": " + closeReason + " --");
                System.exit(0);
            }

            @Override
            public void onError(Throwable error) {
                error.printStackTrace();
                System.exit(1);
            }

            @Override
            public void setServer(ChatServer server) {
                this.server = server;
            }

            @Override
            public ChatServer getServer() {
                return server;
            }
        };

        // create and connect to the server
        ChatServer server = ServerBuilder.of(ChatServer_Impl::new).setUrl(url).start(client);

        // log in
        server.login(username, Callback.of(ignore -> System.out.println("Login Successful")));

        // until user sends ctrl-d, send all messages to server
        Scanner input = new Scanner(System.in);
        while (input.hasNextLine()) {
            server.say(input.nextLine());
        }

        // close connection, user is done
        server.close();
    }
}

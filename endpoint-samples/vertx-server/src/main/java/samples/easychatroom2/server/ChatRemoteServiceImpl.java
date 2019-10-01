package samples.easychatroom2.server;

import org.gwtproject.rpc.api.Callback;
import samples.easychatroom2.shared.ChatRemoteServiceAsync;

public class ChatRemoteServiceImpl implements ChatRemoteServiceAsync {
    @Override
    public void send(String message, Callback<String, String> callback) {
        try {
            if (Math.random() > .7) {
                throw new IllegalStateException("Sample exception");
            }

            callback.onSuccess("Successfully sent " + message);
        } catch (Exception ex) {
            callback.onFailure("Error occurred: " + (ex.getMessage() == null ? ex.toString() : ex.getMessage()));
        }
    }
}

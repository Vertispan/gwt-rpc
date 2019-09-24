package org.gwtproject.rpc.vertx.server.websocket;

import org.gwtproject.rpc.websockets.shared.Client;
import org.gwtproject.rpc.websockets.shared.Server;

public abstract class AbstractServerImpl<S extends Server<S, C>, C extends Client<C, S>> {
    private C client;

    //	@Override
    public void onOpen(Server.Connection connection, C client) {
        // default empty implementation to allow clients to define this only if desired
    }

    //	@Override
    public void onClose(Server.Connection connection, C client) {
        // default empty implementation to allow clients to define this only if desired
    }

    //	@Override
    public C getClient() {
        return client;
    }

    //	@Override
    public void setClient(C client) {
        this.client = client;
    }

    //	@Override
    public final void close() {
        throw new IllegalStateException("This method may not be called on the server, only on the client. To close the connection, invoke WebSocketConnection.close() on the connection you want to stop.");
    }
}

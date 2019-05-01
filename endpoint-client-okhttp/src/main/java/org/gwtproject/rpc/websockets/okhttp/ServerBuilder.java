package org.gwtproject.rpc.websockets.okhttp;

import org.gwtproject.rpc.websockets.okhttp.impl.ServerBuilderImpl;
import org.gwtproject.rpc.websockets.shared.Client;
import org.gwtproject.rpc.websockets.shared.Server;
import org.gwtproject.rpc.websockets.shared.impl.AbstractEndpointImpl;

/**
 * Base interface to be extended and given a concrete Server interface in a client project,
 * causing code to be generated to connect to a websocket server.
 */
public interface ServerBuilder<S extends Server<? super S, ?>> {
    /**
     * Sets the full url, including protocol, host, port, and path for the next server connection
     * to be started. If called with a non-null value, will override the other setters in this
     * builder.
     *
     * @param url
     * @return
     */
    ServerBuilder<S> setUrl(String url);
//
//    /**
//     * Sets the path for the next server to be started. Defaults to the RemoteServiceRelativePath
//     * annotation value for the Server interface, if any.
//     *
//     * @param path
//     * @return
//     */
//    ServerBuilder<S> setPath(String path);
//
//    /**
//     * Sets the port of the next server instance to be started. Defaults to the port the current
//     * page loaded from.
//     *
//     * @param port
//     * @return
//     */
//    ServerBuilder<S> setPort(int port);
//
//    /**
//     * Sets the hostname for the next server to be started. Defaults to the hostname the current
//     * page loaded from.
//     *
//     * @param hostname
//     * @return
//     */
//    ServerBuilder<S> setHostname(String hostname);
//
//    /**
//     * Sets the protocol ("ws" or "wss") to connect with. Defaults to wss if the current page
//     * loaded using https, and ws otherwise.
//     *
//     * @param protocol
//     * @return
//     */
//    ServerBuilder<S> setProtocol(String protocol);

    /**
     * Creates a new instance of the specified server type, starts, and returns it. May
     * be called more than once to create additional connections, such as after the first
     * is closed.
     *
     * @return
     */
    <C extends Client<C, ? extends S>>  S start(C client);

//    /**
//     * Specifies a handler to receive errors when a problem occurs around the protocol: the connection,
//     * serialization, or other unhandled issues.
//     * @param errorHandler the handler to send connection errors to
//     */
//    void setConnectionErrorHandler(ConnectionErrorHandler errorHandler);
//
//    /**
//     * Allows de/serialization and connection problems to be handled rather than rethrowing
//     * or just pushing to GWT.log.
//     */
//    public interface ConnectionErrorHandler {
//        void onError(Object ex);
//    }

    /**
     * Simple create method that takes the generated server endpoint's constructor and returns a functioning
     * server builder.
     */
    static <E extends Server<E, L>, L extends Client<L, E>> ServerBuilder<E> of(AbstractEndpointImpl.EndpointImplConstructor<E> constructor) {
        return new ServerBuilderImpl<E>(constructor);
    }
}
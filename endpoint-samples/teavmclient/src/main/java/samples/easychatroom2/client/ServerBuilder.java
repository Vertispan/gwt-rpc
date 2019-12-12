package samples.easychatroom2.client;

import org.gwtproject.rpc.api.Client;
import org.gwtproject.rpc.api.Server;
import org.gwtproject.rpc.api.impl.AbstractEndpointImpl;

public interface ServerBuilder<S extends Server<? super S, ?>> {
    <C extends Client<C, ? extends S>> S start(C client);

    ServerBuilder<S> setUrl(String url);

    /**
     * Specifies a handler to receive errors when a problem occurs around the protocol: the connection,
     * serialization, or other unhandled issues.
     * @param errorHandler the handler to send connection errors to
     */
    ServerBuilder<S> setConnectionErrorHandler(ConnectionErrorHandler errorHandler);

    /**
     * Allows de/serialization and connection problems to be handled rather than rethrowing
     * or just pushing to a logger.
     */
    public interface ConnectionErrorHandler {
        void onError(Object ex);
    }


    static <E extends Server<E, L>, L extends Client<L, E>> ServerBuilder<E> of (AbstractEndpointImpl.EndpointImplConstructor<E> constructor) {
        return new ServerBuilderImpl<E>(constructor);
    }
}

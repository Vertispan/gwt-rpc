package samples.easychatroom2.client;

import org.gwtproject.rpc.api.Client;
import org.gwtproject.rpc.api.Server;
import org.gwtproject.rpc.api.impl.AbstractEndpointImpl;
import org.gwtproject.rpc.api.impl.AbstractEndpointImpl.EndpointImplConstructor;
import org.gwtproject.rpc.api.impl.AbstractWebSocketServerImpl;
import org.gwtproject.rpc.serialization.stream.bytebuffer.ByteBufferSerializationStreamReader;
import org.gwtproject.rpc.serialization.stream.bytebuffer.ByteBufferSerializationStreamWriter;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Int8Array;
import samples.easychatroom2.client.impl.WebSocket;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class ServerBuilderImpl<S extends Server<? super S, ?>> implements ServerBuilder<S> {

    private static class ServerImpl<S extends Server<S, C>, C extends Client<C, S>> {
        private WebSocket websocket;
        private final ConnectionErrorHandler errorHandler;
        private final S endpoint;
        private Consumer<ArrayBuffer> onmessage;

        public ServerImpl(String url, EndpointImplConstructor<S> constructor, ConnectionErrorHandler errorHandler, C client) {
            endpoint = constructor.create(
                    serializer -> {
                        ByteBufferSerializationStreamWriter writer = new ByteBufferSerializationStreamWriter(serializer);
                        writer.prepareToWrite();
                        return writer;
                    },
                    stream -> {
                        ByteBuffer fullPayload = stream.getFullPayload();
                        Int8Array arr = Int8Array.create(fullPayload.remaining());
                        for (int i = 0; i < arr.getLength(); i++) {
                            arr.set(i, fullPayload.get());
                        }
                        websocket.send(arr.getBuffer());
                    },
                    (send, serializer) -> {
                        onmessage = message -> {
                            Int8Array arr = Int8Array.create(message);
                            byte[] javaArr = new byte[arr.getLength()];
                            for (int i = 0; i < arr.getLength(); i++) {
                                javaArr[i] = arr.get(i);
                            }
                            ByteBuffer bb = ByteBuffer.wrap(javaArr);
                            send.accept(new ByteBufferSerializationStreamReader(serializer, bb));
                        };
                    }
            );

            websocket = WebSocket.create(url + "?checksum=" + ((AbstractWebSocketServerImpl<?, ?>) endpoint).getChecksum());
            this.errorHandler = errorHandler;
            websocket.setBinaryType("arraybuffer");
            websocket.onClose(e -> {
                endpoint.getClient().onClose(e.getCode(), e.getReason());
            });
            websocket.onOpen(e -> {
                endpoint.getClient().onOpen();
            });
            websocket.onMessage(event -> {
                onmessage.accept(event.getDataAsArray());
            });
            websocket.onError(e -> {
                if (errorHandler != null) {
                    errorHandler.onError(e);
                } else {
                    System.out.println("A transport error occurred - pass a error handler to your server builder to handle this yourself: " + e);
                }
            });
            ((AbstractWebSocketServerImpl<?, ?>) endpoint).close = websocket::close;

            endpoint.setClient(client);
            client.setServer(endpoint);
        }

        public S getEndpoint() {
            return endpoint;
        }
    }

    private String url;
    private ConnectionErrorHandler errorHandler;

    private final AbstractEndpointImpl.EndpointImplConstructor<S> constructor;

    public ServerBuilderImpl(EndpointImplConstructor<S> constructor) {
        this.constructor = constructor;
    }

    @Override
    public ServerBuilder<S> setConnectionErrorHandler(ConnectionErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }
    
    public ConnectionErrorHandler getErrorHandler() {
        return errorHandler;
    }

    @Override
    public ServerBuilder<S> setUrl(String url) {
        this.url = url;
        return this;
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    @Override
    public <C extends Client<C, ? extends S>> S start(C client) {
        //noinspection unchecked
        return (S) new ServerImpl(url, constructor, errorHandler, client).getEndpoint();
    }
}

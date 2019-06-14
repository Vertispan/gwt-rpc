package org.gwtproject.rpc.websockets.okhttp.impl;

import okhttp3.*;
import okio.ByteString;
import org.gwtproject.rpc.serialization.stream.bytebuffer.ByteBufferSerializationStreamReader;
import org.gwtproject.rpc.serialization.stream.bytebuffer.ByteBufferSerializationStreamWriter;
import org.gwtproject.rpc.websockets.okhttp.ServerBuilder;
import org.gwtproject.rpc.websockets.shared.Client;
import org.gwtproject.rpc.websockets.shared.Server;
import org.gwtproject.rpc.websockets.shared.impl.AbstractEndpointImpl;
import org.gwtproject.rpc.websockets.shared.impl.AbstractWebSocketServerImpl;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class ServerBuilderImpl<S extends Server<? super S, ?>> implements ServerBuilder<S> {
    private final AbstractEndpointImpl.EndpointImplConstructor<S> constructor;

    private static class ServerImpl<S extends Server<S, C>, C extends Client<C, S>> {
        private WebSocket websocket;
        private S endpoint;
        private Consumer<ByteBuffer> onMessage;

        public ServerImpl(Request.Builder reqBuilder, AbstractEndpointImpl.EndpointImplConstructor<S> constructor, C client) {

            WebSocketListener listener = new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    endpoint.getClient().onOpen();
                    super.onOpen(webSocket, response);
                }

                @Override
                public void onMessage(WebSocket webSocket, ByteString bytes) {
                    onMessage.accept(bytes.asByteBuffer());
                }

                @Override
                public void onClosing(WebSocket webSocket, int code, String reason) {
                    webSocket.close(code, "Server closed connection");
                    endpoint.getClient().onClose();
                    super.onClosed(webSocket, code, reason);
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    endpoint.getClient().onError(t);
                    super.onFailure(webSocket, t, response);
                }
            };

            endpoint = constructor.create(
                    serializer -> {
                        ByteBufferSerializationStreamWriter writer = new ByteBufferSerializationStreamWriter(serializer);
                        writer.prepareToWrite();
                        return writer;
                    },
                    stream -> websocket.send(ByteString.of(stream.getFullPayload())),
                    (send, serializer) -> {
                        onMessage = buffer -> {
                            send.accept(new ByteBufferSerializationStreamReader(serializer, buffer));
                        };
                    }
            );
            endpoint.setClient(client);
            client.setServer(endpoint);

            // Append the checksum querystring. This should be entirely safe since the builder isn't exposed to client code at all
            Request withoutQueryString = reqBuilder.build();
            Request.Builder withQueryString = withoutQueryString.newBuilder()
                    .url(withoutQueryString.url().toString() + "?checksum=" + ((AbstractWebSocketServerImpl<?, ?>) endpoint).getChecksum());

            websocket = new OkHttpClient().newWebSocket(withQueryString.build(), listener);

            ((AbstractWebSocketServerImpl<?, ?>)endpoint).close = () -> websocket.close(1000, null);
        }

        public S getEndpoint() {
            return endpoint;
        }
    }
    private Request.Builder reqBuilder = new Request.Builder();
//    private URL urlBuilder = new URL(DomGlobal.window.location.getHref());
//    private ConnectionErrorHandler errorHandler;

    /**
     *
     */
    public ServerBuilderImpl(AbstractEndpointImpl.EndpointImplConstructor<S> constructor) {
        this.constructor = constructor;
    }

//    @Override
//    public void setConnectionErrorHandler(ConnectionErrorHandler errorHandler) {
//        this.errorHandler = errorHandler;
//    }
//    public ConnectionErrorHandler getErrorHandler() {
//        return errorHandler;
//    }

    @Override
    public ServerBuilder<S> setUrl(String url) {
        reqBuilder.url(url);
        return this;
    }

//    /**
//     * @return the url
//     */
//    public String getUrl() {
//        return url == null ? urlBuilder.toString_() : url;
//    }

//    @Override
//    public ServerBuilder<S> setProtocol(String protocol) {
//        urlBuilder.protocol = protocol;
//        return this;
//    }
//    @Override
//    public ServerBuilder<S> setHostname(String hostname) {
//        urlBuilder.host = hostname;
//        return this;
//    }
//    @Override
//    public ServerBuilder<S> setPort(int port) {
//        urlBuilder.port = "" + port;
//        return this;
//    }
//    @Override
//    public ServerBuilder<S> setPath(String path) {
//        urlBuilder.pathname = path;
//        return this;
//    }


    @Override
    public <C extends Client<C, ? extends S>> S start(C client) {
        return (S) new ServerImpl(reqBuilder, constructor, client).getEndpoint();
    }

}

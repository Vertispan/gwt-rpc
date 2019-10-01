package org.gwtproject.rpc.vertx.server.websocket;

import io.netty.handler.codec.http.QueryStringDecoder;
import io.vertx.core.buffer.Buffer;
import org.gwtproject.rpc.serialization.stream.bytebuffer.ByteBufferSerializationStreamReader;
import org.gwtproject.rpc.serialization.stream.bytebuffer.ByteBufferSerializationStreamWriter;
import org.gwtproject.rpc.api.Client;
import org.gwtproject.rpc.api.Server;

import io.vertx.core.Handler;
import io.vertx.core.http.ServerWebSocket;
import org.gwtproject.rpc.api.impl.AbstractEndpointImpl;
import org.gwtproject.rpc.api.impl.AbstractWebSocketClientImpl;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class RpcWebSocketHandler<S extends Server<S, C>, C extends Client<C, S>> implements Handler<ServerWebSocket> {
    private final AbstractEndpointImpl.EndpointImplConstructor<C> clientConstructor;
    private final Supplier<S> serverFactory;

    public RpcWebSocketHandler(AbstractEndpointImpl.EndpointImplConstructor<C> clientConstructor, Supplier<S> serverFactory) {
        this.clientConstructor = clientConstructor;
        this.serverFactory = serverFactory;
    }

    @Override
    public void handle(ServerWebSocket incomingWebsocket) {
        // assume that we've been set up correctly to only handle our own events
        // this is a new socket, so could allocate a new server instance to talk to?
        C instance = clientConstructor.create(
                typeSerializer -> {
                    ByteBufferSerializationStreamWriter writer = new ByteBufferSerializationStreamWriter(typeSerializer);
                    writer.prepareToWrite();
                    return writer;
                },
                writer -> {
                    // TODO prevent this from copying the whole payload
                    ByteBuffer fullPayload = writer.getFullPayload();
                    Buffer buffer = Buffer.buffer(fullPayload.array())
                            .slice(fullPayload.position(), fullPayload.limit());

                    incomingWebsocket.writeFinalBinaryFrame(buffer);
                },
                (onMessage, serializer) -> {
                    incomingWebsocket.frameHandler(frame -> {
                        // TODO prevent this from copying the whole payload
                        ByteBuffer byteBuffer = frame.binaryData().getByteBuf().nioBuffer();
                        onMessage.accept(new ByteBufferSerializationStreamReader(serializer, byteBuffer));
                    });
                }
        );

        List<String> checksum = new QueryStringDecoder(incomingWebsocket.uri()).parameters().get("checksum");
        if (checksum == null || checksum.isEmpty()) {
            incomingWebsocket.close((short) 1003, "The checksum query parameter was not specified, cannot accept request");
            return;
        } else if (checksum.size() > 1) {
            incomingWebsocket.close((short) 1003, "More than one checksum query parameter specified, cannot accept request");
            return;
        } else {
            String expected = ((AbstractWebSocketClientImpl<?, ?>) instance).getChecksum();
            String actual = checksum.iterator().next();
            if (!expected.equals(actual)) {
                incomingWebsocket.close((short) 1003, "Expected checksum with value " + expected);
                return;
            }
        }

        S server = serverFactory.get();
        server.setClient(instance);
        instance.setServer(server);

        VertxConnection connection = new VertxConnection(incomingWebsocket);

        incomingWebsocket.exceptionHandler(server::onError);
        incomingWebsocket.closeHandler(c -> server.onClose(connection, instance));

        server.onOpen(connection, instance);
    }

    public static class VertxConnection implements Server.Connection {
        private final ServerWebSocket webSocket;
        private final ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<>();

        public VertxConnection(ServerWebSocket webSocket) {
            this.webSocket = webSocket;
        }

        @Override
        public void data(String key, Object value) {
            map.put(key, value);
        }

        @Override
        public Object data(String key) {
            return map.get(key);
        }

        @Override
        public void close() {
            webSocket.close();
        }

        @Override
        public void close(int closeCode, String closeReason) {
            webSocket.close((short) closeCode, closeReason);
        }
    }
}

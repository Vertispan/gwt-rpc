package org.gwtproject.rpc.vertx.server.xhr;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.gwtproject.rpc.serialization.stream.string.StringSerializationStreamReader;
import org.gwtproject.rpc.serialization.stream.string.StringSerializationStreamWriter;
import org.gwtproject.rpc.api.Endpoint;
import org.gwtproject.rpc.api.RemoteService;
import org.gwtproject.rpc.api.impl.AbstractEndpointImpl;
import org.gwtproject.rpc.api.impl.AbstractNoRemoteImpl;

import java.util.List;
import java.util.function.Supplier;

public class RpcXhrHandler<S extends RemoteService.RemoteServiceAsync> implements Handler<HttpServerRequest> {

    public static final String STRONG_NAME_HEADER = "X-GWT-RPC-Checksum";
    //TODO move above to the shared interface

    private static final String GWT_RPC_CONTENT_TYPE = "text/x-gwt-rpc";
    private static final String CHARSET_UTF8_NAME = "UTF-8";

    private static final String CONTENT_TYPE_APPLICATION_JSON_UTF8 = "application/json; charset=utf-8";
    private static final String CONTENT_DISPOSITION = "Content-Disposition";
    private static final String ATTACHMENT = "attachment";



    private final AbstractEndpointImpl.EndpointImplConstructor<Endpoint.NoRemoteEndpoint<S>> clientFactory;
    private final Supplier<S> serverFactory;

    public RpcXhrHandler(AbstractEndpointImpl.EndpointImplConstructor<Endpoint.NoRemoteEndpoint<S>> clientFactory, Supplier<S> serverFactory) {
        this.clientFactory = clientFactory;
        this.serverFactory = serverFactory;
    }


    @Override
    public void handle(HttpServerRequest event) {
        String contentType = event.getHeader("Content-Type");
        if (contentType == null) {
            event.response().setStatusCode(400).setStatusMessage("Content-Type was null, expected '" + GWT_RPC_CONTENT_TYPE + "'.").end();
            return;
        }
        if (!contentType.startsWith(GWT_RPC_CONTENT_TYPE)) {// allow a suffix with charset
            event.response().setStatusCode(400).setStatusMessage("Content-Type was '" + contentType + "', expected '" + GWT_RPC_CONTENT_TYPE + "'.").end();
            return;
        }

        //TODO character encoding?

        Endpoint.NoRemoteEndpoint<S> c = clientFactory.create(
                typeSerializer -> {
                    StringSerializationStreamWriter writer = new StringSerializationStreamWriter(typeSerializer);
                    writer.prepareToWrite();
                    return writer;
                },
                writer -> {

                    String response = writer.toString();
                    event.response()
                            .putHeader("Content-Type", CONTENT_TYPE_APPLICATION_JSON_UTF8)
                            .putHeader(CONTENT_DISPOSITION, ATTACHMENT)
                            .end(response);
                },
                ((onMessage, serializer) -> {
                    //endhandler never goes off synchronously, so this is safe
                    event.bodyHandler(buffer -> {
                        onMessage.accept(new StringSerializationStreamReader(serializer, buffer.toString()));
                    });
                }));

        S instance = serverFactory.get();

        List<String> checksum = event.headers().getAll(STRONG_NAME_HEADER);
        if (checksum == null || checksum.isEmpty()) {
            event.response().setStatusCode(400).setStatusMessage("The checksum query parameter was not specified, cannot accept request").end();
            return;
        } else if (checksum.size() > 1) {
            event.response().setStatusCode(400).setStatusMessage("More than one checksum query parameter specified, cannot accept request").end();
            return;
        } else {
            String expected = ((AbstractNoRemoteImpl<?>) c).getChecksum();
            String actual = checksum.iterator().next();
            if (!expected.equals(actual)) {
                event.response().setStatusCode(400).setStatusMessage("Expected checksum with value " + expected).end();
                return;
            }
        }

        c.setRemote(instance);

    }
}

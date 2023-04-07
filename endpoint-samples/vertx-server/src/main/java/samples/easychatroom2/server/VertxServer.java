package samples.easychatroom2.server;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import org.gwtproject.rpc.vertx.server.websocket.RpcWebSocketHandler;
import org.gwtproject.rpc.vertx.server.xhr.RpcXhrHandler;
import samples.easychatroom2.shared.*;

public class VertxServer {

    public static void main(String[] args) {

        Vertx vertx = Vertx.vertx();
        HttpServer server = vertx.createHttpServer(new HttpServerOptions().setCompressionSupported(true));

        Router router = Router.router(vertx);

        // Note that the endpoint-server-vertx project depends only on vertx-core, not vertx-web, so we don't have
        // a handler which deals with RoutingContexts, just HttpServerRequest and ServerWebSocket instances.

        // ==== Websocket setup ====
        // I'm not sure of the idiomatic way to create this - but this lets us only allocate a single handler instance and
        // make new client/server instances as needed.
        // Note that it is up to your own implementation of the Server endpoint how you wish to create it - a single instance
        // could be shared if that is appropriate for your code, threading model. Presently the client proxy can only be created
        // this way, since it has connection-specific variables to deal with threading properly.
        RpcWebSocketHandler<ChatServer, ChatClient> websocketHandler = new RpcWebSocketHandler<>(ChatClient_Impl::new, ChatServerImpl::new);

        // When any request comes in on the /chat route, try to upgrade to a websocket and send it off to the websocket handler
        router.route("/chat").handler(req -> req.request().toWebSocket().onSuccess(websocketHandler));

        // ==== XHR setup ====
        // Similarly to the above, we create a handler once, with simple ways to create client and server instances
        RpcXhrHandler<ChatRemoteServiceAsync> rpcServerHandler = new RpcXhrHandler<>(ChatRemoteServiceAsync_ImplRemote::new, ChatRemoteServiceImpl::new);

        // This one handler then is wired up to the POST /hello route
        router.post("/hello").handler(req -> rpcServerHandler.handle(req.request()));


        // ==== GWT war content ====
        // This next line is necessary to read content from the war, which is being shaded in as if it were a jar. Normal
        // usages of vertx shouldn't require this, as long as your compiled app content can be served normally.
        router.get("/samples.easychatroom2.ChatSample/*").handler(StaticHandler.create("samples.easychatroom2.ChatSample"));

        // ==== Normal static content ====
        // Serve other static content normally
        router.get("/*").handler(StaticHandler.create());

        server.requestHandler(router).listen(8080);


    }
}

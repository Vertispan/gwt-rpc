package samples.easychatroom2.server;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import org.gwtproject.rpc.vertx.server.websocket.RpcWebSocketHandler;
import org.gwtproject.rpc.vertx.server.xhr.RpcXhrHandler;
import samples.easychatroom2.shared.ChatClient_Impl;
import samples.easychatroom2.shared.ChatRemoteServiceAsync_ImplRemote;

public class VertxServer {

    public static void main(String[] args) {

        Vertx vertx = Vertx.vertx();
        HttpServer server = vertx.createHttpServer(new HttpServerOptions().setCompressionSupported(true));

        Router router = Router.router(vertx);

        router.route("/chat").handler(req -> {
            new RpcWebSocketHandler<>(ChatClient_Impl::new, ChatServerImpl::new)
                    .handle(req.request().upgrade());
        });

        router.post("/hello").handler(req -> {new RpcXhrHandler<>(ChatRemoteServiceAsync_ImplRemote::new, ChatRemoteServiceImpl::new).handle(req.request());});

        router.get("/samples.easychatroom2.ChatSample/*").handler(StaticHandler.create("samples.easychatroom2.ChatSample"));
        router.get("/*").handler(StaticHandler.create());



//        server.websocketHandler(websocket -> {
//        });

        server.requestHandler(router).listen(8080);


    }
}

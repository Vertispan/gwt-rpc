package samples.easychatroom2.client.impl;

import org.teavm.jso.JSBody;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.ArrayBufferView;

public abstract class WebSocket extends org.teavm.jso.websocket.WebSocket {
    @JSBody(params = "url", script = "return new WebSocket(url);")
    public static native WebSocket create(String url);


    public abstract void send(ArrayBuffer data);
    public abstract void send(ArrayBufferView data);

}

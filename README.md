#GWT-RPC

(Now with more annotation processors!)

The purpose of this project is to bring the easy to use "GWT-RPC" tooling to GWT3 and to other clients
other than GWT. The same basic ideas hold - create asynchronous interfaces to describe the api
of the remote service, and serialize Java objects in predictable ways based on those methods.

The project is broken into several discrete pieces, to better facilitate sharing and reusing code.
The serialization mechanism is in several pieces, allowing field serializers to be written or generated
regardless of how they are sent over the wire, and the wire format to likewise be selected without 
consideration for how the data will be written. Then, generating the proxies required for endpoints
to communicate with each other is separate still, each able to select its own wire format and be used
from any platform they provide support for.

To use this, start with a three-module project. The "shared" module should consist only of the endpoint
interfaces and the types they reference, then any "client" or "server" modules (gwt, jvm, or otherwise)
then depend on the "shared" module and the code generated for it. It isn't strictly necessary that there
even be more than two, just "shared" and the code which uses it (for example a client using the 
serialization for a version-dependent cache), but providing that split will improve how much work the 
annotation processor must do when things change.

The "shared" module then should depend on the gwt-rpc-api jar, which includes the basics required to
annotate an interface so that it will be understood by the processor, and gwt-rpc-processor itself to
generate the endpoints (and in turn the serializers). Optionally, you can depend on gwt-rpc-serializer-api
so that custom field serializers can be defined or to build your own serializer without any endpoints. 
Skipping endpoint generation means you can skip gwt-rpc-api and gwt-rpc-processor, and instead directly
include gwt-rpc-serializer-processor. In GWT2 you will also need to add an `inherits` statement to 
correctly indicate to the compiler that you are using this project. Note that in the future, these may be
split into more discrete dependencies, to better limit what code the compiler ends up processing.

Selecting the right dependency for your client and server will depend on what runtime each is using -
java-servlet vs vertx server, gwt or jvm for the client. Each module has its own documentation on how to
"turn it on" and make it work.



# Old Readme from serialization emul
#GWT Serializable Objects

This project takes the base wiring of GWT-RPC and rebuilds it using annotation processors. This
has several benefits:

 * Dev mode and other recompiles do not take the time to build the SerializableTypeOracles
   (though of course that work must still be done in the build, but if your IDE or build tool
   detects that this work doesn't need to be done at all, it will be skipped).
 * RPC mechanisms that use the generated serialization stream tool can run the same on any
   Java-compatible platform, including the JVM (both client and server), Android, iOS (via
   J2ObjC), and of course the browser, via GWT.
 * As an updated version of GWT-RPC, this can be updated to be more flexible with regard to
   "type explosion" issues, as well as other customizations as we add them.


Aspects of this project, and their progress:

 * SerializableTypeOracleBuilder - the meat of RPC, the part that identifies which classes are
   eligible to be instantiated, serialized, and deserialized on either side of the wire. About
   70% complete:
   * Partially implemented TypeConstrainer, allows us to remove some potential serializers
   * Missing some handling of generic arrays
   * Missing some logging
   * Future improvements should include optional "asserts" that assist in knowing exactly
     why some types are not eligible for serialization
   * Type name obfuscation, simple access to a "version" as was available in classic GWT-RPC.
 * Serialization Wiring processor - the "biggest" annotation processor that should be part of
   RPC. Other RPC tools (serializing to blob, sending to another iframe, worker, websocket,
   xhr/fetch call) should generate an interface annotated with this, to limit when rebuilds need
   to occur. About 95% complete (I.E. seems functional, just missing a new annotation to supply
   reference material on external classes to consider as subclasses, and some cleanup, testing).
 * Package/artifact scanning tools - These will come in several forms, but their end goal is to
   let developers keep the "type explosion" firmly under their control. They will allow
   specifying packages to scan directly, or pre-build lists of classes in external/upstream
   jars that are compatible with the platforms that the generated serializer can be used on.
   * One implementation is complete, scanning a jar for all classes, and allowing that list to 
     be passed directly to a the main processor.
 * Serialization formats - These can be specified by downstream projects, but we'll probably try
   to collect them here too. Three complete and working examples (designed for classic GWT-RPC,
   but should be compatible with the new variety) that will be available in examples:
    * The pipe-delimited string format, but into GWT-RPC that we all know and love
    * ByteBuffer-based binary format, emulated in JavaScript through the use of TypedArrays,
      without any Strings, so that the message payload can be passed efficiently over a
      MessagePort, such as to another iframe, or between worker and page
    * ByteBuffer-based binary format, including Strings written out, allowing a relatively
      compact format to be written to a WebSocket, sent over fetch/xhr, or stored as a Blob.
      This may include the use of compression to minimize bytes stored or sent, at the possible
      cost of some time.
 * More options on automatic serialization than getters/setters, such as "violator-pattern"
   `private`/`final` field access, reflection on platforms that support it, builders and
   factories and `@ConstructorProperties` and more...
 * Option to disable polymorphism on the stream, and always assume that the declared type is the
   only possible type that can be provided there, or that a custom serializer is available to
   handle any possible polymorphism. This will reduce the size of the stream in specific cases,
   and can allow for non-object oriented data to read and write data (JSON, C, etc).
 * JPA or other bytecode enhancement support (opaque to most clients), if requested.

#Old readme from gwt-webbit

(Name in flux, probably will be renamed to gwt-websockets)

GWT library to make RPC calls over websockets. As in regular GWT-RPC, the client can call the server at
any time, but with this library, the server can also call back the the client.

WebSockets are established from the client to the server, and while open are maintained to allow them
to continue to communicate over the same channel. Either side can send a string to the other at any time.
This project uses this to enable one-way RPC methods to easily be sent.

The default setup for this is to only use a pair of interfaces, one for the client, and one for the server.
Since the client API needs to know about the server and vice versa, generics are used so that either side
of the API can see to the other.

Methods can optionally take a `Callback<T,F>` object as the final parameter, but instead of the server
implementing the same method synchronously, both client and server code are written to assume async behavior.
Note however that callbacks are often not required - messages can be passed without *expecting* a reply back.
Note also that callbacks are one-time use, and cannot be invoked multiple times - use a different method
on the opposite interface to achieve that effect.

[JSR-356](https://www.jcp.org/en/jsr/detail?id=356) is used presently as the only server-side implementation
(the spec for javax.websocket, implemented by [Glassfish](https://tyrus.java.net/),
[Jetty](http://www.eclipse.org/jetty/documentation/current/jetty-javaee.html#jetty-javaee-7), and
[Tomcat](tomcat.apache.org/tomcat-7.0-doc/web-socket-howto.html)). The library uses version 1.0 of this
API, as Jetty (and perhaps others) do not yet support 1.1. 

A new project has also been started adding rpc-like communication between web/shared/service workers.


## Example

### Client-Server Contract

These interfaces refer to each other in their generics. Here is a simple client interface for a chat app:

    /**
     * Simple example of methods implemented by a GWT client that can be called from the server
     *
     */
    @Endpoint
    public interface ChatClient extends Client<ChatClient, ChatServer> {
    	/**
    	 * Tells the client that a user posted a message to the chat room
    	 * @param username the user who sent the message
    	 * @param message the message the user sent
    	 */
    	void say(String username, String message);

    	/**
    	 * Indicates that a new user has entered the chat room
    	 * @param username the user who logged in
    	 */
    	void join(String username);

    	/**
    	 * Indicates that a user has left the chat room
    	 * @param username the user who left
    	 */
    	void part(String username);

    	/**
    	 * Test method to have the server send the client a message and get a response right away.
    	 * This demonstrates that the server can call the client with a callback to get its response
    	 * other than via a Server method.
    	 *
    	 * @param callback response that the client should call upon receipt of this method
    	 */
    	void ping(Callback<Void, Void> callback);
    }

The client code must implement this interface, and will be called when the server invokes any of those
methods. Once implemented, the client may do anything with these details - fire events to the rest of the
app, call other methods, directly interact with the UI, etc.

    /**
     * Simple example of methods a server can have that can be invoked by a client.
     *
     */
    @Endpoint
    public interface ChatServer extends Server<ChatServer, ChatClient> {
    	/**
    	 * Brings the user into the chat room, with the given username
    	 * @param username the name to use
    	 * @param callback indicates the login was successful, or passes back an error message
    	 */
    	void login(String username, Callback<Void, String> callback);

    	/**
    	 * Sends the given message to the chatroom
    	 * @param message the message to say to the room
    	 */
    	void say(String message);
    }

In this matching server interface, we can see the messages that any client can send to the server after
connecting. The server in turn must implement this, and may call back to the client using any of the
methods defined in the first interface.

Both interfaces get a `@Endpoint` annotation, indicating that the annotation processor should look at
them and construct an actual implementation to communicate over the wire.

### Client Wiring
In client code, first build an implementation of the client interface - this will be its way of receiving all
'callbacks' from the server. To connect to the server, we create a `ServerBuilder` which we'll configure
with the url to connect to the server, and details about creating the generated server endpoint:

    ServerBuilder<ChatServer> builder = ServerBuilder.of(ChatServer_Impl::new);

Now we can set the remote URL, create the connection, and attach the local "client" implementation.

    		// Create an instance of the build
    		final ChatServerBuilder builder = GWT.create(ChatServerBuilder.class);
    		// Set the url to connect to - may or may not be the same as the original server
    		builder.setUrl("ws://" + Window.Location.getHost() + "/chat");

    		// Get an instance of the client impl
    		ChatClient impl = ...;

    		// Start a connection to the server, and attach the client impl
    		final ChatServer server = builder.start();
    		server.setClient(impl);

Once the `onOpen()` method gets called on the client object, the connection is established, and the client
may invoke any server method until `onClose()` is called. To restart the connection (or start another
simultaneous connection), call `start()` again on the same instance and then talk to the newly returned
server object.

The `AbstractClientImpl` class can serve as a handy base class, providing default implementations of the
`onOpen()` and `onClose()` methods that fire events.

### Server Wiring

With either API there is an `AbstractServerImpl` class. This provides the working details of the `Server`
interface as well as the specifics how to interact with JSR-356.

From within either client or server implementation, you can always get a reference to the other side - the
server can call `getClient()`, and the client already has an instance (see below). Our ChatServerImpl
probably needs to track all connected clients, so we'll start off with a field to hold them:

    public class ChatServerImpl extends AbstractServerImpl<ChatServer, ChatClient> implements ChatServer {
    	private final Map<ChatClient, String> loggedIn =
    	            Collections.synchronizedMap(new HashMap<ChatClient, String>());

Next, we'll want to implement each method - for example, when any user says something, we'll send it to all
other connected users:

    	@Override
    	public void say(String message) {
    		ChatClient c = getClient();
    		String userName = loggedIn.get(c);

    		for (ChatClient connected : loggedIn.keySet()) {
    			connected.say(userName, message);
    		}
    	}

Note that this is not the only way to keep several clients in communication with each other, just one
possible implementation, made deliberately simple for the sake of an example. For a larger solution, some
kind of message queue could be used to send out messages to the proper recipients.

### JSR-356
For the `javax.websocket` api, we have two basic options, as documented at http://docs.oracle.com/javaee/7/tutorial/doc/websocket.htm.
The simplest way to do this is usually the [annotated approach](http://docs.oracle.com/javaee/7/tutorial/doc/websocket004.htm#BABFEBGA),
by which the endpoint class must be decorated with `@ServerEndpoint()` to indicate the url it should be
used to respond to. The other annotations are already present in the `RpcEndpoint` class (the superclass
of `AbstractServerImpl`).

    @ServerEndpoint("/chat")
    public class ChatServerImpl extends AbstractServerImpl<ChatServer, ChatClient> implements ChatServer {
    	//...

Check out the [javaee-websocket-gwt-rpc-sample](javaee-websocket-gwt-rpc-sample/) project for a working,
runnable example of the above code.

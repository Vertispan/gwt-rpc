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

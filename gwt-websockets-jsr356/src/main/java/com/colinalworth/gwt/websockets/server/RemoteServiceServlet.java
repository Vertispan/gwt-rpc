/*
 * #%L
 * gwt-websockets-jsr356
 * %%
 * Copyright (C) 2011 - 2018 Vertispan LLC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.colinalworth.gwt.websockets.server;

import com.colinalworth.gwt.websockets.shared.Endpoint.NoRemoteEndpoint;
import com.colinalworth.gwt.websockets.shared.RemoteService.RemoteServiceAsync;
import com.colinalworth.gwt.websockets.shared.impl.AbstractEndpointImpl.EndpointImplConstructor;
import com.google.gwt.user.client.rpc.SerializationException;
import com.vertispan.serial.streams.string.StringSerializationStreamReader;
import com.vertispan.serial.streams.string.StringSerializationStreamWriter;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.zip.GZIPOutputStream;

/**
 * Handles POST calls with a similar API to GWT2-style RPC. The primary difference is
 * that since no reflection is used to handle serialization or dispatch, both the policy
 * wiring is absent, and this class must be created differently.
 */
public abstract class RemoteServiceServlet<S extends RemoteServiceAsync> extends HttpServlet {
	/**
	 * Used by {@link #doSetContentType}.
	 */
	public static final String CONTENT_TYPE_HEADER = "Content-Type";

	/**
	 * Used by {@link #doFinish}.
	 */
	/*
	 * NB: Also used by RpcServlet.
	 */
	public static final String MODULE_BASE_HEADER = "X-GWT-Module-Base";

	/**
	 * Used by {@link #doFinish}.
	 */
	/*
	 * NB: Also used by AbstractRemoteServiceServlet.
	 */
	public static final String STRONG_NAME_HEADER = "X-GWT-Permutation";
	//TODO move above to the shared interface

	private static final String GWT_RPC_CONTENT_TYPE = "text/x-gwt-rpc";
	public static final String CHARSET_UTF8_NAME = "UTF-8";
	/**
	 * The UTF-8 Charset. Use this to avoid concurrency bottlenecks when
	 * converting between byte arrays and Strings.
	 * See http://code.google.com/p/google-web-toolkit/issues/detail?id=6398
	 */
	public static final Charset CHARSET_UTF8 = Charset.forName(CHARSET_UTF8_NAME);
	static final int BUFFER_SIZE = 4096;

	/**
	 * Controls the compression threshold at and below which no compression will
	 * take place.
	 */
	private static final int UNCOMPRESSED_BYTE_SIZE_LIMIT = 256;
	private static final String ACCEPT_ENCODING = "Accept-Encoding";
	private static final String ATTACHMENT = "attachment";
	private static final String CONTENT_ENCODING_GZIP = "gzip";
	private static final String CONTENT_DISPOSITION = "Content-Disposition";
	private static final String CONTENT_ENCODING = "Content-Encoding";
	private static final String CONTENT_TYPE_APPLICATION_JSON_UTF8 = "application/json; charset=utf-8";

	private static final String GENERIC_FAILURE_MSG = "The call failed on the server; see server log for details";

	protected transient ThreadLocal<HttpServletRequest> perThreadRequest;
	protected transient ThreadLocal<HttpServletResponse> perThreadResponse;

	/**
	 * The implementation of the service.
	 */
	private final S delegate;
	private final EndpointImplConstructor<NoRemoteEndpoint<S>> clientFactory;

	/**
	 * The default constructor used by service implementations that
	 * extend this class.  The servlet will delegate AJAX requests to
	 * the appropriate method in the subclass.
	 */
	public RemoteServiceServlet(EndpointImplConstructor<NoRemoteEndpoint<S>> clientFactory) {
		this.delegate = (S) this;
		this.clientFactory = clientFactory;
	}

	/**
	 * The wrapping constructor used by service implementations that are
	 * separate from this class.  The servlet will delegate AJAX
	 * requests to the appropriate method in the given object.
	 */
	public RemoteServiceServlet(S delegate, EndpointImplConstructor<NoRemoteEndpoint<S>> clientFactory) {
		this.delegate = delegate;
		this.clientFactory = clientFactory;
	}

	@Override
	protected final void doPost(HttpServletRequest request, HttpServletResponse response) {
		// Ensure the thread-local data fields have been initialized

		try {
			// Store the request & response objects in thread-local storage.
			//
			synchronized (this) {
				validateThreadLocalData();
				perThreadRequest.set(request);
				perThreadResponse.set(response);
			}

			processPost(request, response);

		} catch (Throwable e) {
			// Give a subclass a chance to either handle the exception or rethrow it
			//
			doUnexpectedFailure(e);
		} finally {
			// null the thread-locals to avoid holding request/response
			//
			perThreadRequest.set(null);
			perThreadResponse.set(null);
		}
	}

	/**
	 * Override this method to control what should happen when an exception
	 * escapes the {@link #doPost} method. The default implementation will log the
	 * failure and send a generic failure response to the client.
	 * <p>
	 * An "expected failure" is an exception thrown by a service method that is
	 * declared in the signature of the service method. These exceptions are
	 * serialized back to the client, and are not passed to this method. This
	 * method is called only for exceptions or errors that are not part of the
	 * service method's signature, or that result from SecurityExceptions,
	 * SerializationExceptions, or other failures within the RPC framework.
	 * <p>
	 * Note that if the desired behavior is to both send the GENERIC_FAILURE_MSG
	 * response AND to rethrow the exception, then this method should first send
	 * the GENERIC_FAILURE_MSG response itself (using getThreadLocalResponse), and
	 * then rethrow the exception. Rethrowing the exception will cause it to
	 * escape into the servlet container.
	 *
	 * @param e the exception which was thrown
	 */
	protected void doUnexpectedFailure(Throwable e) {
		try {
			getThreadLocalResponse().reset();
		} catch (IllegalStateException ex) {
			/*
			 * If we can't reset the request, the only way to signal that something
			 * has gone wrong is to throw an exception from here. It should be the
			 * case that we call the user's implementation code before emitting data
			 * into the response, so the only time that gets tripped is if the object
			 * serialization code blows up.
			 */
			throw new RuntimeException("Unable to report failure", e);
		}
		ServletContext servletContext = getServletContext();
		writeResponseForUnexpectedFailure(servletContext,
				getThreadLocalResponse(), e);
	}

	/**
	 * Called when the servlet itself has a problem, rather than the invoked
	 * third-party method. It writes a simple 500 message back to the client.
	 *
	 * @param servletContext
	 * @param response
	 * @param failure
	 */
	public static void writeResponseForUnexpectedFailure(
			ServletContext servletContext, HttpServletResponse response,
			Throwable failure) {
		servletContext.log("Exception while dispatching incoming RPC call", failure);

		// Send GENERIC_FAILURE_MSG with 500 status.
		//
		try {
			response.setContentType("text/plain");
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			try {
				response.getOutputStream().write(GENERIC_FAILURE_MSG.getBytes(CHARSET_UTF8));
			} catch (IllegalStateException e) {
				// Handle the (unexpected) case where getWriter() was previously used
				response.getWriter().write(GENERIC_FAILURE_MSG);
			}
		} catch (IOException ex) {
			servletContext.log(
					"respondWithUnexpectedFailure failed while sending the previous failure to the client",
					ex);
		}
	}

	/**
	 * Returns the strong name of the permutation, as reported by the client that
	 * issued the request, or <code>null</code> if it could not be determined.
	 * This information is encoded in the
	 * {@value com.google.gwt.user.client.rpc.RpcRequestBuilder#STRONG_NAME_HEADER}
	 * HTTP header.
	 */
	protected final String getPermutationStrongName() {
		return getThreadLocalRequest().getHeader(STRONG_NAME_HEADER);
	}

	/**
	 * Gets the <code>HttpServletRequest</code> object for the current call. It is
	 * stored thread-locally so that simultaneous invocations can have different
	 * request objects.
	 */
	protected final HttpServletRequest getThreadLocalRequest() {
		synchronized (this) {
			validateThreadLocalData();
			return perThreadRequest.get();
		}
	}

	/**
	 * Gets the <code>HttpServletResponse</code> object for the current call. It
	 * is stored thread-locally so that simultaneous invocations can have
	 * different response objects.
	 */
	protected final HttpServletResponse getThreadLocalResponse() {
		synchronized (this) {
			validateThreadLocalData();
			return perThreadResponse.get();
		}
	}

	/**
	 * Override this method to examine the deserialized version of the request
	 * before the call to the servlet method is made. The default implementation
	 * does nothing and need not be called by subclasses.
	 *
	 * @param rpcRequest
	 */
	@Deprecated
	protected void onAfterRequestDeserialized(Object rpcRequest) {
	}

	/**
	 * This method is called by {@link #processCall(String)} and will throw a
	 * SecurityException if {@link #getPermutationStrongName()} returns
	 * <code>null</code>. This method can be overridden to be a no-op if there are
	 * clients that are not expected to provide the
	 * {@value com.google.gwt.user.client.rpc.RpcRequestBuilder#STRONG_NAME_HEADER}
	 * header.
	 *
	 * @throws SecurityException if {@link #getPermutationStrongName()} returns
	 *           <code>null</code>
	 */
	protected void checkPermutationStrongName() throws SecurityException {
		if (getPermutationStrongName() == null) {
			throw new SecurityException(
					"Blocked request without GWT permutation header (XSRF attack?)");
		}
	}

	/**
	 * Process a call originating from the given request. This method calls
	 * {@link RemoteServiceServlet#checkPermutationStrongName()} to prevent
	 * possible XSRF attacks and then decodes the <code>payload</code> using
	 * {@link RPC#decodeRequest(String, Class, SerializationPolicyProvider)}
	 * to do the actual work.
	 * Once the request is decoded {@link RemoteServiceServlet#processCall(RPCRequest)}
	 * will be called.
	 * <p>
	 * Subclasses may optionally override this method to handle the payload in any
	 * way they desire (by routing the request to a framework component, for
	 * instance). The {@link HttpServletRequest} and {@link HttpServletResponse}
	 * can be accessed via the {@link #getThreadLocalRequest()} and
	 * {@link #getThreadLocalResponse()} methods.
	 * </p>
	 * This is public so that it can be unit tested easily without HTTP.
	 *
	 * @param payload the UTF-8 request payload
	 * @return a string which encodes either the method's return, a checked
	 *         exception thrown by the method, or an
	 *         {@link IncompatibleRemoteServiceException}
	 * @throws SerializationException if we cannot serialize the response
	 * @throws UnexpectedException if the invocation throws a checked exception
	 *           that is not declared in the service method's signature
	 * @throws RuntimeException if the service method throws an unchecked
	 *           exception (the exception will be the one thrown by the service)
	 */
	public String processCall(String payload) throws SerializationException {
		// First, check for possible XSRF situation
		checkPermutationStrongName();

		String[] holder = new String[1];
		Runnable[] executeCall = new Runnable[1];
		NoRemoteEndpoint<S> c = clientFactory.create(
				ts -> {
					StringSerializationStreamWriter writer = new StringSerializationStreamWriter(ts, "module base url", "policy strong name");
					writer.prepareToWrite();
					return writer;
				},
				writer -> {
					holder[0] = writer.toString();
				},
				(serializationStreamReaderConsumer, typeSerializer) -> {
					// this will be called during creation before it is time to set it up, so we stash it away for right afterward
					executeCall[0] = () -> serializationStreamReaderConsumer.accept(new StringSerializationStreamReader(typeSerializer, payload));
				}
		);
		c.setRemote(delegate);

		// execute the stashed function, causing the payload to be parsed and executed
		executeCall[0].run();

		assert holder[0] != null : "Async responses not yet supported!";
		return holder[0];
	}

	/**
	 * Standard HttpServlet method: handle the POST.
	 *
	 * This doPost method swallows ALL exceptions, logs them in the
	 * ServletContext, and returns a GENERIC_FAILURE_MSG response with status code
	 * 500.
	 *
	 * @throws ServletException
	 * @throws SerializationException
	 */
	public final void processPost(HttpServletRequest request,
								  HttpServletResponse response) throws IOException, ServletException,
			SerializationException {
		// Read the request fully.
		//
		String requestPayload = readContent(request);

		// Let subclasses see the serialized request.
		//
		onBeforeRequestDeserialized(requestPayload);

		// Invoke the core dispatching logic, which returns the serialized
		// result.
		//
		String responsePayload = processCall(requestPayload);

		// Let subclasses see the serialized response.
		//
		onAfterResponseSerialized(responsePayload);

		// Write the response.
		//
		writeResponse(request, response, responsePayload);
	}

	/**
	 * Override this method in order to control the parsing of the incoming
	 * request. For example, you may want to bypass the check of the Content-Type
	 * and character encoding headers in the request, as some proxies re-write the
	 * request headers. Note that bypassing these checks may expose the servlet to
	 * some cross-site vulnerabilities. Your implementation should comply with the
	 * HTTP/1.1 specification, which includes handling both requests which include
	 * a Content-Length header and requests utilizing <code>Transfer-Encoding:
	 * chuncked</code>.
	 *
	 * @param request the incoming request
	 * @return the content of the incoming request encoded as a string.
	 */
	protected String readContent(HttpServletRequest request)
			throws ServletException, IOException {
		if (GWT_RPC_CONTENT_TYPE != null) {
			assert (GWT_RPC_CONTENT_TYPE != null);
			String contentType = request.getContentType();
			boolean contentTypeIsOkay = false;

			if (contentType != null) {
				contentType = contentType.toLowerCase(Locale.ROOT);
				/*
				 * NOTE:We use startsWith because some servlet engines, i.e. Tomcat, do
				 * not remove the charset component but others do.
				 */
				if (contentType.startsWith(GWT_RPC_CONTENT_TYPE.toLowerCase(Locale.ROOT))) {
					contentTypeIsOkay = true;
				}
			}

			if (!contentTypeIsOkay) {
				throw new ServletException("Content-Type was '"
						+ (contentType == null ? "(null)" : contentType) + "'. Expected '"
						+ GWT_RPC_CONTENT_TYPE + "'.");
			}
		}
		if (CHARSET_UTF8_NAME != null) {
			assert (CHARSET_UTF8_NAME != null);
			boolean encodingOkay = false;
			String characterEncoding = request.getCharacterEncoding();
			if (characterEncoding != null) {
				/*
				 * TODO: It would seem that we should be able to use equalsIgnoreCase here
				 * instead of indexOf. Need to be sure that servlet engines return a
				 * properly parsed character encoding string if we decide to make this
				 * change.
				 */
				if (characterEncoding.toLowerCase(Locale.ROOT).contains(CHARSET_UTF8_NAME.toLowerCase(Locale.ROOT))) {
					encodingOkay = true;
				}
			}

			if (!encodingOkay) {
				throw new ServletException("Character Encoding is '"
						+ (characterEncoding == null ? "(null)" : characterEncoding)
						+ "'.  Expected '" + CHARSET_UTF8_NAME + "'");
			}
		}

		/*
		 * Need to support 'Transfer-Encoding: chunked', so do not rely on
		 * presence of a 'Content-Length' request header.
		 */
		InputStream in = request.getInputStream();
		byte[] buffer = new byte[BUFFER_SIZE];
		ByteArrayOutputStream out = new  ByteArrayOutputStream(BUFFER_SIZE);
		try {
			while (true) {
				int byteCount = in.read(buffer);
				if (byteCount == -1) {
					break;
				}
				out.write(buffer, 0, byteCount);
			}
			return new String(out.toByteArray(), CHARSET_UTF8);
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}

	/**
	 * Initializes the perThreadRequest and perThreadResponse fields if they are
	 * null. This will occur the first time they are accessed after an instance of
	 * this class is constructed or deserialized. This method should be called
	 * from within a 'synchronized(this) {}' block in order to ensure that only
	 * one thread creates the objects.
	 */
	private void validateThreadLocalData() {
		if (perThreadRequest == null) {
			perThreadRequest = new ThreadLocal<HttpServletRequest>();
		}
		if (perThreadResponse == null) {
			perThreadResponse = new ThreadLocal<HttpServletResponse>();
		}
	}

	private void writeResponse(HttpServletRequest request,
							   HttpServletResponse response, String responsePayload) throws IOException {
		boolean gzipEncode = acceptsGzipEncoding(request)
				&& exceedsUncompressedContentLengthLimit(responsePayload);

		writeResponse(getServletContext(), response,
				responsePayload, gzipEncode);
	}
	/**
	 * Returns <code>true</code> if the {@link HttpServletRequest} accepts Gzip
	 * encoding. This is done by checking that the accept-encoding header
	 * specifies gzip as a supported encoding.
	 *
	 * @param request the request instance to test for gzip encoding acceptance
	 * @return <code>true</code> if the {@link HttpServletRequest} accepts Gzip
	 *         encoding
	 */
	private static boolean acceptsGzipEncoding(HttpServletRequest request) {
		assert (request != null);

		String acceptEncoding = request.getHeader(ACCEPT_ENCODING);
		if (null == acceptEncoding) {
			return false;
		}

		return (acceptEncoding.indexOf(CONTENT_ENCODING_GZIP) != -1);
	}

	/**
	 * Returns <code>true</code> if the response content's estimated UTF-8 byte
	 * length exceeds 256 bytes.
	 *
	 * @param content the contents of the response
	 * @return <code>true</code> if the response content's estimated UTF-8 byte
	 *         length exceeds 256 bytes
	 */
	private static boolean exceedsUncompressedContentLengthLimit(String content) {
		return (content.length() * 2) > UNCOMPRESSED_BYTE_SIZE_LIMIT;
	}
	/**
	 * Write the response content into the {@link HttpServletResponse}. If
	 * <code>gzipResponse</code> is <code>true</code>, the response content will
	 * be gzipped prior to being written into the response.
	 *
	 * @param servletContext servlet context for this response
	 * @param response response instance
	 * @param responseContent a string containing the response content
	 * @param gzipResponse if <code>true</code> the response content will be gzip
	 *          encoded before being written into the response
	 * @throws IOException if reading, writing, or closing the response's output
	 *           stream fails
	 */
	private static void writeResponse(ServletContext servletContext,
									 HttpServletResponse response, String responseContent, boolean gzipResponse)
			throws IOException {

		byte[] responseBytes = responseContent.getBytes(CHARSET_UTF8);
		if (gzipResponse) {
			// Compress the reply and adjust headers.
			//
			ByteArrayOutputStream output = null;
			GZIPOutputStream gzipOutputStream = null;
			Throwable caught = null;
			try {
				output = new ByteArrayOutputStream(responseBytes.length);
				gzipOutputStream = new GZIPOutputStream(output);
				gzipOutputStream.write(responseBytes);
				gzipOutputStream.finish();
				gzipOutputStream.flush();
				setGzipEncodingHeader(response);
				responseBytes = output.toByteArray();
			} catch (IOException e) {
				caught = e;
			} finally {
				if (null != gzipOutputStream) {
					gzipOutputStream.close();
				}
				if (null != output) {
					output.close();
				}
			}

			if (caught != null) {
				servletContext.log("Unable to compress response", caught);
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				return;
			}
		}

		// Send the reply.
		//
		response.setContentLength(responseBytes.length);
		response.setContentType(CONTENT_TYPE_APPLICATION_JSON_UTF8);
		response.setStatus(HttpServletResponse.SC_OK);
		response.setHeader(CONTENT_DISPOSITION, ATTACHMENT);
		response.getOutputStream().write(responseBytes);
	}

	/**
	 * Sets the correct header to indicate that a response is gzipped.
	 */
	public static void setGzipEncodingHeader(HttpServletResponse response) {
		response.setHeader(CONTENT_ENCODING, CONTENT_ENCODING_GZIP);
	}


	/**
	 * Override this method to examine the serialized response that will be
	 * returned to the client. The default implementation does nothing and need
	 * not be called by subclasses.
	 *
	 * @param serializedResponse
	 */
	protected void onAfterResponseSerialized(String serializedResponse) {
	}

	/**
	 * Override this method to examine the serialized version of the request
	 * payload before it is deserialized into objects. The default implementation
	 * does nothing and need not be called by subclasses.
	 *
	 * @param serializedRequest
	 */
	protected void onBeforeRequestDeserialized(String serializedRequest) {
	}

}

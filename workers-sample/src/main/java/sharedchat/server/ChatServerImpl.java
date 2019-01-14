/*
 * #%L
 * workers-sample
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
package sharedchat.server;

import com.colinalworth.gwt.websockets.server.AbstractServerImpl;
import sharedchat.common.shared.ChatClient;
import sharedchat.common.shared.ChatClient_Impl;
import sharedchat.common.shared.ChatServer;

import javax.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/chat")
public class ChatServerImpl extends AbstractServerImpl<ChatServer, ChatClient> implements ChatServer {
	private static final Map<ChatClient, String> loggedIn = new ConcurrentHashMap<>();

	public ChatServerImpl() {
		super(ChatClient_Impl::new);
	}

	@Override
	public void onClose(Connection connection, ChatClient client) {
		String userName = loggedIn.remove(client);
		if (userName == null) {
			return;
		}

		for (ChatClient connected : loggedIn.keySet()) {
			connected.part(userName);
		}
	}

	@Override
	public void login(String username) {
		System.out.println("login: " + username);

		ChatClient c = getClient();
		for (ChatClient connected : loggedIn.keySet()) {
			connected.join(username);
		}
		loggedIn.put(c, username);
	}

	@Override
	public void say(String message) {
		System.out.println("say: " + message);
		ChatClient c = getClient();
		String userName = loggedIn.get(c);

		for (ChatClient connected : loggedIn.keySet()) {
			connected.say(userName, message);
		}
	}

	@Override
	public void onError(Throwable error) {
		error.printStackTrace();
	}

}
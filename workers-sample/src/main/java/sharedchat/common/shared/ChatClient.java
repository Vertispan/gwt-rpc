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
package sharedchat.common.shared;

import com.colinalworth.gwt.websockets.shared.Client;
import com.colinalworth.gwt.websockets.shared.Endpoint;

/**
 * Created by colin on 2/7/17.
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
}

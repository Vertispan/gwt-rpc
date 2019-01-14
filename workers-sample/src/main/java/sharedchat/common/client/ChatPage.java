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
package sharedchat.common.client;

import com.colinalworth.gwt.websockets.shared.Endpoint;
import com.colinalworth.gwt.worker.client.MessagePortEndpoint;
import sharedchat.common.shared.ChatEvent;

import java.util.List;

/**
 * Created by colin on 2/6/17.
 */
@Endpoint
public interface ChatPage extends MessagePortEndpoint<ChatWorker> {
	void init(String username, List<ChatEvent> events);

	void connected();

	void disconnected();

	void join(String username);

	void leave(String username);

	void say(String username, String text);
}

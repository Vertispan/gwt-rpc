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

import sharedchat.common.client.ChatPage;

/**
 * Created by colin on 2/7/17.
 */
public class ChatMessage implements ChatEvent {
	private String username;
	private String text;

	public ChatMessage() {
	}

	public ChatMessage(String username, String text) {
		this.username = username;
		this.text = text;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	@Override
	public void handle(ChatPage chatPage) {
		chatPage.say(getUsername(), getText());
	}
}

/*
 * #%L
 * javaee-websocket-gwt-rpc-sample
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
package samples.easychatroom2.client;

import com.colinalworth.gwt.websockets.client.AbstractClientImpl;
import com.colinalworth.gwt.websockets.shared.Callback;
import samples.easychatroom2.shared.ChatClient;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import samples.easychatroom2.shared.ChatServer;

public class ChatClientWidget extends AbstractClientImpl<ChatClient, ChatServer> implements ChatClient, IsWidget {
	FlowPanel panel = new FlowPanel();
	TextBox message = new TextBox();
	DockLayoutPanel root;
	Button send = new Button("Send");

	public ChatClientWidget() {
		root = new DockLayoutPanel(Unit.PX);

		HorizontalPanel input = new HorizontalPanel();
		message.setWidth("200px");
		input.add(message);
		input.add(send);

		root.addSouth(input, 30);
		ScrollPanel scroll = new ScrollPanel();
		scroll.add(panel);
		root.add(scroll);
	}

	@Override
	public Widget asWidget() {
		return root;
	}

	@Override
	public void say(String username, String message) {
		addMessage(username + ": " + message);
	}

	@Override
	public void join(String username) {
		addMessage(username + " has joined");
	}

	@Override
	public void part(String username) {
		addMessage(username + " has left");
	}

	@Override
	public void ping(Callback<Void, Void> callback) {
		callback.onSuccess(null);
	}

	@Override
	public void onOpen() {
		super.onOpen();
		addMessage("You've joined the chat");
	}

	@Override
	public void onClose() {
		super.onClose();
		addMessage("You've left the chat");
	}

	@Override
	public void onError(Throwable error) {
		Window.alert(error.getMessage());
	}

	protected void addMessage(String message) {
		Label m = new Label(message);
		panel.add(m);
		m.getElement().scrollIntoView();
	}

}

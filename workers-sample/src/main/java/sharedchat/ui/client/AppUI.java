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
package sharedchat.ui.client;

import com.colinalworth.gwt.worker.client.WorkerFactory;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import sharedchat.common.client.ChatPage;
import sharedchat.common.client.ChatWorker;
import sharedchat.common.client.ChatWorker_Impl;
import sharedchat.common.shared.ChatEvent;
import sharedchat.common.shared.ChatJoin;
import sharedchat.common.shared.ChatLeave;
import sharedchat.common.shared.ChatMessage;

import java.util.List;

/**
 * Created by colin on 2/6/17.
 */
public class AppUI implements EntryPoint {

	private PopupPanel popup;

	@Override
	public void onModuleLoad() {
		WorkerFactory<ChatWorker, ChatPage> sharedWorkerFactory = WorkerFactory.of(ChatWorker_Impl::new);

		ChatClientWidget widget = new ChatClientWidget();

		ChatWorker sharedWorker = sharedWorkerFactory.createSharedWorker("sharedchat_worker/worker.js", new ChatPage() {
			private ChatWorker chatWorker;

			@Override
			public void init(String username, List<ChatEvent> events) {
				widget.clear();
				if (username == null) {
					//hide chat
					RootLayoutPanel.get().clear();
					//show login screen
					popup.show();
				} else {
					popup.hide();
					RootLayoutPanel.get().add(widget);

					infoPopup("Logged in as " + username);
					for (ChatEvent event : events) {
						if (event instanceof ChatJoin) {
							widget.join(((ChatJoin) event).getUsername());
						} else if (event instanceof ChatLeave) {
							widget.leave(((ChatLeave) event).getUsername());
						} else if (event instanceof ChatMessage) {
							widget.say(((ChatMessage) event).getUsername(), ((ChatMessage) event).getText());
						}
					}
				}
			}

			@Override
			public void join(String username) {
				widget.join(username);
			}

			@Override
			public void leave(String username) {
				widget.leave(username);
			}

			@Override
			public void say(String username, String text) {
				widget.say(username, text);
			}

			@Override
			public void setRemote(ChatWorker chatWorker) {
				this.chatWorker = chatWorker;
			}

			@Override
			public ChatWorker getRemote() {
				return chatWorker;
			}

			@Override
			public void connected() {
				unmask();
				infoPopup("Reconnected");
			}

			@Override
			public void disconnected() {
				mask("Connection lost, reconnecting...");
			}
		});

		widget.send.addClickHandler(e -> {
			String message = widget.message.getValue();
			if (message != null && message.trim().length() > 0) {
				sharedWorker.send(message);
			}
		});

		Button logout = new Button("Logout", (ClickHandler) e -> {
			sharedWorker.logout();
		});
		logout.getElement().getStyle().setPosition(Position.ABSOLUTE);
		logout.getElement().getStyle().setRight(0, Unit.PX);
		RootPanel.get().add(logout);

		popup = new PopupPanel();
		popup.setGlassEnabled(true);
		FlowPanel w = new FlowPanel();
		w.add(new Label("Pick a username:"));
		TextBox username = new TextBox();
		w.add(username);
		w.add(new Button("Login", (ClickHandler) e -> sharedWorker.login(username.getValue())));
		popup.setWidget(w);

		mask("Connecting...");
	}

	public void mask(String message) {

	}
	public void unmask() {

	}
	public void infoPopup(String message) {

	}
}

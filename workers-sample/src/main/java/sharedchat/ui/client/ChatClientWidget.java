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

import com.google.gwt.core.client.Callback;
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

/**
 * Created by colin on 2/7/17.
 */
public class ChatClientWidget implements IsWidget {
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

	public void say(String username, String message) {
		addMessage(username + ": " + message);
	}

	public void join(String username) {
		addMessage(username + " has joined");
	}

	public void leave(String username) {
		addMessage(username + " has left");
	}

	protected void addMessage(String message) {
		Label m = new Label(message);
		panel.add(m);
		m.getElement().scrollIntoView();
	}

	public void clear() {
		panel.clear();
	}
}

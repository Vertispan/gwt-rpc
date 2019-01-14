/*
 * #%L
 * gwt-websockets-api
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
package com.colinalworth.gwt.websockets.shared;

import java.util.function.Consumer;

/**
 * Simple callback interface to allow responses from remote messages.
 * @param <T>
 * @param <F>
 */
public interface Callback<T, F> {
	static <T> Callback<T, ?> of(Consumer<T> success) {
		return of(success, ignore -> {/*TODO stop ignoring and report somehow*/});
	}
	static <T, F> Callback<T, F> of(Consumer<T> success, Consumer<F> failure) {
		return new Callback<T, F>() {
			@Override
			public void onSuccess(T value) {
				success.accept(value);
			}

			@Override
			public void onFailure(F error) {
				failure.accept(error);
			}
		};
	}
	void onSuccess(T value);
	void onFailure(F error);
}

/*
 * Copyright 2014-2018 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jkoolcloud.tnt4j.samples;

import java.nio.file.FileVisitor;
import java.nio.file.WatchEvent;

import com.jkoolcloud.tnt4j.sink.EventSink;

/**
 * This interface provides a way to implement folder/file changes
 *
 * @version $Revision: 1$
 */
public interface WatchEventHandler<K> extends FileVisitor<K> {
	void handleEvent(WatchEvent<K> event, K root);

	EventSink getEventSink();
}

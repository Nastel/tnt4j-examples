/*
 * Copyright 2014-2015 JKOOL, LLC.
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
package com.nastel.jkool.tnt4j.samples;

import java.io.File;
import java.nio.file.Path;

import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.core.PropertySnapshot;
import com.nastel.jkool.tnt4j.core.Snapshot;
import com.nastel.jkool.tnt4j.core.ValueTypes;
import com.nastel.jkool.tnt4j.sink.EventSink;
import com.nastel.jkool.tnt4j.sink.SinkEventFilter;
import com.nastel.jkool.tnt4j.source.Source;
import com.nastel.jkool.tnt4j.tracker.TrackingActivity;
import com.nastel.jkool.tnt4j.tracker.TrackingEvent;

public class PathEventFilter implements SinkEventFilter {
	FolderEventHandler evHandler;

	PathEventFilter(FolderEventHandler evHandler) {
		this.evHandler = evHandler;
	}

	@Override
	public boolean filter(EventSink sink, TrackingEvent event) {
		Object[] args = event.getMessageArgs();
		for (int i = 0; args != null && i < args.length; i++) {
			if (args[i] instanceof Path) {
				Path path = (Path) args[i];
				File file = path.toFile();
				String resource = path.toUri().toString();
				event.getOperation().setResource(resource);
				boolean exists = file.exists();
				if (exists) {
					PropertySnapshot snap = new PropertySnapshot("FileSystem", file.getPath());
					snap.add("Exists", file.exists(), ValueTypes.VALUE_TYPE_FLAG);
					snap.add("CanRead", file.canRead(), ValueTypes.VALUE_TYPE_FLAG);
					snap.add("CanWrite", file.canWrite(), ValueTypes.VALUE_TYPE_FLAG);
					snap.add("CanExecute", file.canExecute(), ValueTypes.VALUE_TYPE_FLAG);
					snap.add("FileSize", file.length(), ValueTypes.VALUE_TYPE_SIZE_BYTE);
					snap.add("FreeSpace", file.getFreeSpace(), ValueTypes.VALUE_TYPE_SIZE_BYTE);
					snap.add("TotalSpace", file.getTotalSpace(), ValueTypes.VALUE_TYPE_SIZE_BYTE);
					snap.add("UsableSpace", file.getUsableSpace(), ValueTypes.VALUE_TYPE_SIZE_BYTE);
					snap.add("LastModified", file.lastModified(), ValueTypes.VALUE_TYPE_AGE_MSEC);
					event.getOperation().addSnapshot(snap);
					evHandler.trackPropertyChanges(file, event);
				}
			}
		}
		return true;
	}

	@Override
	public boolean filter(EventSink sink, TrackingActivity activity) {
		return true;
	}

	@Override
	public boolean filter(EventSink sink, Snapshot snapshot) {
		return true;
	}

	@Override
	public boolean filter(EventSink sink, long ttl, Source source, OpLevel level, String msg, Object... args) {
		return true;
	}
}
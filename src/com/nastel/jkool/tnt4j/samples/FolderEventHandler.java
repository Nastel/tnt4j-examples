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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.nastel.jkool.tnt4j.TrackingLogger;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.core.OpType;
import com.nastel.jkool.tnt4j.core.PropertySnapshot;
import com.nastel.jkool.tnt4j.sink.EventSink;
import com.nastel.jkool.tnt4j.tracker.TimeTracker;
import com.nastel.jkool.tnt4j.tracker.TrackingEvent;
import com.nastel.jkool.tnt4j.utils.Utils;

/**
 * Simple class to handle directory events and track file changes.
 * This class also tracks changes within properties and configuration files.
 *
 * @version $Revision: 1$
 */
class FolderEventHandler extends SimpleFileVisitor<Path> implements WatchEventHandler<Path> {
	private static final String CONTENTS_CHANGED = "ContentsChanged";
	private static final String CONTENTS_ADDED = "ContentsAdded";
	private static final String CONTENTS_REMOVED = "ContentsRemoved";
	
	private static final String PATH_CHANGED = "PathModified";
	private static final String PATH_ADDED = "PathCreated";
	private static final String PATH_REMOVED = "PathDeleted";
	
	TrackingLogger logger;
	String extListString;
	String [] extList;
	TimeTracker timeTracker;
	Map<String, Properties> PROP_TABLE = new HashMap<String, Properties>();

	public FolderEventHandler(String name, String exts) throws IOException {
		this.extListString = exts;
		this.extList = exts.split(";");
		this.timeTracker = TimeTracker.newTracker(1000, TimeUnit.DAYS.toMillis(30));
		logger = TrackingLogger.getInstance(name);
		logger.addSinkEventFilter(new PathEventFilter(this));
		logger.open();
	}

	private boolean isPropertyFile(File file) {
		boolean flag = false;
		if (!file.isFile()) return flag;
		
		for (int i=0; i < extList.length; i++) {
			flag = file.getName().endsWith(extList[i]);
			if (flag) break;
		}
		return flag;
	}
	
	@Override
	public EventSink getEventSink() {
		return logger.getEventSink();
	}
	
	@Override
	public void handleEvent(WatchEvent<Path> event, Path root) {
		Kind<Path> kind = event.kind();
		Path child = root.resolve(event.context());
		String resource = child.toUri().toString();
		if (kind.equals(StandardWatchEventKinds.ENTRY_CREATE)) {
			logger.tnt(OpLevel.INFO, OpType.ADD, PATH_ADDED, null, resource,
					TimeUnit.NANOSECONDS.toMicros(timeTracker.hitAndGet(resource)),
					"Path created: {0}", child);
		} else if (kind.equals(StandardWatchEventKinds.ENTRY_DELETE)) {
			logger.tnt(OpLevel.WARNING, OpType.REMOVE, PATH_REMOVED, null, resource,
					TimeUnit.NANOSECONDS.toMicros(timeTracker.hitAndGet(resource)),
					"Path deleted: {0}", child);
		} else if (kind.equals(StandardWatchEventKinds.ENTRY_MODIFY)) {
			logger.tnt(OpLevel.INFO, OpType.UPDATE, PATH_CHANGED, null, resource, 
					TimeUnit.NANOSECONDS.toMicros(timeTracker.hitAndGet(resource)),
					"Path changed: {0}", child);
		}
	}

	protected void trackPropertyChanges(File file, TrackingEvent event) {
		if (isPropertyFile(file)) {
			Properties before = PROP_TABLE.get(file.getPath());
			try {
				Properties after = loadPropFile(file);
				if (before != null && after != null) {
					compareProperties(file.getPath(), before, after, event);
				}
				PROP_TABLE.put(file.getPath(), after);
			} catch (IOException e) {
				logger.error("Cant read: file={0}", file.toPath(), e);									
			}
		}
	}

	protected Properties loadPropFile(File file) throws IOException {
		if (isPropertyFile(file)) {
			Properties prop = new Properties();
			InputStream in = new FileInputStream(file);
			try {
				prop.load(in);
				return prop;
			} finally {
				Utils.close(in);
			}
		}
		return null;
	}

	private TrackingEvent compareProperties(String fileName, Properties before, Properties after, TrackingEvent event) {
		PropertySnapshot changes = new PropertySnapshot(CONTENTS_CHANGED, fileName);
		PropertySnapshot added = new PropertySnapshot(CONTENTS_ADDED, fileName);
		PropertySnapshot removed = new PropertySnapshot(CONTENTS_REMOVED, fileName);

		HashSet<Object> all = new HashSet<Object>();
		all.addAll(before.keySet());
		all.addAll(after.keySet());

		for (Object key : all) {
			String beforeValue = before.getProperty(key.toString());
			String afterValue = after.getProperty(key.toString());
			if (beforeValue != null && afterValue != null) {
				if (!Utils.equal(beforeValue, afterValue)) {
					changes.add(key, beforeValue + "=>" + afterValue);
				}
			} else if (beforeValue == null && afterValue != null) {
				added.add(key, after.getProperty(key.toString()));
			} else if (beforeValue != null && afterValue == null) {
				removed.add(key, before.getProperty(key.toString()));
			}
		}

		if (changes.size() > 0) {
			event.getOperation().addSnapshot(changes);
		}
		if (added.size() > 0) {
			event.getOperation().addSnapshot(added);
		}
		if (removed.size() > 0) {
			event.getOperation().addSnapshot(removed);
		}
		return event;
	}

	@Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		try {
			Properties prop = loadPropFile(file.toFile());
			if (prop != null) {
				logger.debug("Loaded properties: file={0}, type={1}, prop.count={2}", file,
				        Files.probeContentType(file), prop.size());
				PROP_TABLE.put(file.toFile().getPath(), prop);
			}
		} catch (Throwable e) {
			logger.error("Cant read: file={0}", file, e);
		}
		return FileVisitResult.CONTINUE;
    }
}
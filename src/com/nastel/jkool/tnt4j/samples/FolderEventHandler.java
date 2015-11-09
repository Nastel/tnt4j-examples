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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;

import com.nastel.jkool.tnt4j.TrackingLogger;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.core.OpType;
import com.nastel.jkool.tnt4j.core.PropertySnapshot;
import com.nastel.jkool.tnt4j.sink.EventSink;
import com.nastel.jkool.tnt4j.tracker.TrackingEvent;

// Simple class to handle directory events.
class FolderEventHandler implements WatchEventHandler<Path> {
	private static final String CONTENTS_CHANGED = "ContentsChanged";
	private static final String CONTENTS_ADDED = "ContentsAdded";
	private static final String CONTENTS_REMOVED = "ContentsRemoved";
	
	private static final String PATH_CHANGED = "PathModified";
	private static final String PATH_ADDED = "PathCreated";
	private static final String PATH_REMOVED = "PathDeleted";
	
	private Path folder;
	TrackingLogger logger;
	String extListString;
	String [] extList;
	Map<String, Properties> PROP_TABLE = new HashMap<String, Properties>();

	public FolderEventHandler(String name, String exts, Path path, boolean recursive, boolean verbose) throws IOException {
		this.folder = path;
		this.extListString = exts;
		this.extList = exts.split(";");
		logger = TrackingLogger.getInstance(name);
		logger.addSinkEventFilter(new PathEventFilter(this));
		logger.open();
		loadPropFiles(folder, recursive, verbose, PROP_TABLE);
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
		if (kind.equals(StandardWatchEventKinds.ENTRY_CREATE)) {
			logger.tnt(OpLevel.INFO, OpType.ADD, PATH_ADDED, null, child.toUri().toString(), 0, "Path created: {0}", child);
		} else if (kind.equals(StandardWatchEventKinds.ENTRY_DELETE)) {
			logger.tnt(OpLevel.WARNING, OpType.REMOVE, PATH_REMOVED, null, child.toUri().toString(), 0, "Path deleted: {0}", child);
		} else if (kind.equals(StandardWatchEventKinds.ENTRY_MODIFY)) {
			logger.tnt(OpLevel.INFO, OpType.UPDATE, PATH_CHANGED, null, child.toUri().toString(), 0, "Path changed: {0}", child);
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
				logger.error("Cant read: file={0}", file.getPath(), e);									
			}
		}
	}

	protected void loadPropFiles(Path root, boolean recursive, boolean verbose, Map<String, Properties> map) {
		File dir = root.toFile();
		long count = 0;
		if (dir.isDirectory()) {
			long start = System.currentTimeMillis();
			if (verbose) {
				System.out.format("Scanning for '%s' in '%s'\n", extListString, dir);
			}
			File[] files = dir.listFiles();
			for (int i = 0; i < files.length; i++) {
				File file = files[i];
				if (recursive && file.isDirectory()) {
					loadPropFiles(file.toPath(), recursive, verbose, map);
				} else {
					try {
						Properties prop = loadPropFile(file);
						if (prop != null) {
							logger.debug("Loaded properties: file={0}, type={1}, prop.count={2}", file.getPath(),
							        Files.probeContentType(file.toPath()), prop.size());
							count++;
							map.put(file.getPath(), prop);
						}
					} catch (Throwable e) {
						logger.error("Cant read: file={0}", file.getPath(), e);
					}
				}
			}
			if (verbose) {
				System.out.format("Scanning done, '%s' in '%s', found %d files, elapsed.ms=%d\n", 
					extListString, dir, count, (System.currentTimeMillis() - start));
			}
		}
	}

	protected Properties loadPropFile(File file) throws IOException {
		if (isPropertyFile(file)) {
			Properties prop = new Properties();
			InputStream in = new FileInputStream(file);
			prop.load(in);
			in.close();
			return prop;
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
				if (!equal(beforeValue, afterValue)) {
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

	public static boolean equal(final Object obj1, final Object obj2) {
		return obj1 == obj2 || (obj1 != null && obj1.equals(obj2));
	}
}
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import com.nastel.jkool.tnt4j.TrackingLogger;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.core.OpType;
import com.nastel.jkool.tnt4j.core.PropertySnapshot;
import com.nastel.jkool.tnt4j.core.Snapshot;
import com.nastel.jkool.tnt4j.core.ValueTypes;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;
import com.nastel.jkool.tnt4j.sink.SinkEventFilter;
import com.nastel.jkool.tnt4j.source.Source;
import com.nastel.jkool.tnt4j.tracker.TrackingActivity;
import com.nastel.jkool.tnt4j.tracker.TrackingEvent;

/**
 * Simple application that monitors changes to file system folders.
 * 
 * @version $Revision: 1 $
 */
public class FolderMonitor {
	private static final String PROP_FILE_EXT = System.getProperty("tnt4j.folder.monitor.property.ext", ".properties;.conf");
	
	private static final EventSink logger = DefaultEventSinkFactory.defaultEventSink(FolderMonitor.class);

	public static void main(String[] args) throws InterruptedException, IOException {
		if (args.length < 1) {
			System.out.println("Usage: folder-list");
			System.exit(-1);
		}
		try {
			for (int i = 0; i < args.length; i++) {
				Path pathToWatch = FileSystems.getDefault().getPath(args[i]);
				FolderWatcher monitor = new FolderWatcher(FolderMonitor.class.getName() + "." + pathToWatch.toFile().getName(), PROP_FILE_EXT, pathToWatch);
				Thread monitorThread = new Thread(monitor);
				monitorThread.start();
			}
		} catch (Throwable ex) {
			logger.log(OpLevel.ERROR, "Unable to watch: {0}", args[0], ex);
		}
	}
}

class PathEventFilter implements SinkEventFilter {

	FolderWatcher watcher;

	PathEventFilter(FolderWatcher fw) {
		watcher = fw;
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
					try {
						watcher.trackPropertyChanges(file, event);
					} catch (IOException e) {
					}
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

// Simple class to watch directory events.
class FolderWatcher implements Runnable {
	private Path folder;
	TrackingLogger logger;
	String [] extList;
	ConcurrentHashMap<String, Properties> PROP_TABLE = new ConcurrentHashMap<String, Properties>();

	public FolderWatcher(String name, String exts, Path path) throws IOException {
		this.folder = path;
		extList = exts.split(";");
		logger = TrackingLogger.getInstance(name);
		logger.addSinkEventFilter(new PathEventFilter(this));
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
	
	private void handleEvent(WatchEvent<?> event) throws IOException {
		Kind<?> kind = event.kind();
		if (kind.equals(StandardWatchEventKinds.ENTRY_CREATE)) {
			Path path = folder.resolve((Path) event.context());
			logger.tnt(OpLevel.INFO, OpType.ADD, "PathCreated", null, path.toUri().toString(), 0, "Path created: {0}", path);
		} else if (kind.equals(StandardWatchEventKinds.ENTRY_DELETE)) {
			Path path = folder.resolve((Path) event.context());
			logger.tnt(OpLevel.WARNING, OpType.REMOVE, "PathDeleted", null, path.toUri().toString(), 0, "Path deleted: {0}", path);
		} else if (kind.equals(StandardWatchEventKinds.ENTRY_MODIFY)) {
			Path path = folder.resolve((Path) event.context());
			logger.tnt(OpLevel.DEBUG, OpType.UPDATE, "PathModified", null, path.toUri().toString(), 0, "Path changed: {0}", path);
		}
	}

	protected void trackPropertyChanges(File file, TrackingEvent event) throws IOException {
		if (isPropertyFile(file)) {
			Properties before = PROP_TABLE.get(file.getPath());
			Properties after = loadPropFile(file);
			if (before != null && after != null) {
				compareProperties(file.getPath(), before, after, event);
			}
			PROP_TABLE.put(file.getPath(), after);
		}
	}

	@Override
	public void run() {
		try {
			logger.open();
			loadPropFiles();
			WatchService watchService = folder.getFileSystem().newWatchService();
			folder.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY,
			        StandardWatchEventKinds.ENTRY_DELETE);
			logger.info("Start watching: {0}", folder);
			while (true) {
				WatchKey key = watchService.take();

				for (final WatchEvent<?> event : key.pollEvents()) {
					handleEvent(event);
				}

				boolean valid = key.reset();
				if (!valid) {
					key.cancel();
					watchService.close();
					break;
				}
			}
		} catch (InterruptedException ex) {
			logger.warn("Unable to watch: {0}", folder, ex);
			return;
		} catch (Throwable ex) {
			logger.error("Unable to watch: {0}", folder, ex);
			return;
		} finally {
			logger.info("Stopped watching: {0}", folder);
			logger.close();
		}
	}

	private void loadPropFiles() throws FileNotFoundException, IOException {
		File dir = folder.toFile();
		if (dir.isDirectory()) {
			File[] files = dir.listFiles();
			for (int i = 0; i < files.length; i++) {
				File file = files[i];
				Properties prop = loadPropFile(file);
				if (prop != null) {
					logger.info("Loaded properties: file={0}, type={1}, prop.count={2}", file.getPath(), Files.probeContentType(file.toPath()), prop.size());
					PROP_TABLE.put(file.getPath(), prop);
				}
			}
		}
	}

	private Properties loadPropFile(File file) throws IOException {
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
		PropertySnapshot changes = new PropertySnapshot("ContentsChanged", fileName);
		PropertySnapshot added = new PropertySnapshot("ContentsAdded", fileName);
		PropertySnapshot removed = new PropertySnapshot("ContentsRemoved", fileName);

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
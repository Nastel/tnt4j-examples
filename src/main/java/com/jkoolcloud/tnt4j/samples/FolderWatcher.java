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

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

/**
 * This class monitors for changes in folders, files and triggers a specified handler {@link WatchEventHandler}.
 *
 * @see WatchEventHandler
 * 
 * @version $Revision: 1$
 */
public class FolderWatcher implements Runnable {

	private final Map<WatchKey, Path> watchMap;
	private final WatchEventHandler<Path> handler;
	private final Path folder;
	private WatchService watcher;
	private final boolean recursive;
	private boolean verbose = false;

	FolderWatcher(Path folder, boolean recursive, WatchEventHandler<Path> handler) {
		this.folder = folder;
		this.recursive = recursive;
		this.handler = handler;
		this.watchMap = new HashMap<WatchKey, Path>();
	}

	public FolderWatcher load() throws IOException {
		this.watcher = FileSystems.getDefault().newWatchService();
		if (recursive) {
			long begin = System.currentTimeMillis();
			System.out.format("Scanning path %s ...\n", folder);
			watchAll(folder);
			System.out.format("Scanning done, path.count=%d, elapsed.ms=%d\n", watchMap.size(),
					(System.currentTimeMillis() - begin));
		} else {
			watch(folder);
		}
		return this;
	}

	public FolderWatcher setVerbose(boolean flag) {
		verbose = flag;
		return this;
	}

	@SuppressWarnings("unchecked")
	public static <T> WatchEvent<T> cast(WatchEvent<?> event) {
		return (WatchEvent<T>) event;
	}

	private void watch(Path folder) throws IOException {
		WatchKey key = folder.register(watcher, StandardWatchEventKinds.ENTRY_CREATE,
				StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
		Path prev = watchMap.get(key);
		if (verbose) {
			if (prev == null) {
				System.out.format("watch path: %s\n", folder);
			} else {
				if (!folder.equals(prev)) {
					System.out.format("update path: %s -> %s\n", prev, folder);
				}
			}
		}
		watchMap.put(key, folder);
	}

	private void watchAll(Path start) throws IOException {
		Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				handler.visitFile(file, attrs);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult preVisitDirectory(Path folder, BasicFileAttributes attrs) throws IOException {
				watch(folder);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	void preProcessEvent(WatchEvent<Path> event, Path dir) {
		Path child = dir.resolve(event.context());

		if (verbose) {
			System.out.format("%s: %s\n", event.kind().name(), child);
		}
		if (recursive && (event.kind() == StandardWatchEventKinds.ENTRY_CREATE)) {
			try {
				if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
					watchAll(child);
				}
			} catch (IOException x) {
			}
		}
	}

	void go() throws InterruptedException {
		for (;;) {
			WatchKey key = watcher.take();
			Path folder = watchMap.get(key);
			if (folder == null) {
				continue;
			}

			for (WatchEvent<?> event : key.pollEvents()) {
				Kind<?> kind = event.kind();

				if (kind == StandardWatchEventKinds.OVERFLOW) {
					continue;
				}
				WatchEvent<Path> ev = cast(event);
				preProcessEvent(ev, folder);
				handler.handleEvent(ev, folder);
			}

			boolean valid = key.reset();
			if (!valid) {
				watchMap.remove(key);
				if (watchMap.isEmpty()) {
					break;
				}
			}
		}
	}

	@Override
	public void run() {
		try {
			go();
		} catch (InterruptedException e) {
		} finally {
			System.out.println("Watcher for " + folder + " stopped");
		}
	}
}

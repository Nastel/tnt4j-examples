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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * Simple application that monitors changes to file system folders.
 * 
 * @version $Revision: 1 $
 */
public class FolderMonitor {
	private static final String PROP_FILE_EXT = System.getProperty("tnt4j.folder.property.file.ext", ".properties;.conf");	
	private static final EventSink logger = DefaultEventSinkFactory.defaultEventSink(FolderMonitor.class);

	public static void main(String[] args) throws InterruptedException, IOException {
		if (args.length < 1) {
			System.out.println("Usage: [+r|-r] [+v|-v] folder-watch-list");
			System.exit(-1);
		}
		boolean recursive = false, verbose = false;
		try {
			for (int i = 0; i < args.length; i++) {
				if (args[i].equalsIgnoreCase("+r")) {
					recursive = true;
					continue;
				} else if (args[i].equalsIgnoreCase("-r")) {
					recursive = false;
					continue;
				} else if (args[i].equalsIgnoreCase("+v")) {
					verbose = true;
					continue;
				} else if (args[i].equalsIgnoreCase("-v")) {
					verbose = false;
					continue;
				}
				Path pathToWatch = Paths.get(args[i]);				
				System.out.println("Watch path: " + pathToWatch + ", recursive=" + recursive + ", verbose=" + verbose);
				FolderEventHandler evHandler = new FolderEventHandler(FolderMonitor.class.getName() + "." + pathToWatch.toFile().getName(),
						PROP_FILE_EXT, pathToWatch, recursive, verbose);
				FolderWatcher folderWatcher = new FolderWatcher(pathToWatch, recursive, evHandler);
				Thread monitorThread = new Thread(folderWatcher.setVerbose(verbose).load());
				monitorThread.start();
			}
			System.out.println("Done & Ready :)");
		} catch (Throwable ex) {
			logger.log(OpLevel.ERROR, "Unable to watch: {0}", args[0], ex);
		}
	}
}
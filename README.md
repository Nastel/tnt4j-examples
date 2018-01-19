# tnt4j-examples
TNT4J Samples

Running Samples
===============================================
* Directory Monitor (`com.jkoolcloud.tnt4j.samples.FolderMonitor`). Monitors a given directory for added, modified, deleted files.
```java	
java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug -Dtnt4j.config=config/tnt4j.properties -Dtnt4j.dump.on.vm.shutdown=true -Dtnt4j.dump.provider.default=true -classpath tnt4j-samples*.jar;lib/* com.jkoolcloud.tnt4j.samples.FolderMonitor /temp
```
* Sample Pinger (`com.jkoolcloud.tnt4j.samples.Pinger`). Generate activity ping at a specified interval.
```java	
java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug -Dtnt4j.config=config/tnt4j.properties -Dtnt4j.dump.on.vm.shutdown=true -Dtnt4j.dump.provider.default=true -classpath tnt4j-samples*.jar;lib/* com.jkoolcloud.tnt4j.samples.Pinger com.nastel.pinger pingActivity 1000
```

**Command line arguments:**
* `-Dorg.slf4j.simpleLogger.defaultLogLevel=debug` -- default logging level for SLF4J simple logger binding.
* `-Dtnt4j.dump.on.vm.shutdown=true` java property allows application state dumps generated automatically upon VM shutdown.
* `-Dtnt4j.dump.provider.default=true` java property registers all default dump providers (memory, stack, logging stats).
* `-Dtnt4j.formatter.json.newline=true` java property directs `JSONFormatter` to append new line when formatting log entries.

See `<timestamp>.log` and `<vmid>.dump` files for output produced by `com.jkoolcloud.tnt4j.samples.FolderMonitor`.
See `config/tnt4j.properties` for TNT4J configuration: factories, formatters, listeners, etc.

# Project Dependencies
* JDK 1.7+
* TNT4J (https://github.com/Nastel/TNT4J)

These examples require TNT4J. You will therefore need to point TNT4J to it's property file via the -Dtnt4j.config argument. This property file is located here in GitHub under the /config directory. If using JCenter or Maven, it can be found in the zip assembly along with the source code and javadoc.

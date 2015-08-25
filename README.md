# tnt4j-examples
TNT4J Samples

Running Samples
===============================================
* Directory Monitor (`com.nastel.jkool.tnt4j.samples.FolderMonitor`). Monitors a given directory for added, modified, deleted files.
```java	
java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug -Dtnt4j.config=config/tnt4j.properties -Dtnt4j.dump.on.vm.shutdown=true -Dtnt4j.dump.provider.default=true -classpath tnt4j-api-final-all.jar com.nastel.jkool.tnt4j.samples.FolderMonitor /temp
```
* JMX Pinger (`com.nastel.jkool.tnt4j.samples.Pinger`). Monitors a given directory for added, modified, deleted files.
```java	
java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug -Dtnt4j.config=config/tnt4j.properties -Dtnt4j.dump.on.vm.shutdown=true -Dtnt4j.dump.provider.default=true -classpath tnt4j-api-final-all.jar com.nastel.jkool.tnt4j.samples.Pinger
```

<b>Command line arguments:</b>
* `-Dorg.slf4j.simpleLogger.defaultLogLevel=debug` -- default logging level for SLF4J simple logger binding.
* `-Dtnt4j.dump.on.vm.shutdown=true` java property allows application state dumps generated automatically upon VM shutdown.
* `-Dtnt4j.dump.provider.default=true` java property registers all default dump providers (memory, stack, logging stats).
* `-Dtnt4j.formatter.json.newline=true` java property directs `JSONFormatter` to append new line when formatting log entries.

See `<timestamp>.log` and `<vmid>.dump` files for output produced by `com.nastel.jkool.tnt4j.samples.FolderMonitor`.
See `config/tnt4j.properties` for TNT4J configuration: factories, formatters, listeners, etc.

# Project Dependencies
* JDK 1.7+
* TNT4J (https://github.com/Nastel/TNT4J)

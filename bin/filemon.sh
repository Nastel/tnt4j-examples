#! /bin/bash
RUNDIR=`pwd`

CLPATH="$RUNDIR/../tnt4j-samples.jar:$RUNDIR/../lib/*"
MAINCL=com.nastel.jkool.tnt4j.samples.FolderMonitor
TNT4JOPTS="-Dorg.slf4j.simpleLogger.defaultLogLevel=debug -Dtnt4j.dump.on.vm.shutdown=true -Dtnt4j.dump.provider.default=true -Dtnt4j.config=$RUNDIR/../config/tnt4j.properties"
java $TNT4JOPTS -classpath $CLPATH $MAINCL $@

@echo off
setlocal
set RUNDIR=%~p0

set CLPATH=%RUNDIR%../tnt4j-samples.jar;%RUNDIR%../lib/*
set MAINCL=com.nastel.jkool.tnt4j.samples.FolderMonitor
set TNT4JOPTS=-Dorg.slf4j.simpleLogger.defaultLogLevel=info -Dtnt4j.dump.on.vm.shutdown=true -Dtnt4j.dump.provider.default=true -Dtnt4j.config=%RUNDIR%../config/tnt4j.properties
java %TNT4JOPTS% -classpath %CLPATH% %MAINCL% %*
endlocal
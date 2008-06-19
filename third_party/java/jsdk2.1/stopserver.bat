@echo off
rem $Id: shutdown.bat,v 1.2 1999/04/09 19:50:32 duncan Exp $
rem Startup batch file for servlet runner.

rem This batch file written and tested under Windows NT
rem Improvements to this file are welcome

if "%CLASSPATH%" == "" goto noclasspath

rem else
set _CLASSPATH=%CLASSPATH%
set CLASSPATH=server.jar;servlet.jar;classes;%CLASSPATH%
goto next

:noclasspath
set _CLASSPATH=
set CLASSPATH=server.jar;servlet.jar;classes
goto next

:next
rem echo Using Classpath: %CLASSPATH%
java com.sun.web.shell.Shutdown %1 %2 %3 %4 %5 %6 %7 %8 %9

rem clean up classpath after
set CLASSPATH=%_CLASSPATH%
set _CLASSPATH=


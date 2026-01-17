@echo off
cd /d "C:\Users\oaSIS\Desktop\bistro server"

rem --- JavaFX SDK home ---
set FX_HOME=C:\Users\oaSIS\Desktop\bistro server\javafx-sdk-25.0.1

rem --- Make sure native DLLs can be found ---
set PATH=%FX_HOME%\bin;%PATH%

rem --- Run server ---
java ^
  --enable-native-access=javafx.graphics ^
  --module-path "%FX_HOME%\lib" ^
  --add-modules javafx.controls,javafx.fxml,javafx.graphics ^
  -cp "G13_server.jar;G13_server_lib\*" ^
  server.ServerMain

pause

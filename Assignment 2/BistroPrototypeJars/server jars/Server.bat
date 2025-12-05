@echo off
cd /d "C:\Users\oaSIS\Desktop\BistroJars\server jars"

rem --- JavaFX SDK home ---
set FX_HOME=C:\Users\oaSIS\Desktop\javafx-sdk-25.0.1

rem --- Make sure native DLLs can be found ---
set PATH=%FX_HOME%\bin;%PATH%

rem --- Run server ---
java ^
  --enable-native-access=javafx.graphics ^
  --module-path "%FX_HOME%\lib" ^
  --add-modules javafx.controls,javafx.fxml,javafx.graphics ^
  -cp "G13_Prototype_Server.jar;G13_Prototype_Server_lib\*" ^
  server.ServerMain

pause

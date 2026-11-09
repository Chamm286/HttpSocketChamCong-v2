@echo off
setlocal
cd /d "%~dp0"

echo [1/5] Kiem tra Java...
java -version >nul 2>&1
if errorlevel 1 (
    echo Khong tim thay java. Hay cai JDK 21 va them vao PATH.
    pause
    exit /b 1
)

echo [2/5] Bien dich toan bo source (Server + Client)...
if not exist bin mkdir bin
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -d bin ^
  --module-path "lib\javafx-sdk-21.0.5\lib" ^
  --add-modules javafx.controls,javafx.fxml ^
  -cp "lib\mysql-connector-j-26.7.0.jar;lib\ikonli-core-12.3.1.jar;lib\ikonli-javafx-12.3.1.jar;lib\ikonli-fontawesome5-pack-12.3.1.jar;lib\ikonli-fontawesome-pack-12.3.1.jar" ^
  @sources.txt
if errorlevel 1 (
    echo Compile that bai. Kiem tra JDK/JavaFX/MySQL driver.
    pause
    exit /b 1
)

echo [3/5] Khoi dong HTTP Socket Server (port 2020) trong cua so rieng...
start "ChamCong ServerApp" cmd /k java --module-path "lib\javafx-sdk-21.0.5\lib" --add-modules javafx.controls,javafx.fxml -cp "bin;lib\mysql-connector-j-26.7.0.jar;lib\ikonli-core-12.3.1.jar;lib\ikonli-javafx-12.3.1.jar;lib\ikonli-fontawesome5-pack-12.3.1.jar;lib\ikonli-fontawesome-pack-12.3.1.jar" com.httpchamcong.server.ServerApp --autostart

echo Doi server khoi dong (toi da 15 giay)...
powershell -NoProfile -Command "$ok=$false; for($i=0;$i -lt 30;$i++){ try { $c=New-Object Net.Sockets.TcpClient; $c.Connect('127.0.0.1',2020); $c.Close(); $ok=$true; break } catch { Start-Sleep -Milliseconds 500 } }; if(-not $ok){ exit 1 }"
if errorlevel 1 (
    echo Khong the ket noi toi port 2020. Kiem tra cua so ServerApp.
    pause
    exit /b 1
)

echo [4/5] Khoi dong JavaFX Desktop Client...
start "ChamCong Desktop Client" cmd /k java --module-path "lib\javafx-sdk-21.0.5\lib" --add-modules javafx.controls,javafx.fxml -cp "bin;lib\mysql-connector-j-26.7.0.jar;lib\ikonli-core-12.3.1.jar;lib\ikonli-javafx-12.3.1.jar;lib\ikonli-fontawesome5-pack-12.3.1.jar;lib\ikonli-fontawesome-pack-12.3.1.jar" com.httpchamcong.client.ClientApp

echo [5/5] Mo Web Client tren trinh duyet...
start "" "http://localhost:2020/"

echo Hoan tat. Server dang chay o http://localhost:2020/
echo Web Client: trinh duyet | Desktop Client: JavaFX
pause
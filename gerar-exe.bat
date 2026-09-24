@echo off
setlocal

cd /d "%~dp0"
set "APP_NAME=GeradorRecibos"
set "APP_VERSION=1.0.0"
set "MAIN_CLASS=com.seuusuario.App"
set "JAR_NAME=gerador-recibos-1.0-SNAPSHOT.jar"
set "INPUT_DIR=target\jpackage-input"
set "OUTPUT_DIR=dist"

if not defined JAVA_HOME for /d %%D in ("%ProgramFiles%\Eclipse Adoptium\jdk-17*") do set "JAVA_HOME=%%~fD"
if not defined MAVEN_HOME for /d %%D in ("%USERPROFILE%\tools\apache-maven-*") do set "MAVEN_HOME=%%~fD"
if exist "%ProgramFiles(x86)%\WiX Toolset v3.14\bin" set "PATH=%ProgramFiles(x86)%\WiX Toolset v3.14\bin;%PATH%"
if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"
if defined MAVEN_HOME set "PATH=%MAVEN_HOME%\bin;%PATH%"

where java >nul 2>&1 || goto :missing_java
where mvn >nul 2>&1 || goto :missing_maven
where jpackage >nul 2>&1 || goto :missing_jpackage

 echo [1/4] Compilando o aplicativo...
call mvn clean package -DskipTests
if errorlevel 1 goto :error

if not exist "target\%JAR_NAME%" (
    echo ERRO: JAR principal nao foi gerado.
    goto :error
)

 echo [2/4] Preparando os arquivos do aplicativo...
if exist "%INPUT_DIR%" rmdir /s /q "%INPUT_DIR%"
mkdir "%INPUT_DIR%"
copy /y "target\%JAR_NAME%" "%INPUT_DIR%\%JAR_NAME%" >nul
if exist "target\dependency" copy /y "target\dependency\*.jar" "%INPUT_DIR%\" >nul
del /q "%INPUT_DIR%\javafx-base-17.0.8.jar" 2>nul
del /q "%INPUT_DIR%\javafx-controls-17.0.8.jar" 2>nul
del /q "%INPUT_DIR%\javafx-graphics-17.0.8.jar" 2>nul

 echo [3/4] Gerando a versao portatil do aplicativo...
if exist "%OUTPUT_DIR%\%APP_NAME%-%APP_VERSION%.exe" del /q "%OUTPUT_DIR%\%APP_NAME%-%APP_VERSION%.exe"
if exist "%OUTPUT_DIR%\%APP_NAME%" rmdir /s /q "%OUTPUT_DIR%\%APP_NAME%"
if exist "%OUTPUT_DIR%\%APP_NAME%-portatil.zip" del /q "%OUTPUT_DIR%\%APP_NAME%-portatil.zip"

jpackage ^
  --type app-image ^
  --name "%APP_NAME%" ^
  --app-version "%APP_VERSION%" ^
  --input "%INPUT_DIR%" ^
  --dest "%OUTPUT_DIR%" ^
  --main-jar "%JAR_NAME%" ^
  --main-class "%MAIN_CLASS%" ^
  --java-options "--module-path $APPDIR" ^
  --java-options "--add-modules javafx.controls" ^
  --vendor "Gerador de Recibos" ^
  --description "Aplicativo para gerar recibos em PDF"
if errorlevel 1 goto :error

 echo [4/4] Criando um ZIP portatil para distribuicao...
%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe -NoProfile -Command "Compress-Archive -Path '%OUTPUT_DIR%\%APP_NAME%' -DestinationPath '%OUTPUT_DIR%\%APP_NAME%-portatil.zip' -Force"
if errorlevel 1 goto :error

echo Aplicativo portatil gerado com sucesso:
echo %CD%\%OUTPUT_DIR%\%APP_NAME%\%APP_NAME%.exe
echo ZIP portatil:
echo %CD%\%OUTPUT_DIR%\%APP_NAME%-portatil.zip
exit /b 0

:missing_java
echo ERRO: JDK 17 nao encontrado.
goto :error

:missing_maven
echo ERRO: Maven nao encontrado.
goto :error

:missing_jpackage
echo ERRO: jpackage nao encontrado. Use um JDK 17 completo, nao apenas um JRE.
goto :error

:error
echo A geracao do instalador falhou.
exit /b 1

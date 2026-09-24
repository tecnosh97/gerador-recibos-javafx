@echo off
setlocal
cd /d "%~dp0"

if "%~2"=="" (
    echo Uso: gerar-licenca.bat cliente dias-ou-PERMANENTE
    exit /b 1
)

if not exist "chave-privada-gerador.key" (
    echo ERRO: chave-privada-gerador.key nao encontrada.
    exit /b 1
)

if not defined MAVEN_HOME for /d %%D in ("%USERPROFILE%\tools\apache-maven-*") do set "MAVEN_HOME=%%~fD"
if defined MAVEN_HOME set "PATH=%MAVEN_HOME%\bin;%PATH%"

where mvn >nul 2>&1 || (
    echo ERRO: Maven nao encontrado.
    exit /b 1
)

call mvn -q -DskipTests package
if errorlevel 1 exit /b 1

java -cp "target\classes" com.seuusuario.LicenseKeyGenerator chave-privada-gerador.key "%~1" "%~2"
endlocal
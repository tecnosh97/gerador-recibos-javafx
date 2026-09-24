@echo off
setlocal
cd /d "%~dp0"

echo --- INICIANDO ATUALIZACAO DO PROJETO ---

git add .gitignore pom.xml atualizar.bat atualizar.sh gerar-exe.bat src
if errorlevel 1 exit /b 1

git diff --cached --quiet
if not errorlevel 1 (
	echo Nenhuma alteracao para enviar.
	exit /b 0
)

set /p "mensagem=Digite a mensagem da atualizacao: "
if "%mensagem%"=="" set "mensagem=Atualizacao do projeto"

git commit -m "%mensagem%"
if errorlevel 1 exit /b 1

git push origin main
if errorlevel 1 exit /b 1

echo --- PROJETO ATUALIZADO COM SUCESSO NO GITHUB! ---
endlocal


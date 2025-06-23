@echo off

if exist ".env" (
  for /f "usebackq tokens=1,2 delims==" %%i in (`type .env`) do (
    set %%i=%%j
  )
)

set "channelId=%channelId%"
if not "%~1"=="" set "channelId=%~1"

set "token=%token%"
if not "%~2"=="" set "token=%~2"

if "%token%"=="" (
  echo Usage: %~nx0 ^<channelId^> ^<token^>
  echo channelId and token can be set as environment variables, passed as arguments or read from a .env file.
  exit /b 1
)

if "%channelId%"=="" (
  echo Usage: %~nx0 ^<channelId^> ^<token^>
  echo channelId and token can be set as environment variables, passed as arguments or read from a .env file.
  exit /b 1
)

if not exist ".\data" (
  mkdir .\data
)

docker run --rm -v "%cd%\data:/out" tyrrrz/discordchatexporter:stable export -t "%token%" -c "%channelId%" -f Json
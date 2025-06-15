REM Description: This script runs a java process for a specified number of cold (fresh) runs.

@echo off
setlocal enabledelayedexpansion

REM Path to Gurobi JAR (adjust if needed)
set GUROBI_JAR=C:\gurobi1103\win64\lib\gurobi.jar

REM Number of cold runs
set RUNS=6

for /L %%i in (1,1,%RUNS%) do (
    echo ============================
    echo Run %%i
    echo ============================

    java -cp "out/production/TTP_GAI;!GUROBI_JAR!" Masterprobleem/Main

    echo Waiting 1 second before next run...
    REM N pings = N-1 seconds wait
    ping 127.0.0.1 -n 2 >nul
)
@ECHO OFF

REM Définir les variables (modifier les valeurs entre guillemets)

SET APP_DIR=%~dp0
SET LIB_DIR=%APP_DIR%lib

MKDIR "%APP_DIR%classes"

REM Copier les *.java dans un dossier temporaire tempjava
MKDIR "%APP_DIR%\tempjava"
for /R "%APP_DIR%" %%G IN (*.java) DO (
    XCOPY /Y "%%G" "%APP_DIR%\tempjava"
)

REM Compiler les classes Java
javac -parameters -cp "%LIB_DIR%\*" -d "%APP_DIR%classes" %APP_DIR%tempjava\*.java
ECHO javac -parameters -cp "%LIB_DIR%\*" -d "%APP_DIR%classes" %APP_DIR%tempjava\*.java


jar cvf framework.jar -C "%APP_DIR%classes" .

RD /S /Q "%APP_DIR%tempjava"
RD /S /Q "%APP_DIR%classes"
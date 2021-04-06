SET lib=..\..\java-advanced-2021\lib
SET art=..\..\java-advanced-2021\artifacts
SET dest=out\production\java-advanced-2021\
SET manifest=MANIFEST.MF

javac -d %dest% --module-path %lib%;%art% info\kgeorgiy\ja\monakhov\implementor\Implementor.java module-info.java

jar cfm Implementor.jar %manifest% -C %dest% .
rmdir /s /q %dest%


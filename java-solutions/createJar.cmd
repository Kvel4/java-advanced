SET impl=modules\info.kgeorgiy.ja.monakhov.implementor\info\kgeorgiy\ja\monakhov\implementor\
SET dep=modules\info.kgeorgiy.java.advanced.implementor\info\kgeorgiy\java\advanced\implementor\
SET dest=out\production\java-advanced-2021\
SET manifest=%proj%manifest.txt

javac -d %dest% %impl%\Implementor.java %dep%Impler.java %dep%JarImpler.java %dep%ImplerException.java

jar cfm Implementor.jar %manifest% -C %dest% .
rmdir /s /q %dest%


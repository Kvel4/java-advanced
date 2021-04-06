SET impl=info\kgeorgiy\ja\monakhov\implementor\
SET dep=..\..\java-advanced-2021\modules\info.kgeorgiy.java.advanced.implementor\info\kgeorgiy\java\advanced\implementor\
SET link=https://docs.oracle.com/en/java/javase/11/docs/api/

javadoc -d javaDoc -link %link% -private %impl%Implementor.java %dep%Impler.java %dep%JarImpler.java %dep%ImplerException.java
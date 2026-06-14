$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
Set-Location 'C:\Users\kyilm\OneDrive\MASAST~1\auditHub\auditHub'
& 'C:\Users\kyilm\OneDrive\MASAST~1\auditHub\apache-maven-3.9.16\bin\mvn.cmd' spring-boot:run

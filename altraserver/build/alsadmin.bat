@echo off
if "%1"=="" goto start
if "%1"=="start" (
	:start
	java -Xms1024m -Xmx1024m -Xmn768m -XX:MaxTenuringThreshold=3 -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:ParallelGCThreads=2 -Dsun.rmi.transport.tcp.responseTimeout=1000 -cp "./;./javaExtensions/;jar/*;jar/javamail/*" -Dfile.encoding=UTF-8 com.altratek.altraserver.AltraServer %2 %3 %4
) else (
	if "%1"=="stop" (
		java -cp "jar/alsadmin.jar;jar/altraserver.jar;jar/dom4j-1.6.1.jar;jar/mina-core-1.1.7.jar" Admin altraserver-admin altraserver-whoami! admin$zone-2008! %2 stop
	) else (
		if "%1"=="reloader" (
			java -cp "jar/alsadmin.jar;jar/altraserver.jar;jar/dom4j-1.6.1.jar;jar/mina-core-1.1.7.jar" Admin altraserver-admin altraserver-whoami! admin$zone-2008! %2 reloader %3 %4
		)
	)
)
@echo on

#!/bin/sh

if [ $1==1 ]
then
	javac client.java
	javac server.java
fi

if [ $1==2 ]
then
	javac client_en.java
	javac server_en.java
fi

if [ $1==3 ]
then
	javac client_sig.java
	javac server_sig.java
fi

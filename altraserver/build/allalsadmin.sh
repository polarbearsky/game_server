#!/bin/sh
if [ $1 == "start" ]
then
  ./alsadmin.sh start . workspace1 workspace1
  ./alsadmin.sh start . workspace2 workspace2
else
  if [ $1 == "stop" ]
  then
    ./alsadmin.sh stop 8000
    ./alsadmin.sh stop 8001
  else
    if [ $1 == "reloader" ]
    then
      ./alsadmin.sh reloader 8000 $2 $3
      ./alsadmin.sh reloader 8001 $2 $3
    fi
  fi
fi

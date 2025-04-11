#!/bin/bash
cd /home/pi/accessPoint
echo started: >> shell/auto.log
date >> shell/auto.log

sleep 15 #to wait till bluetooth is started properly

# run python main
/bin/python python/main.py 2> shell/error.log

echo finished unexpectedly >> shell/auto.log
exit 0
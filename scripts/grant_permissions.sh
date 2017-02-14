#!/bin/bash
#
# Usage
# You can run any command adb provides on all your currently connected devices
# ./adb+ <command> is the equivalent of ./adb -s <serial number> <command>
#

adb devices | while read line
do

    if [ ! "$line" = "" ] && [ `echo $line | awk '{print $2}'` = "device" ]
    then
        device=`echo $line | awk '{print $1}'`
        echo "$device $@ ..."
        adb -s $device $@
    fi
done
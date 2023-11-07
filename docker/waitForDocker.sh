#!/usr/bin/env bash
# Initial sleep to make sure docker has started
sleep 5


while : ; do
    STARTING=`docker ps -f "health=starting" --format "{{.Names}}"`
    if [ -z "$STARTING" ] #if STARTING is empty
    then
      break
    fi
    echo "Waiting for containers to finish starting"
    sleep 1
done


while : ; do

    # Get cpu % from docker stats, remove '%' and then sum all the values into one number
    CPU=`docker stats --no-stream --format "{{.CPUPerc}}" | awk '{gsub ( "[%]","" ) ; print $0 }' | awk '{s+=$1} END {print s}'`
    echo "CPU: $CPU%"

    # Do floating point comparison, if $CPU is bigger than 15, WAIT will be 1
    WAIT=`echo $CPU'>'15 | bc -l`
    echo "WAIT (0/1): $WAIT"

    sleep 1

     # Get cpu % from docker stats, remove '%' and then sum all the values into one number
    CPU2=`docker stats --no-stream --format "{{.CPUPerc}}" | awk '{gsub ( "[%]","" ) ; print $0 }' | awk '{s+=$1} END {print s}'`
    echo "CPU2: $CPU2%"

    # Do floating point comparison, if $CPU is bigger than 15, WAIT will be 1
    WAIT2=`echo $CPU'>'15 | bc -l`
    echo "WAIT2 (0/1): $WAIT2"

    # Break from loop if WAIT is 0, which is when the sum of the cpu usage is smaller than 15%
    [[ "$WAIT" -eq 0 ]] && [[ "$WAIT2" -eq 0 ]] && break

    # Else sleep and loop
    echo "Waiting for docker"
    sleep 1

done

while : ; do
    STARTING=`docker ps -f "health=starting" --format "{{.Names}}"`
    if [ -z "$STARTING" ] #if STARTING is empty
    then
      break
    fi
    echo "Waiting for containers to finish starting"
    sleep 1
done

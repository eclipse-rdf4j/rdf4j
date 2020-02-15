#!/bin/bash

REPORT_DIRS=`find . -name "surefire-reports" -type d`
for d in $REPORT_DIRS;
do
  MODULE=${d/\/target\/surefire-reports/}
  REPORTS=`find $d -type f -name "*.txt"`
  FAILURES=false
  for report in $REPORTS;
  do
    if egrep -q "FAILURE!|ERROR!" "$report"; then
      FAILURES=true
    fi
  done

  if $FAILURES; then
    echo ""
    echo "###### ${MODULE} ######"
    echo ""
    for report in $REPORTS;
    do
     if egrep -q "FAILURE!|ERROR!" "$report"; then
        cat $report
      fi
    done
    echo ""
  fi
done


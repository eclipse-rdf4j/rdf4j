#!/bin/bash

for path in ./*.tgz; do
    archive=$(basename -- "$path")
    dirname="${archive%.*}"
    if test -d "$dirname"; then
        if [ "$dirname" -ot "$archive" ]; then
            tar xzvf $archive
        fi
    else
        tar xzvf $archive
    fi
    touch $dirname 
done


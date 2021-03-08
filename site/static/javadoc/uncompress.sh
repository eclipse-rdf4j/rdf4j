#!/bin/bash

for path in ./*.tgz; do
    archive=$(basename -- "$path")
    dirname="${archive%.*}"
    if test -d "$dirname"; then
        if [ "$dirname" -ot "$archive" ]; then
            tar xzf $archive -C $dirname
        fi
    else
        mkdir $dirname
        tar xzf $archive -C $dirname
    fi
    touch $dirname 
done


#!/bin/bash

INPUT=$1

if echo ${INPUT} | egrep -i '\.zip$'; then
  echo "Uncompressing ${INPUT}"
  7za x ./${INPUT}
  result=$?
elif echo ${INPUT} | egrep -i '\.(tar\.gz|tar|tgz)$'; then
  echo "Uncompressing tarred ${INPUT}"
  
  # if the tar doesn't contain any subdirectories, create a subdirectory with the basename
  # of the tar
  if tar tf ./${INPUT} | grep -v /; then
    # remove the compression suffix
    SUBDIR=$(echo $INPUT | sed -r 's;\.(tar\.gz|tar|tgz)$;;gI')
    echo "Creating subdirectory ${SUBDIR}"
    mkdir ${SUBDIR}
    tar xf ./${INPUT} -C ${SUBDIR}
    result=$?
  else
    # since tar 1.15 (2004-12-20), 'z' doesn't need to be provided to properly untar gzipped files
    # see https://www.gnu.org/software/tar/
    tar xf ./${INPUT}
    result=$?
  fi
else
  echo "ERROR: Unexpected file to uncompress ${INPUT}. Supported are: .zip, .tar.gz, .tgz and tar"  
  exit 1
fi

if [ ${result} -eq 0 ]
then
  echo "Uncompression script done."
  exit ${result}
else
  echo "Error during uncompression script excecution." >&2
  exit ${result}
fi

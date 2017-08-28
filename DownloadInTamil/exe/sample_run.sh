#!/bin/bash
java -version
# Make sure you have Java 1.8 installed

# Set HTTP Proxy if needed
# export http_proxy=http://<host>:<port>
# export https_proxy=$http_proxy

# Run me
java -jar DownloadInTamil-EXE.jar --outdir=/tmp --search=mersal

read -p "Enter to Quit"
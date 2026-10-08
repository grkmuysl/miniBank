FROM ubuntu:latest
LABEL authors="gorkm"

ENTRYPOINT ["top", "-b"]
#!/bin/bash

# Copyright (C) 2020, Martin Drößler <m.droessler@handelsblattgroup.com>
# Copyright (C) 2020, Handelsblatt GmbH
#
# This file is part of pki-web / client-certificate-webapp
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.


PROCESS_SEARCH_TERM="pki-web.*jar"
JAR_NAME="pki-web.jar"
JAR_FILE="/server/data/applications/pki-web/${JAR_NAME}"
JAVA_BIN="/server/data/applications/java/bin/java"

function usage() {
    echo "Usage: $0 start|stop|restart|status";
    exit;
}

function status() {
    running=`ps -ef|grep -i "${PROCESS_SEARCH_TERM}"| grep -v "grep" | wc -l`
    if [ ${running} -ge 1 ]; then
        echo "pki-web is running";
    else
        echo "pki-web is NOT running";
    fi
    exit;
}

function stop() {
    CPID=`ps -eo pid,cmd|grep "${PROCESS_SEARCH_TERM}"|grep -v "grep" | sed -e 's/^[[:space:]]\?\([0-9]\+\) .*/\1/gi'`
    if [ ! -z $CPID ]; then
        echo "Stopping pki-web...";
        kill $CPID

        sleep 2;
        # check again
        CPID=`ps -eo pid,cmd|grep "${PROCESS_SEARCH_TERM}"|grep -v "grep" | sed -e 's/^[[:space:]]\?\([0-9]\+\) .*/\1/gi'`
        if [ ! -z $CPID ]; then
            echo "pki-web did not shut down in time. Force kill...";
            kill -9 $CPID
        fi
    else
        echo "pki-web is NOT running";
    fi
}

function start() {
    running=`ps -ef | grep -i "${PROCESS_SEARCH_TERM}" | grep -v "grep" | wc -l`
    if [ ${running} -ge 1 ]; then
        echo "pki-web is already running";
        exit;
    fi

    ${JAVA_BIN} -jar ${JAR_FILE} > /dev/null 2>&1 &
}

function restart() {
    stop
    start
}


if [ $# -eq 0 ]; then
    usage
    exit;
fi

for i in "$@"; do
    case $i in
        "start" )
            start
        ;;
        "stop" )
            stop
        ;;
        "restart" )
            restart
        ;;
        "status" )
            status
        ;;
        * )
            usage
        ;;
    esac
done

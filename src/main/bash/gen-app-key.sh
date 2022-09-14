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

echoerr() { echo "$@" 1>&2; }

workDir=$1
appName=$2
password=$3

if [ ! -d "${workDir}" ]; then
  echoerr "No such directory: ${workDir}";
  exit 1;
fi

if [ ! -f "${workDir}/openssl.cnf.tmpl" ]; then
  echoerr "Invalid working directory (missing config-template): ${workDir}";
  exit 1;
fi

if [ -z "${appName}" ]; then
  echoerr "No App-Name specified! Aborting";
  exit 1;
fi

if [ -d "${workDir}/${appName}" ]; then
  echoerr "App-Name already exists! Aborting";
  exit 1;
fi

if [ -z "${password}" ]; then
  echoerr "No key password specified!";
  exit 1;
fi

mkdir "${workDir}/${appName}"
cd "${workDir}/${appName}"

openssl genrsa -aes256 -passout pass:"${password}" -out "${appName}.key.pem" 4096

rc=$?
if [ $rc -gt 0 ]; then
  exit $rc;
fi

echo "Keyfile is in:
${workDir}/${appName}/${appName}.key.pem";

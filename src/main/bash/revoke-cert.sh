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


workDir=$1
certFile=$2
password=$3

if [ ! -d "${workDir}" ]; then
  echo "No such directory: ${workDir}";
  exit 1;
fi

if [ ! -f "${workDir}/intermediate/openssl.cnf" ]; then
  echo "Invalid working directory: ${workDir}}";
  exit 1;
fi

if [ -z "${certFile}" -o ! -f "${certFile}" ]; then
  echo "No or invalid Certificate-File specified! Aborting";
  exit 1;
fi

if [ -z "${password}" ]; then
  echo "No key password specified!";
  exit 1;
fi

# working directory is IMPORTANT!
# All paths in openssl.cnf are relative!
cd "${workDir}"

openssl ca -batch -config intermediate/openssl.cnf -passin pass:"${password}" -revoke "${certFile}"

rc=$?
if [ $rc -gt 0 ]; then
  exit $rc;
fi

echo "Certificate revoked!";

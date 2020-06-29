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


echo "Checking preconditions...";

opensslpath=$(which openssl)
if [ $? -gt 0 ]; then
	echo "${opensslpath}";
	exit 1;
fi

echo "Create a private key...";

username=$(whoami)
if [ ! -z "$1" ]; then
	echo "Username override: $1";
	username=$1
fi

sed -e "s/REPLACE_USERNAME/${username}/g" openssl.cnf.tmpl > openssl.cnf

openssl genrsa -aes256 -out "${username}.key.pem" 4096
echo "DONE";


echo "Create a CSR...";

openssl req -config openssl.cnf -utf8 -new -key "${username}.key.pem" -out "${username}.csr.pem"
echo "DONE";

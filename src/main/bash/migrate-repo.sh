#!/bin/bash

##
# migrate the cert-repository from openssl 1.x to openssl 3.x
# See: https://github.com/openssl/openssl/issues/19730
#
# OpenSSL 3 will fail to decrypt files, encrypted with OpenSSL 1, when using a fixed salt!
# There is a workaround however:
# "Actually the 1.1.1 encrypted file is possible to decrypt with 3.0 without problems - just drop the -S option from the command line."
#
# ...but as this is a one-time only operation, we need this script to do it
#

workDir=$1
cryptSalt=$2
password=$3
opensslVersion=$(openssl version | tr '[:upper:]' '[:lower:]')

#
# check parameters and environment
#
if [ ! -d "${workDir}" ]; then
  echo "No such directory: ${workDir}";
  exit 1;
fi
if [ ! -f "${workDir}/intermediate/openssl.cnf" ]; then
  echo "Invalid working directory: ${workDir}}";
  exit 1;
fi

if [ -z "${cryptSalt}" ]; then
  echo "No salt specified!";
  exit 1;
fi

if [ -z "${password}" ]; then
  echo "No password specified!";
  exit 1;
fi

if [[ ! "${opensslVersion}" =~ "openssl 3." ]]; then
  echo "OpenSSL Version is still < 3 - no migration possible";
  exit 1;
fi

# working directory is IMPORTANT!
# All paths in openssl.cnf are relative!
cd "${workDir}"


echo "decypting files in compat-mode..."
while read g
do 
  # we must not use the salt, to properly decrypt the openssl v1 files with openssl v3
  # Also see: https://github.com/openssl/openssl/issues/19730#issuecomment-1323858675
  openssl enc -d -p -aes256 -a -pbkdf2 -iter 20000 -pass pass:${password} -in "${g}" -out "${g}.tmp"
  mv "${g}.tmp" "${g}"
done < <(find . -type f -iname "*.pem" -o -iname "*.pfx" -o -iname "*.csr" -o -iname "*.crt")
echo "DONE"

echo "Check the files and verify that they have been decrypted properly!
Waiting for confirmation before proceeding.
"

# give the user a chance to verify, that the operation was successful.
read -p "decryption was successful? (y/n): " userinput

confirm=$(echo "${userinput}" | tr '[:upper:]' '[:lower:]' | tr -d '[:space:]')
if [[ ! "${confirm}" == "y" ]]; then
  echo "OK, stop here";
  exit 1;
fi

echo "encrypting files again..."
while read g
do 
  # later on, the salt is required again - so use it to re-encrypt the files again
  openssl enc -p -aes256 -a -S ${cryptSalt} -pbkdf2 -iter 20000 -pass pass:${password} -in "${g}" -out "${g}.tmp"
  mv "${g}.tmp" "${g}"
done < <(find . -type f -iname "*.pem" -o -iname "*.pfx" -o -iname "*.csr" -o -iname "*.crt")
echo "DONE"

echo "Don't forget to commit and push the changes!"

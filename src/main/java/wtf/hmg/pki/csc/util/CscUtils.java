/*
 Copyright (C) 2020, Martin Drößler <m.droessler@handelsblattgroup.com>
 Copyright (C) 2020, Handelsblatt GmbH

 This file is part of pki-web / client-certificate-webapp

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package wtf.hmg.pki.csc.util;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public class CscUtils {

    public static final Logger log = LoggerFactory.getLogger(CscUtils.class);

    private CscUtils() {}

    public static boolean isValidCSRFile(final Path path, final BasicFileAttributes attr) {
        return attr.isRegularFile() && isValidCSRFileName(path.getFileName().toString());
    }

    public static boolean isSignedCSRFile(final Path path, final BasicFileAttributes attributes) {
        return isValidCSRFile(path, attributes) && isAccepted(path);
    }

    private static boolean isAccepted(final Path path) {
        return path.getParent() != null && path.getParent().getFileName() != null &&
                "accepted".equalsIgnoreCase(path.getParent().getFileName().toString());
    }

    public static boolean isValidCSRFileName(final String fileName) {
        return StringUtils.endsWithAny(fileName, "pem", "csr");
    }

    public static boolean isRevokedCertFile(final Path path, final BasicFileAttributes attr) {
        return attr.isRegularFile() && isValidCertFileName(path.getFileName().toString()) && isRevoked(path);
    }
    
    public static boolean isValidCertFileName(final String fileName) {
        return StringUtils.endsWithAny(fileName, "crt.pem", "crt");
    }
    
    private static boolean isRevoked(final Path path) {
        return path.getParent() != null && path.getParent().getFileName() != null &&
                "revoked".equalsIgnoreCase(path.getParent().getFileName().toString());
    }
    
    public static boolean validateCSR(final Path csrFile) {
        try {
            return validateCSRInternal(Files.newBufferedReader(csrFile));
        } catch (IOException e) {
            log.error("Unable to read CSR-Input file!", e);
        }

        return false;
    }

    public static boolean validateCSRString(final String csrString) {
        return validateCSRInternal(new StringReader(csrString));
    }

    private static boolean validateCSRInternal(final Reader csrReader) {
        try {
            PEMParser pp = new PEMParser(csrReader);
            PKCS10CertificationRequest csr = (PKCS10CertificationRequest) pp.readObject();
            return csr != null;
        } catch (IOException e) {
            log.error("Unable to parse CSR-Input file!", e);
        } finally {
            closeQuietly(csrReader);
        }
        return false;
    }

    public static String normalizeUserName(final String userName) {
        return StringUtils.replace(userName, "@", "_");
    }

    public static String extractCSRInfo(final Path csrFile) {
        try(PEMParser pp = new PEMParser(Files.newBufferedReader(csrFile));) {
            PKCS10CertificationRequest csr = (PKCS10CertificationRequest) pp.readObject();
            return csr.getSubject().toString();
        } catch (IOException|NullPointerException e) {
            log.error("Unable to parse CSR-Input file!", e);
        }
        return null;
    }

    public static void closeQuietly(final Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            /* empty by design */
        }
    }
    
}

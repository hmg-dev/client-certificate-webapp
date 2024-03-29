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
package wtf.hmg.pki.csc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.util.List;

@Configuration
@ConfigurationProperties("data")
public class AppConfig {
    private Path storagePath;
    private String certRepo;
    private Path gitUserKeyfile;
    private String cryptSalt;
    private Path certRevocationListPath;
    private Path scriptsPath;
    
    private List<String> allowedMailSuffixes;
    private List<String> notificationRecipients;
    private String notificationSender;
    private boolean notificationsEnabled;
    
    public Path getStoragePath() {
        return storagePath;
    }

    public String getCertRepo() {
        return certRepo;
    }

    public Path getGitUserKeyfile() {
        return gitUserKeyfile;
    }

    public String getCryptSalt() {
        return cryptSalt;
    }

    public Path getCertRevocationListPath() {
        return certRevocationListPath;
    }

    public void setStoragePath(final Path storagePath) {
        this.storagePath = storagePath;
    }

    public void setCertRepo(final String certRepo) {
        this.certRepo = certRepo;
    }

    public void setGitUserKeyfile(final Path gitUserKeyfile) {
        this.gitUserKeyfile = gitUserKeyfile;
    }

    public void setCryptSalt(final String cryptSalt) {
        this.cryptSalt = cryptSalt;
    }

    public void setCertRevocationListPath(final Path certRevocationListPath) {
        this.certRevocationListPath = certRevocationListPath;
    }
    
    public Path getScriptsPath() {
        return scriptsPath;
    }
    
    public void setScriptsPath(final Path scriptsPath) {
        this.scriptsPath = scriptsPath;
    }
    
    public List<String> getAllowedMailSuffixes() {
        return allowedMailSuffixes;
    }
    
    public void setAllowedMailSuffixes(final List<String> allowedMailSuffixes) {
        this.allowedMailSuffixes = allowedMailSuffixes;
    }
    
    public List<String> getNotificationRecipients() {
        return notificationRecipients;
    }
    
    public void setNotificationRecipients(final List<String> notificationRecipients) {
        this.notificationRecipients = notificationRecipients;
    }
    
    public String getNotificationSender() {
        return notificationSender;
    }
    
    public void setNotificationSender(final String notificationSender) {
        this.notificationSender = notificationSender;
    }
    
    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }
    
    public void setNotificationsEnabled(final boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }
}

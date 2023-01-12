/*
 Copyright (C) 2021, Martin Drößler <m.droessler@handelsblattgroup.com>
 Copyright (C) 2021, Handelsblatt GmbH

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
package wtf.hmg.pki.csc.service;

import org.springframework.core.io.Resource;
import wtf.hmg.pki.csc.model.SharedApp;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface SharedAppService {
	String SHARED_APPS_FOLDER = "applications";
	
	boolean isValidEMail(String email);
	
	String createAppKey(String appName) throws IOException;
	
	void createCSR(String appName, String password) throws IOException;
	
	void createAppDetails(String appName, String teamName, String contact);
	
	Path findAppFileForFilename(String appName, String fileName);
	
	boolean deleteAppFile(String appName, String fileName) throws IOException;
	
	Resource appFileAsResource(String appName, String filename) throws IOException;
	
	void requestRenewalForCert(String appName, String certFileName) throws IOException;
	
	List<SharedApp> findSharedApps();
}

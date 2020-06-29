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
package wtf.hmg.pki.csc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import wtf.hmg.pki.csc.config.AppConfig;
import wtf.hmg.pki.csc.service.FilesService;

import java.nio.file.Path;

@Controller
public class HealthCheckController {

    @Autowired
    private FilesService filesService;
    @Autowired
    private AppConfig appConfig;

    @GetMapping("/check")
    public String check() {
        Path storagePath = appConfig.getStoragePath();
        if(!filesService.exists(storagePath.resolve("users"))) {
            return "check_notok";
        }

        return "check_ok";
    }

    public void setAppConfig(final AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    public void setFilesService(final FilesService filesService) {
        this.filesService = filesService;
    }
}

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
package wtf.hmg.pki.csc.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import wtf.hmg.pki.csc.service.FilesService;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

/**
 * Mocking calls to {@link Files} does not work, even with Powermock.
 */
@Service
public class WrapperFilesService implements FilesService {

    @Override
    public Stream<Path> find(Path start, int maxDepth,
                             BiPredicate<Path, BasicFileAttributes> matcher,
                             FileVisitOption... options) throws IOException {
        return Files.find(start, maxDepth, matcher, options);
    }

    @Override
    public Path move(Path source, Path target, CopyOption... options) throws IOException {
        return Files.move(source, target, options);
    }

    @Override
    public boolean exists(Path path, LinkOption... options) {
        return Files.exists(path, options);
    }

    @Override
    public boolean deleteRecursively(Path root) throws IOException {
        return FileSystemUtils.deleteRecursively(root);
    }

    @Override
    public Path copy(Path source, Path target, CopyOption... options) throws IOException {
        return Files.copy(source, target, options);
    }

    @Override
    public Path createDirectories(Path dir, FileAttribute<?>... attrs) throws IOException {
        return Files.createDirectories(dir, attrs);
    }
    
    @Override
    public Path createFile(Path path, FileAttribute<?>... attrs) throws IOException {
        return Files.createFile(path, attrs);
    }
    
    @Override
    public boolean isRegularFile(Path path, LinkOption... options) {
        return Files.isRegularFile(path, options);
    }
    
    @Override
    public Path setLastModifiedTime(Path path, FileTime time) throws IOException {
        return Files.setLastModifiedTime(path, time);
    }
    
    @Override
    public FileTime getLastModifiedTime(Path path, LinkOption... options) throws IOException {
        return Files.getLastModifiedTime(path, options);
    }
}

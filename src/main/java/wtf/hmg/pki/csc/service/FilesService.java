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
package wtf.hmg.pki.csc.service;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitOption;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

public interface FilesService {

    boolean exists(Path path, LinkOption... options);

    Path move(Path source, Path target, CopyOption... options) throws IOException;

    Stream<Path> find(Path start, int maxDepth,
                      BiPredicate<Path, BasicFileAttributes> matcher,
                      FileVisitOption... options) throws IOException;

    boolean deleteRecursively(Path root) throws IOException;

    Path copy(Path source, Path target, CopyOption... options) throws IOException;

    Path createDirectories(Path dir, FileAttribute<?>... attrs) throws IOException;
	
	Path createFile(Path path, FileAttribute<?>... attrs) throws IOException;
	
	Path setLastModifiedTime(Path path, FileTime time) throws IOException;
}

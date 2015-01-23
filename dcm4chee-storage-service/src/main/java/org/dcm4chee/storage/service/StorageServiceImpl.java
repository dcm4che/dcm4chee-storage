/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 * Java(TM), hosted at https://github.com/gunterze/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Agfa Healthcare.
 * Portions created by the Initial Developer are Copyright (C) 2012
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See @authors listed below
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.dcm4chee.storage.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Format;
import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.DicomConfiguration;
import org.dcm4che3.net.Device;
import org.dcm4che3.util.TagUtils;
import org.dcm4chee.storage.conf.StorageAvailability;
import org.dcm4chee.storage.conf.ByteSize;
import org.dcm4chee.storage.conf.Filesystem;
import org.dcm4chee.storage.conf.FilesystemGroup;
import org.dcm4chee.storage.conf.Storage;
import org.dcm4chee.storage.conf.StorageConfiguration;
import org.dcm4chee.storage.entity.BaseFileRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class StorageServiceImpl implements StorageService {

    private static Logger log = LoggerFactory.getLogger(StorageServiceImpl.class);

    private static final int BUFFER_SIZE = 65536;

    @Inject
    @Any
    private DicomConfiguration conf;



    @Inject
    @Storage    
    private Device device;
    
    @Inject
    @Storage
    private Format pathFormatter;

    private StorageConfiguration cfg;

    private HashMap<String, String> currentFilesystem = new HashMap<String, String>();

    @PostConstruct
    public void init() {
        cfg = device.getDeviceExtension(StorageConfiguration.class);
    }

    @Override
    public void start() throws Exception {
    }

    @Override
    public void stop() {
    }

    @Override
    public void reload() throws Exception {
        device.reconfigure(conf.findDevice(device.getDeviceName()));
    }

    @Override
    public Device getDevice() {
        return device;
    }

    @Override
    public boolean isRunning() {
        return true;
    }

    /*
     * Filesystem service methods
     * @see org.dcm4chee.storage.file.StorageService#createFilesystem(java.lang.String, java.lang.String, org.dcm4chee.storage.entity.Availability, org.dcm4chee.storage.entity.FileSystemStatus)
     */

    @Override
    public Filesystem getRWFilesystem(String groupID, long filesize) throws ConfigurationException {
        FilesystemGroup grp = this.getFilesystemGroupCfg(groupID);
        synchronized (grp) {
            Filesystem fs = grp.getFilesystem(currentFilesystem.get(groupID));
            if (fs == null) {
                fs = cfg.getWritableFilesystem(groupID);
                if (fs == null) 
                    throw new ConfigurationException("No writable Filesystem configured in group "+groupID);
                currentFilesystem.put(groupID, fs.getId());
            }
            long minFree = getMinFree(grp, fs);
            long requiredSpace = filesize + minFree;
            Filesystem tmp = fs;
            String mountCheckFile = grp.getMountFailedCheckFile();
            while (!isMounted(tmp, mountCheckFile) || !checkFreeSpace(tmp, requiredSpace)) {
                log.info("Filesystem {} has not enough free space ({} bytes)! Switch to next filesystem.", tmp, requiredSpace);
                tmp = switchFilesystem(tmp);
                if (tmp == null || tmp.equals(fs)) {
                    log.error("No writable filesystem in group {} with sufficient space ({} bytes) available!", groupID, requiredSpace);
                    return null;
                }
            }
            if (tmp == fs) {
                log.debug("use current filesystem:{}", tmp);   
            } else {
                log.debug("Writable filesystem found:{}", tmp);
                currentFilesystem.put(groupID, tmp.getId());
            }
            return tmp;
        }
    }

    @Override
    public long getFreeSpaceOf(Path path) {
        while (path != null && !Files.exists(path))
            path = path.getParent();
        return path == null ? -1 : path.toFile().getFreeSpace();
    }

    public long getTotalSpaceOf(Path path) {
        while (path != null && !Files.exists(path))
            path = path.getParent();
        return path == null ? -1 : path.toFile().getTotalSpace();
    }

    public Path calcFilePath(Path basePath, Object obj) throws IOException {
        String filePath;
        synchronized (pathFormatter) {
            filePath = pathFormatter.format(obj);
        }
        Path fullPath = basePath.resolve(filePath.replace('/', File.separatorChar));
        String filename = fullPath.getFileName().toString();
        Files.createDirectories(fullPath.getParent());
        int copy = 1;
        while (true) {
            try {
                return Files.createFile(fullPath);
            } catch (IOException x) {
                fullPath = fullPath.resolveSibling(filename + '.' + copy++);
            }
        }
        
    }

    @Override
    public StorageResult store(InputStream is, String groupID, Object pathDescriptor, MessageDigest md)
            throws ConfigurationException, IOException, NoSuchAlgorithmException {
        StorageResult result = new StorageResult();
        Filesystem fs = getRWFilesystem(groupID, (long) is.available());
        if (fs != null) {
            Path fsPath = fs.getPath();
            Path path = calcFilePath(fsPath, pathDescriptor);
            Path filePath = path.subpath(fsPath.getNameCount(), path.getNameCount());
            try (InputStream dis = prepareInputStream(is, md)){
                log.info("M-CREATE file "+path);
                Files.copy(dis, path, StandardCopyOption.REPLACE_EXISTING);
                result.filesystem = fs;
                result.filePath = filePath;
                result.hash = md == null ? null : TagUtils.toHexString(md.digest());
                result.size = Files.size(path);
            } catch (IOException x) {
                log.error("Store file "+path+" failed!", x);
                deleteFileAndParentDirectories(path);
                throw x;
            }
        } else {
            result.errorMsg = "Not enough space in group "+groupID+" to store input stream with size "+is.available()+"!";
        }
        return result;
    }

    @Override
    public StorageResult move(Path source, String groupID, Object pathDescriptor) throws ConfigurationException, IOException, NoSuchAlgorithmException {
        StorageResult result = new StorageResult();
        if (!Files.isRegularFile(source)) {
            result.errorMsg = "Source must be a regular File";
        } else {
            Filesystem fs = getRWFilesystem(groupID, Files.size(source));
            if (fs != null) {
                Path fsPath = fs.getPath();
                Path path = calcFilePath(fsPath, pathDescriptor);
                Path filePath = path.subpath(fsPath.getNameCount(), path.getNameCount());
                try {
                    log.info("M-CREATE file "+path);
                    Files.move(source, path, StandardCopyOption.REPLACE_EXISTING);
                    result.filesystem = fs;
                    result.filePath = filePath;
                    result.size = Files.size(path);
                } catch (IOException x) {
                    log.error("Store file "+path+" failed!", x);
                    deleteFileAndParentDirectories(path);
                    throw x;
                }
            } else {
                result.errorMsg = "Not enough space in group "+groupID+" to move file "+source+" with size "+Files.size(source)+"!";
            }
        }
        return result;
    }

    @Override
    public boolean checkDigest(InputStream is, String digest, MessageDigest md) {
        try (InputStream dis = prepareInputStream(is, md)) {
            byte[] buf = new byte[Math.min(is.available(), BUFFER_SIZE)];
            while( dis.read(buf) != -1);
            String digest2 = TagUtils.toHexString(md.digest());
            if (digest2.equals(digest))
                return true;
            log.warn("Input has wrong digest ({})! expected: {} ", digest2, digest);
        } catch (Exception x) {
            log.error("checkDigest failed!", x);
        }
        return false;
    }

    public void deleteFileAndParentDirectories(Path path) {
        log.info("Delete File and empty parent directories:"+path);
        try {
            if (Files.deleteIfExists(path))
                log.info("M-DELETE file "+path);
            while ((path = path.getParent()) != null) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                    if (!stream.iterator().hasNext()) {
                        log.info("M-DELETE empty directory "+path);
                        Files.delete(path);
                    }
                }
            }
        } catch (Exception x) {
            log.error("Failed to delete File and empty parents:"+path);
        }
    }

    private InputStream prepareInputStream(InputStream is, MessageDigest md)
            throws NoSuchAlgorithmException {
        if (md == null)
            return is;
        md.reset();
        DigestInputStream dis;
        if (is instanceof DigestInputStream) {
            dis = (DigestInputStream)is;
        } else {
            dis = new DigestInputStream(is, md);
        }
        return dis;
    }

    private boolean checkFreeSpace(Filesystem fs, long requiredSpace) {
        return getFreeSpaceOf(fs.getPath()) > requiredSpace;
    }

    private boolean isMounted(Filesystem fs, String mountCheckFile) {
        if (mountCheckFile != null && Files.exists(fs.getPath().resolve(mountCheckFile))) {
            log.warn("Filesystem "+fs+" is not mounted! 'Mount failed' file exists:"+mountCheckFile);
            return false;
        }
        return true;
    }

    private long getMinFree(FilesystemGroup grp, Filesystem fs) throws ConfigurationException {
        ByteSize minFree = grp.getMinFreeDiskSpaceByteSize();
        return minFree.isRelative() ? minFree.getSize(getTotalSpaceOf(fs.getPath())) : minFree.getSize();
    }

    private FilesystemGroup getFilesystemGroupCfg(String groupID) throws ConfigurationException {
        FilesystemGroup grp = cfg == null ? null : cfg.getFilesystemGroup(groupID);
        if (grp == null) {
            throw new ConfigurationException("Filesystem group with ID "+groupID+" not found!");
        }
        return grp;
    }

    @Override
    public Filesystem switchRWFilesystem(String groupID) throws ConfigurationException {
        Filesystem fs = cfg.getFilesystemGroup(groupID).getFilesystem(currentFilesystem.get(groupID));
        if (fs == null) {
            return this.getRWFilesystem(groupID, 0);
        } else {
            return switchFilesystem(fs);
        }
    }

    private Filesystem switchFilesystem(Filesystem fs) {
        Filesystem next = fs;
        do {
            next =next.getNextFilesystem();
            log.debug("Next configured Filesystem of {} is {}!", fs, next);
            if (next == null || next.getId().equals(fs.getId()))
                return null;
        } while (!next.isWritable() || next.getAvailability().ordinal() > StorageAvailability.NEARLINE.ordinal());
        log.debug("Next valid Filesystem of {} is {}!", fs, next);
        return next;
    }
    
    @Override
    public Path toPath(BaseFileRef f) {
        FilesystemGroup grp = cfg.getFilesystemGroup(f.getGroupID());
        Filesystem fs = grp.getFilesystem(f.getFilesystemID());
        return fs.getPath().resolve(f.getFilePath().replace('/', File.separatorChar));
    }

}

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

package org.dcm4chee.storage.conf;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.core.api.ConfigurableClass;
import org.dcm4che3.conf.core.api.ConfigurableProperty;
import org.dcm4che3.conf.core.api.LDAP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 */
@LDAP(objectClasses = "dcm4cheeFilesystemgroup")
@ConfigurableClass
public class FilesystemGroup implements Serializable {

    private static final long serialVersionUID = -8258532093950989486L;
    private static Logger log = LoggerFactory.getLogger(FilesystemGroup.class);

    public FilesystemGroup() {

    }

    public FilesystemGroup(String groupID, String minFreeDiskSpace) {
        this.id = groupID;
        setMinFreeDiskSpace(minFreeDiskSpace);
    }

    @ConfigurableProperty(name = "storageFileSystemGroupID", label = "Group ID")
    private String id;

    @ConfigurableProperty(
            name = "storageMinFreeDiskSpace",
            label = "Min. free space",
            description = "Threshold of free space of active Storage System in format &lt;integer&gt;{kB|MB|GB|KiB|MiB|GiB} or &lt;integer&gt;%")
    private String minFreeDiskSpace;

    private ByteSize byteSize;

    @ConfigurableProperty(name = "storageMountFailedCheckFile",
            label = "Mount fail check file",
            description = "Specifies path of file which appearance indicates a failed mount"
    )
    private String mountFailedCheckFile;

    @LDAP(distinguishingField = "storageFilesystemID")
    @ConfigurableProperty(name = "filesystems", label = "File systems", order = 4)
    private Map<String, Filesystem> filesystems = new HashMap<String, Filesystem>(5);

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMinFreeDiskSpace() {
        return minFreeDiskSpace;
    }

    public void setMinFreeDiskSpace(String storageMinFreeDiskSpace) {
        byteSize = new ByteSize(storageMinFreeDiskSpace);
        this.minFreeDiskSpace = storageMinFreeDiskSpace;
    }

    public ByteSize getMinFreeDiskSpaceByteSize() {
        return byteSize;
    }

    public String getMountFailedCheckFile() {
        return mountFailedCheckFile;
    }

    public void setMountFailedCheckFile(String mountFailedCheckFile) {
        this.mountFailedCheckFile = mountFailedCheckFile;
    }

    public Map<String, Filesystem> getFilesystems() {
        return filesystems;
    }

    public void setFilesystems(Map<String, Filesystem> filesystems) throws ConfigurationException {
        this.filesystems = filesystems;
        for (Filesystem fs : filesystems.values()) {
            fs.initNextFilesystem(this);
        }
    }

    public void addFilesystem(Filesystem fs) {
        if (filesystems.containsKey(fs.getId()))
            throw new IllegalArgumentException("This filesystem group already contains a filesystem with id " + fs.getId());
        filesystems.put(fs.getId(), fs);
    }

    public Filesystem removeFilesystem(Filesystem fs) {
        Filesystem removed = filesystems.remove(fs.getId());
        if (removed != null) {
            String id = fs.getId();
            try {
                for (Filesystem tmp : filesystems.values()) {
                    if (id.equals(tmp.getNextFilesystemReference())) {
                        tmp.setNextFilesystem(tmp.getId().equals(removed.getNextFilesystemReference()) ? null : removed.getNextFilesystem());
                    }
                }
            } catch (ConfigurationException e) {
                log.error("Error updating next filesystem after removeFilesystem!", e);
            }
        }
        return removed;
    }

    public Filesystem getFilesystem(String filesystemID) {
        return filesystems.get(filesystemID);
    }

}

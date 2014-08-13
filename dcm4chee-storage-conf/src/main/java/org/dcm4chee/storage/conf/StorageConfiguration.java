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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.generic.ConfigClass;
import org.dcm4che3.conf.api.generic.ConfigField;
import org.dcm4che3.conf.api.generic.ReflectiveConfig;
import org.dcm4che3.net.DeviceExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 */

@ConfigClass(commonName = "StorageProvider", objectClass = "dcm4cheeStorage", nodeName = "fileStorage")
public class StorageConfiguration extends DeviceExtension {


    private static final long serialVersionUID = -8258532093950989486L;
    private static Logger log = LoggerFactory.getLogger(StorageConfiguration.class);

    @ConfigField(name = "storageApplicationName")
    private String applicationName;

    @ConfigField(mapName = "filesystemGroups", mapKey = "storageFilesystemGroupID", name = "storageFilesystemGroups", mapElementObjectClass = "filesystemGroupByID")
    private Map<String, FilesystemGroup> filesystemGroups = new HashMap<String, FilesystemGroup>(5);

    @ConfigField(name = "storageDescription")
    private String description;

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }


    public Map<String, FilesystemGroup> getFilesystemGroups() {
        return filesystemGroups;
    }

    public void setFilesystemGroups(Map<String, FilesystemGroup> filesystemGroups) {
        this.filesystemGroups = filesystemGroups;
    }

    public String getDescription() {
        return description;
    }


    public void setDescription(String d) {
        this.description = d;
    }

    public void addFilesystemGroup(FilesystemGroup group) {
        filesystemGroups.put(group.getId(), group);
    }
    public FilesystemGroup getFilesystemGroup(String groupID) {
        return filesystemGroups.get(groupID);
    }
    public boolean removeFilesystemGroupCfg(String groupID) {
        return filesystemGroups.remove(groupID) != null;
    }

    /**
     * Get the best accessible Filesystem of a filesystem group.
     * i.e.: Writable Filesystem with best availability and highest priority.
     * @param groupID
     * @return
     * @throws ConfigurationException
     */
    public Filesystem getWritableFilesystem(String groupID) throws ConfigurationException {
        FilesystemGroup grp = getFilesystemGroup(groupID);
        if (grp == null)
            throw new ConfigurationException("Filesystem group "+groupID+" is not configured!");
        Filesystem fs = null;
        int avail = Availability.OFFLINE.ordinal();
        int prio = Integer.MIN_VALUE;
        for ( Filesystem fsTmp : grp.getFilesystems().values() ) {
            log.debug("Filesystem:{} current writable filesystem:", fsTmp, fs);
            if (fsTmp.isWritable() && ( fsTmp.getAvailability().ordinal() < avail || (fsTmp.getAvailability().ordinal() == avail && fsTmp.getPriority() > prio)) ) {
                fs = fsTmp;
                avail = fs.getAvailability().ordinal();
                prio = fs.getPriority();
            }
        }
        return fs;
    }

    /**
     * Get a list of Filesystem objects that are writable and with an Availability better than OFFLINE
     * 
     * @param groupID
     * @return List of writable Filesystem objects.
     * @throws ConfigurationException
     */
    public List<Filesystem> getWritableFilesystems(String groupID) throws ConfigurationException {
        FilesystemGroup grp = getFilesystemGroup(groupID);
        if (grp == null)
            throw new ConfigurationException("Filesystem group "+groupID+" is not configured!");
        ArrayList<Filesystem> fsList = new ArrayList<Filesystem>();
        for ( Filesystem fsTmp : grp.getFilesystems().values() ) {
            if (fsTmp.isWritable() && fsTmp.getAvailability().ordinal() < Availability.OFFLINE.ordinal()) {
                fsList.add(fsTmp);
            }
        }
        return fsList;
    }
    
    @Override
    public void reconfigure(DeviceExtension from) {
        StorageConfiguration src = (StorageConfiguration) from;
        ReflectiveConfig.reconfigure(src, this);
    }

}

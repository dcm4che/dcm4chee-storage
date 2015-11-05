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

import org.dcm4che3.conf.core.api.ConfigurableClass;
import org.dcm4che3.conf.core.api.ConfigurableProperty;
import org.dcm4che3.conf.core.api.ConfigurationException;
import org.dcm4che3.conf.core.api.LDAP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 */

@LDAP(objectClasses = "dcm4cheeFilesystem")
@ConfigurableClass
public class Filesystem implements Serializable {


    private static final long serialVersionUID = -8258532093950989486L;
    private static Logger log = LoggerFactory.getLogger(Filesystem.class);

    public Filesystem() {
    }

    public Filesystem(String id, String uri, int priority, StorageAvailability availability) {
        this(id, uri, priority, true, true, availability);
    }

    public Filesystem(String id, String uri, int priority, boolean read, boolean write, StorageAvailability availability) {
        this.setId(id);
        this.setUri(uri);
        this.priority = priority;
        this.readable = read;
        this.writable = write;
        this.setAvailability(availability);
    }

    @ConfigurableProperty(name = "storageFileSystemID",
            label = "ID"
    )
    private String id;

    @ConfigurableProperty(name = "storageNextFileSystemReference",
            label = "Next file system ID",
            description = "ID of the file system to switch to if the current one cannot be used anymore (for example full or unavailable)",
            order = 1)
    private String nextFilesystemReference;

    @ConfigurableProperty(name = "storageFileSystemAvailability",
            label = "Availability",
            order = 2
    )
    private StorageAvailability availability;

    @ConfigurableProperty(name = "storageFileSystemPriority",
            label = "Priority",
            description = "If more than one file system is used, this value determines usage priority. Higher value takes precedence over lower values.",
            order = 3)
    private int priority;

    @ConfigurableProperty(name = "storageFileSystemURI",
            label = "URI",
            description = "How to physically access this filesystem",
            order = 4
    )
    private String uri;

    @ConfigurableProperty(name = "storageFileSystemReadable",
            label = "Readable",
            order = 5
    )
    private boolean readable;

    @ConfigurableProperty(name = "storageFileSystemWritable",
            label = "Writable",
            order = 6

    )
    private boolean writable;

    private Filesystem nextFilesystem;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (id == null)
            throw new IllegalArgumentException("Filesystem ID must not be null");
        this.id = id;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        if (uri == null)
            throw new IllegalArgumentException("Filesystem URI must not be null");
        this.uri = uri;
    }

    public Path getPath() {
        try {
            return Paths.get(new URI(uri));
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public StorageAvailability getAvailability() {
        return availability;
    }

    public void setAvailability(StorageAvailability availability) {
        if (availability == null)
            throw new IllegalArgumentException("Availability must not be null");
        this.availability = availability;
    }

    public boolean isReadable() {
        return readable;
    }

    public void setReadable(boolean readable) {
        this.readable = readable;
    }

    public boolean isWritable() {
        return writable;
    }

    public void setWritable(boolean writable) {
        this.writable = writable;
    }

    public void setNextFilesystem(Filesystem fs) throws ConfigurationException {
        setNextFilesystemReference(fs == null ? null : fs.getId());
        nextFilesystem = fs;
    }

    void initNextFilesystem(FilesystemGroup grp) {
        if (nextFilesystemReference != null) {
            nextFilesystem = grp.getFilesystem(nextFilesystemReference);
            if (nextFilesystem == null)
                log.warn("Next Filesystem {} not configured in group {}!", nextFilesystemReference, grp.getId());
        } else {
            nextFilesystem = null;
        }
    }

    public Filesystem getNextFilesystem() {
        return nextFilesystem;
    }

    public String getNextFilesystemReference() {
        return nextFilesystemReference;
    }

    public void setNextFilesystemReference(String next) throws ConfigurationException {
        if (next != null && next.equals(this.id)) {
            throw new ConfigurationException("Next filesystem must not reference itself!");
        }
        this.nextFilesystemReference = next;
    }

    @Override
    public String toString() {
        return "FileSystem[id=" + id
                + ", uri=" + uri
                + ", avail=" + availability
                + ", priority=" + priority
                + ", readable=" + readable
                + ", writable=" + writable
                + ", next=" + nextFilesystemReference
                + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Filesystem))
            return false;
        return ((Filesystem) o).id.equals(id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

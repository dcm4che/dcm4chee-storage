/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contentsOfthis file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copyOfthe License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is partOfdcm4che, an implementationOfDICOM(TM) in
 * Java(TM), hosted at https://github.com/gunterze/dcm4che.
 *
 * The Initial DeveloperOfthe Original Code is
 * Agfa Healthcare.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See @authors listed below
 *
 * Alternatively, the contentsOfthis file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisionsOfthe GPL or the LGPL are applicable instead
 * of those above. If you wish to allow useOfyour versionOfthis file only
 * under the termsOfeither the GPL or the LGPL, and not to allow others to
 * use your versionOfthis file under the termsOfthe MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your versionOfthis file under
 * the termsOfany oneOfthe MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.dcm4chee.storage.conf;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.dcm4che3.conf.api.ConfigurationNotFoundException;
import org.dcm4che3.conf.api.DicomConfiguration;
import org.dcm4che3.conf.core.api.ConfigurationException;
import org.dcm4che3.data.Code;
import org.dcm4che3.data.Issuer;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.DeviceExtension;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class StorageConfigTestBase {

    private static final String NO_MOUNT = "NO_MOUNT";
    protected static final String DEFAULT_STORAGE_DEVICE = "dcm4chee-storage";
    protected static final String DEFAULT_STORAGE_APPNAME = "dcm4chee-store";

    private static final String ONLINE_STORAGE = "ONLINE_STORAGE";
    private static final String NEARLINE_STORAGE = "NEARLINE_STORAGE";
    private static final String TEST_STORAGE = "TEST_STORE";

    private static final Issuer SITE_A = new Issuer("STORAGE_A", "1.2.40.0.13.1.1.999.222.1111", "ISO");
    private static final Code INST_A = new Code("111.1111", "99DCM4CHEE", null, "Site A");

    protected static int testCount = 0;
    protected static String testDeviceName;
    public DicomConfiguration config;

    static Set<String> createdDevices = new HashSet<String>();

    @After
    public void tearDown() throws Exception {
        cleanUp();
    }

    protected void cleanUp() throws Exception {
        if (config == null || testCount == 0)
            return;

        // clean up created devices
        for (String dName : createdDevices) {
            try {
                config.removeDevice(dName);
            } catch (ConfigurationNotFoundException e) {
            }
        }
        createdDevices.clear();

    }

    public Device addDeviceWithExtensionAndPersist(DeviceExtension extension, String deviceName) throws Exception {

        String dName = "testDevice_" + extension.getClass().getSimpleName();
        int i = 0;
        while (createdDevices.contains(dName + i))
            i++;

        createdDevices.add(dName + i);

        Device srcd = createDevice(dName + i, SITE_A, INST_A);

        srcd.addDeviceExtension(extension);
        config.persist(srcd);
        return srcd;

    }


    public <T extends DeviceExtension> T loadConfigAndAssertEquals(String devicename, Class<T> configClass, T configToCompare)
            throws ConfigurationException {

        Device loadedDevice = config.findDevice(devicename);
        T loaded = loadedDevice.getDeviceExtension(configClass);

        boolean eq = DeepEquals.deepEquals(configToCompare, loaded);
        Assert.assertTrue("Root class: " + configClass.getSimpleName() + "\n" + DeepEquals.getLastPair(), eq);

        return loaded;
    }

    @Test
    public void testOnlineStorage() throws Exception {
        Device d = createDevice("storage", SITE_A, INST_A);
        createdDevices.add(d.getDeviceName());    
        StorageConfiguration cfg = new StorageConfiguration();
        FilesystemGroup groupCfg = new FilesystemGroup(ONLINE_STORAGE,"10%");
        cfg.setApplicationName(ONLINE_STORAGE);
        groupCfg.setMountFailedCheckFile(NO_MOUNT);
        cfg.addFilesystemGroup(groupCfg);
        d.addDeviceExtension(cfg);
        config.persist(d);
        afterPersist();

        Device device = config.findDevice("storage");
        StorageConfiguration cfgStored = device.getDeviceExtension(StorageConfiguration.class);
        boolean eq = DeepEquals.deepEquals(cfg, cfgStored);

        Assert.assertTrue("Root: StorageConfiguration \n" + DeepEquals.getLastPair(), eq);

    }

    @Test
    public void testNearlineSTorage() throws Exception {
        Device d = createDevice("storage", SITE_A, INST_A);
        createdDevices.add(d.getDeviceName());        
        StorageConfiguration cfg = new StorageConfiguration();
        d.addDeviceExtension(cfg);
        cfg.setApplicationName(NEARLINE_STORAGE);
        FilesystemGroup groupCfg = new FilesystemGroup(NEARLINE_STORAGE,"100MB");
        cfg.addFilesystemGroup(groupCfg);
        config.persist(d);
        afterPersist();

        // assert
        loadConfigAndAssertEquals("storage", StorageConfiguration.class, cfg);
    }


    @Test
    public void testOnlineStorageWithFilesystem() throws Exception {
        Device d = createDevice("storage", SITE_A, INST_A);
        createdDevices.add(d.getDeviceName());    
        StorageConfiguration cfg = new StorageConfiguration();
        FilesystemGroup groupCfg = new FilesystemGroup(ONLINE_STORAGE,"10%");
        cfg.setApplicationName(ONLINE_STORAGE);
        groupCfg.setMountFailedCheckFile(NO_MOUNT);
        cfg.addFilesystemGroup(groupCfg);
        Filesystem fs = new Filesystem("online_1", "file:///storage/online/fs1", 1, StorageAvailability.ONLINE);
        groupCfg.addFilesystem(fs);
        d.addDeviceExtension(cfg);
        config.persist(d);
        afterPersist();

        Device device = config.findDevice("storage");
        StorageConfiguration cfgStored = device.getDeviceExtension(StorageConfiguration.class);
        boolean eq = DeepEquals.deepEquals(cfg, cfgStored);


        Assert.assertTrue("Root: StorageConfiguration \n" + DeepEquals.getLastPair(), eq);
    }

    @Test
    public void testModify() throws Exception {
        Device d = createDevice("storage_modify", SITE_A, INST_A);
        createdDevices.add(d.getDeviceName());

        StorageConfiguration cfg = new StorageConfiguration();
        d.addDeviceExtension(cfg);

        cfg.setApplicationName(TEST_STORAGE);

        Set<Device> cdevices = new HashSet<Device>();
        cdevices.add(d);

        config.persist(d);

        // assert loaded

        loadConfigAndAssertEquals("storage_modify", StorageConfiguration.class, cfg);

        // modify and merge

        FilesystemGroup groupCfg = new FilesystemGroup();
        groupCfg.setId("MODIFIED");
        groupCfg.setMountFailedCheckFile("NOT_MOUNTED");
        groupCfg.setMinFreeDiskSpace("100MB");
        cfg.addFilesystemGroup(groupCfg);
        config.merge(d);
        afterPersist();

        loadConfigAndAssertEquals("storage_modify", StorageConfiguration.class, cfg);


    }

    @Test
    public void testGetWritableFilesystem() throws Exception {
        Device d = createDevice("storage_writableFS", SITE_A, INST_A);
        createdDevices.add(d.getDeviceName());

        StorageConfiguration cfg = new StorageConfiguration();
        FilesystemGroup onlineCfg = new FilesystemGroup(ONLINE_STORAGE, "10%");
        Filesystem fs1 = new Filesystem("online_1", "file:///storage/online/fs1", 1, StorageAvailability.ONLINE);
        Filesystem fs2 = new Filesystem("online_2", "file:///storage/online/fs2", 2, StorageAvailability.ONLINE);
        Filesystem fs3 = new Filesystem("online_3", "file:///storage/online/fs3", 99, StorageAvailability.ONLINE);
        Filesystem fs4 = new Filesystem("online_4", "file:///storage/online/fs4", 999, StorageAvailability.NEARLINE);
        fs1.setNextFilesystem(fs2);
        fs2.setNextFilesystem(fs1);
        onlineCfg.addFilesystem(fs1);
        onlineCfg.addFilesystem(fs2);
        cfg.addFilesystemGroup(onlineCfg);
        cfg.setApplicationName(TEST_STORAGE);
        d.addDeviceExtension(cfg);
        config.persist(d);
        checkWritableFS(d, fs2);

        onlineCfg.addFilesystem(fs3);
        config.merge(d);
        checkWritableFS(d, fs3);

        onlineCfg.addFilesystem(fs4);
        config.merge(d);
        checkWritableFS(d, fs3);

        onlineCfg.removeFilesystem(fs3);
        config.merge(d);
        checkWritableFS(d, fs2);

        onlineCfg.removeFilesystem(fs2);
        config.merge(d);
        checkWritableFS(d, fs1);

        onlineCfg.removeFilesystem(fs1);
        config.merge(d);
        checkWritableFS(d, fs4);

        onlineCfg.addFilesystem(fs3);
        fs4.setAvailability(StorageAvailability.ONLINE);
        config.merge(d);
        checkWritableFS(d, fs4);

        fs4.setWritable(false);
        config.merge(d);
        checkWritableFS(d, fs3);

        fs3.setWritable(false);
        config.merge(d);
        checkWritableFS(d, null);

        onlineCfg.removeFilesystem(fs3);
        onlineCfg.removeFilesystem(fs4);
        config.merge(d);
        checkWritableFS(d, null);
    }

    private void checkWritableFS(Device d, Filesystem expected) throws ConfigurationException {
        Device device = config.findDevice(d.getDeviceName());
        System.out.println("#################### device:"+device);
        StorageConfiguration storage = device.getDeviceExtension(StorageConfiguration.class);
        System.out.println("#################### storage:"+storage);
        Filesystem fs = storage.getWritableFilesystem(ONLINE_STORAGE);
        System.out.println("#################### fs:"+fs);
        assertEquals("Writable filesystem choosen from "+storage.getFilesystemGroup(ONLINE_STORAGE).getFilesystems().values(), expected, fs);
        if (fs != null && fs.getNextFilesystemReference() != null) {
            Filesystem next = fs.getNextFilesystem();
            Assert.assertNotNull("Next Filesystem object for reference is null", next);
        }
    }

    @Test
    public void testDefaultConfig() throws Exception {

        // if default config device is already in the config - leave it
        // untouched, cancel test
        try {
            if (config.findDevice(DEFAULT_STORAGE_DEVICE) != null)
                return;
        } catch (Exception ignored) {
        }

        Device d = createDevice(DEFAULT_STORAGE_DEVICE, SITE_A, INST_A);
        if (!Boolean.getBoolean("keep"))
            createdDevices.add(DEFAULT_STORAGE_DEVICE);

        StorageConfiguration cfg = new StorageConfiguration();
        FilesystemGroup onlineCfg = new FilesystemGroup(ONLINE_STORAGE,"10%");
        Filesystem fs1 = new Filesystem("online_1", "file:///storage/online/fs1", 1, StorageAvailability.ONLINE);
        Filesystem fs2 = new Filesystem("online_2", "file:///storage/online/fs2", 2, StorageAvailability.ONLINE);
        fs1.setNextFilesystem(fs2);
        fs2.setNextFilesystem(fs1);
        onlineCfg.addFilesystem(fs1);
        onlineCfg.addFilesystem(fs2);
        cfg.addFilesystemGroup(onlineCfg);
        FilesystemGroup nearlineCfg = new FilesystemGroup(NEARLINE_STORAGE,"100MB");
        Filesystem fs3 = new Filesystem("nearline_1", "file:///storage/nearline/fs1", 1, true, false, StorageAvailability.NEARLINE);
        nearlineCfg.addFilesystem(fs3);
        cfg.addFilesystemGroup(nearlineCfg);
        cfg.setApplicationName(DEFAULT_STORAGE_APPNAME);
        d.addDeviceExtension(cfg);
        config.persist(d);

        StorageConfiguration store = loadConfigAndAssertEquals(DEFAULT_STORAGE_DEVICE, StorageConfiguration.class, cfg);

        FilesystemGroup fsg1 = store.getFilesystemGroup(ONLINE_STORAGE);
        boolean eq = DeepEquals.deepEquals(fsg1, onlineCfg);
        Assert.assertTrue("ONLINE group: Root class: " + onlineCfg.getClass().getSimpleName() + "\n" + DeepEquals.getLastPair(), eq);

        FilesystemGroup fsg2 = store.getFilesystemGroup(NEARLINE_STORAGE);
        eq = DeepEquals.deepEquals(fsg2, nearlineCfg);
        Assert.assertTrue("NEARLINE group: Root class: " + nearlineCfg.getClass().getSimpleName() + "\n" + DeepEquals.getLastPair(), eq);
    }

    /*_*/

    public void afterPersist() throws Exception {
    }

    private Device init(Device device, Issuer issuer, Code institutionCode) throws Exception {
        device.setIssuerOfPatientID(issuer);
        device.setIssuerOfAccessionNumber(issuer);
        if (institutionCode != null) {
            device.setInstitutionNames(institutionCode.getCodeMeaning());
            device.setInstitutionCodes(institutionCode);
        }
        return device;
    }

    public Device createDevice(String name, Issuer issuer, Code institutionCode) throws Exception {
        testDeviceName = name;
        Device device = new Device(name);
        init(device, issuer, institutionCode);
        return device;
    }

}

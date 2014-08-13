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
 * Portions created by the Initial Developer are Copyright (C) 2011-2014
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.prefs.Preferences;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.dcm4che3.conf.api.ConfigurationException;
import org.dcm4che3.conf.api.DicomConfiguration;
import org.dcm4che3.conf.ldap.LdapDicomConfiguration;
import org.dcm4che3.conf.ldap.generic.LdapGenericConfigExtension;
import org.dcm4che3.conf.prefs.PreferencesDicomConfiguration;
import org.dcm4che3.conf.prefs.cdi.PrefsFactory;
import org.dcm4che3.conf.prefs.generic.PreferencesGenericConfigExtension;
import org.dcm4che3.util.StreamUtils;
import org.dcm4che3.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 * 
 */
public class StorageConfigurationFactory {

    private static final String LDAP_PROPERTIES_PROPERTY = "org.dcm4chee.storage.ldapPropertiesURL";

    public static final Logger log = LoggerFactory.getLogger(StorageConfigurationFactory.class);

    
    /**
     * Allows for custom Preferences implementations to be used, like jdbc-prefs
     */
    @Inject
    Instance<PrefsFactory> prefsFactoryInstance;
    
    @Produces
    @Storage
    @ApplicationScoped
    public DicomConfiguration createDicomConfiguration() throws ConfigurationException {
        return System.getProperty(LDAP_PROPERTIES_PROPERTY) != null ? getLdapConfiguration() : getPrefsConfiguration();
    }

    protected DicomConfiguration getLdapConfiguration() throws ConfigurationException {
        LdapDicomConfiguration conf = new LdapDicomConfiguration(ldapEnv());
        conf.addDicomConfigurationExtension(new LdapGenericConfigExtension<StorageConfiguration>(StorageConfiguration.class));
        return conf;
    }

    protected DicomConfiguration getPrefsConfiguration() throws ConfigurationException {
        
        PreferencesDicomConfiguration conf; 
        
        // check if there is an implementation of PrefsFactory provided and construct DicomConfiguration accordingly
        if (!prefsFactoryInstance.isUnsatisfied()) {
            Preferences prefs = prefsFactoryInstance.get().getPreferences();
            log.info("Using custom Preferences implementation {}", prefs.getClass().toString());
            conf = new PreferencesDicomConfiguration(prefs); 
        } else
            conf = new PreferencesDicomConfiguration();
        
        conf.addDicomConfigurationExtension(new PreferencesGenericConfigExtension<StorageConfiguration>(StorageConfiguration.class));
        return conf;
    }

    public void disposeDicomConfiguration(@Disposes @Storage DicomConfiguration conf) {
        conf.close();
    }
    
    private Properties ldapEnv() throws ConfigurationException {
        String url = System.getProperty(LDAP_PROPERTIES_PROPERTY);
        url = StringUtils.replaceSystemProperties(url);
        Properties p = new Properties();
        try (InputStream in = StreamUtils.openFileOrURL(url);) {
            p.load(in);
        } catch (IOException e) {
            throw new ConfigurationException(e);
        }
        return p;
    }

}

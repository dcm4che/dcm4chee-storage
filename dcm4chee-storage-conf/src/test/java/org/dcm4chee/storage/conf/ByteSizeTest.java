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

import org.junit.Test;


public class ByteSizeTest {
    private static final long KILO = 1000;
    private static final long MEGA = KILO*KILO;
    private static final long GIGA = MEGA*KILO;
    private static final long TERA = GIGA*KILO;
    private static final long PETA = TERA*KILO;
    private static final long EXA = PETA*KILO;

    private static final long KIBI = 1024;
    private static final long MEBI = KIBI*KIBI;
    private static final long GIBI = MEBI*KIBI;
    private static final long TEBI = GIBI*KIBI;
    private static final long PEBI = TEBI*KIBI;
    private static final long EXABI = PEBI*KIBI;

    @Test
    public void testDecimal() {
        assertEquals("100B", 100, new ByteSize("100B").getSize());
        assertEquals("5565342", 5565342, new ByteSize("5565342").getSize());
        assertEquals("25kB", 25*KILO, new ByteSize("25kB").getSize());
        assertEquals("12MB", 12*MEGA, new ByteSize("12MB").getSize());
        assertEquals("2GB", 2*GIGA, new ByteSize("2GB").getSize());
        assertEquals("212TB", 212*TERA, new ByteSize("212TB").getSize());
        assertEquals("20PB", 20*PETA, new ByteSize("20PB").getSize());
        assertEquals("1EB", EXA, new ByteSize("1EB").getSize());
    }
    
    @Test
    public void testBinary() {
        assertEquals("25KiB", 25*KIBI, new ByteSize("25KiB").getSize());
        assertEquals("12MiB", 12*MEBI, new ByteSize("12MiB").getSize());
        assertEquals("2GiB", 2*GIBI, new ByteSize("2GiB").getSize());
        assertEquals("212TiB", 212*TEBI, new ByteSize("212TiB").getSize());
        assertEquals("20PiB", 20*PEBI, new ByteSize("20PiB").getSize());
        assertEquals("1EiB", EXABI, new ByteSize("1EiB").getSize());
    }

    @Test
    public void testRelative() {
        assertEquals("25%", 25, new ByteSize("25%").getSize(100));
        assertEquals("12%", 24000, new ByteSize("12%").getSize(200000));
    }
    
    @Test
    public void testFormatBinary() {
        assertEquals("1023", "1023B", ByteSize.toString(1023, true, true));
        assertEquals("4096", "4KiB", ByteSize.toString(4096, true, true));
        assertEquals("5000", "4KiB", ByteSize.toString(5000, true, true));
        assertEquals("5000", "5000B", ByteSize.toString(5000, true, false));
    }
    @Test
    public void testFormatDecimal() {
        assertEquals("999", "999B", ByteSize.toString(999, false, true));
        assertEquals("4000", "4kB", ByteSize.toString(4000, false, true));
        assertEquals("5010", "5kB", ByteSize.toString(5010, false, true));
        assertEquals("5010", "5010B", ByteSize.toString(5010, false, false));
    }
}

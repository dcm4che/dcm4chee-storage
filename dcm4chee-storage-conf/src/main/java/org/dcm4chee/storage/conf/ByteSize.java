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

import java.io.Serializable;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 */

public class ByteSize implements Serializable {

    private static final long serialVersionUID = -8258532093950989486L;

    private static final char[] FACTOR_PREFIX = {'K','M','G','T','P','E'};
    private static final int K_BINARY = 1024;
    private static final int K_DECIMAL = 1000;

    private Number value;
    private boolean binary;
    
    public ByteSize(String cfg) {
        int pos = cfg.length()-1;
        char c = cfg.charAt(pos);
        if (c == '%') {
            value = new Integer(cfg.substring(0, pos));
        } else if (c == 'B') {
            if (cfg.charAt(--pos) == 'i') {
                value = toNrOfBytes(cfg, --pos, K_BINARY);
            } else {
                value = toNrOfBytes(cfg, pos, K_DECIMAL);
            }
        } else {
            value = new Long(cfg);
        }
    }

    private Long toNrOfBytes(String s, int pos, int base) {
        long f = 1l;
        switch (s.charAt(pos)) {
        case 'E':
            f *= base;
        case 'P':
            f *= base;
        case 'T':
            f *= base;
        case 'G':
            f *= base;
        case 'M':
            f *= base;
        case 'K': case 'k':
            f *= base;
            break;
        default:
            pos++;
        }
        return f * Long.parseLong(s.substring(0,pos));
    }
    
    public boolean isRelative() {
        return value.getClass() == Integer.class;
    }

    public long getSize(long... absolute) {
        if (isRelative()) {
            if (absolute.length == 0)
                throw new IllegalArgumentException("SpaceSize is relative! Need absolute argument to calculate size!");
            return absolute[0] * value.intValue() / 100;
        }
        return value.longValue();
    }
    
    public static String toString(long value, boolean binary, boolean roundDown) {
        int f = binary ? K_BINARY :K_DECIMAL;
        int idx = -1;
        while (value > f && (roundDown || value % f == 0)) {
            value /= f;
            idx++;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(value);
        if (idx == 0 && !binary) {
            sb.append("k");
        } else if (idx != -1) {
            sb.append(FACTOR_PREFIX[idx]);
            if (binary)
                sb.append('i');
        }
        sb.append('B');
        return sb.toString();
    }
    
    @Override
    public String toString() {
        if (this.isRelative())
            return value+"%";
        else
            return toString(value.longValue(), binary, true);
    }
    
    @Override
    public int hashCode() {
        return value.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (o == null || o.getClass() != this.getClass())
            return false;
        return value.equals(((ByteSize) o).value);
    }
}
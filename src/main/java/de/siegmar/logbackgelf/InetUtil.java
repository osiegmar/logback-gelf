/*
 * Logback GELF - zero dependencies Logback GELF appender library.
 * Copyright (C) 2019 Oliver Siegmar
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package de.siegmar.logbackgelf;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.LinkedHashSet;

final class InetUtil {

    private InetUtil() {
    }

    /**
     * Set this system property to a simple hostname to use a fixed host name.
     */
    public static final String PROPERTY_LOGSTASH_GELF_HOSTNAME = "logstash-gelf.hostname";

    static String sysEnv (String envVarName){
        try {
            String value = System.getProperty(envVarName);
            if (value != null && !value.isEmpty()){
                return value;
            }
        } catch (Throwable ignore){
        }
        try {
            return System.getenv(envVarName);
        } catch (Exception ignore){
        }
        return null;
    }

    static String trim (String str){
        return str == null ? ""
            : str.trim();
    }

    /**
     * Retrieves the local host's name. Preferably the fully qualified one.
     *
     * @return the (fully qualified) name of local host or the IP address if not resolvable.
     * @throws UnknownHostException if the hostname could not be resolved due to a system
     *                              misconfiguration.
     */
    static String getLocalHostName() throws UnknownHostException {
        String definedHostName = trim(sysEnv(PROPERTY_LOGSTASH_GELF_HOSTNAME));
        if (!definedHostName.isEmpty()){
            return definedHostName;// explicitly specified by user or already cached
        }

        LinkedHashSet<String> names = new LinkedHashSet<>(31);
        LinkedHashSet<String> namesLoCase = new LinkedHashSet<>(31);
        {
            String s = trim(sysEnv("COMPUTERNAME"));
            namesLoCase.add(s.toLowerCase());
            names.add(s);

            s = trim(sysEnv("HOSTNAME"));
            if (namesLoCase.add(s.toLowerCase())){
                names.add(s);
            }
            s = trim(sysEnv("NAME"));
            if (namesLoCase.add(s.toLowerCase())){
                names.add(s);
            }
        }
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();

            collectInetAddrProperties(inetAddress, namesLoCase, names);
        } catch (Exception e){
            System.err.println("getLocalHostName: failed to get InetAddress.getLocalHost()/getHostName(): "+ e);
        }

        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

            while (networkInterfaces.hasMoreElements()){
                NetworkInterface ni = networkInterfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()){
                    continue;
                }
                if (trim(ni.getDisplayName()).contains("Virtual")){
                    continue; // Microsoft Virtual
                }

                Enumeration<InetAddress> ias = ni.getInetAddresses();
                while (ias.hasMoreElements()){
                    InetAddress inetAddress = ias.nextElement();

                    collectInetAddrProperties(inetAddress, namesLoCase, names);
                }
            }

        } catch (Exception e){
            System.err.println("getLocalHostName: failed to get NetworkInterface.getNetworkInterfaces(): "+ e);
        }

        names.removeIf(s -> {
            if (s.isEmpty()){ return true; }
            String z = s.toLowerCase();
            return z.contains("localhost") || z.contains("docker") || z.contains("mshome.net");
        });
        if (names.isEmpty()){
            return "unknown"; // UUID?
        }

        final String hostName = String.join("|", names);
        System.setProperty(PROPERTY_LOGSTASH_GELF_HOSTNAME, hostName);
        return hostName;
    }

    private static void collectInetAddrProperties (InetAddress inetAddress, LinkedHashSet<String> namesLoCase, LinkedHashSet<String> names) {
        if (inetAddress.isLoopbackAddress()){
            return;
        }

        String s = trim(inetAddress.getCanonicalHostName());
        String z = s.toLowerCase();
        if (namesLoCase.add(z)){
            names.add(s);// e.g. host.docker.internal
        }
        s = trim(inetAddress.getHostName());
        z = s.toLowerCase();
        if (namesLoCase.add(s.toLowerCase())){
            names.add(s); // e.g. K39
        }
        //s = trim(inetAddress.getHostAddress()); if (namesLoCase.add(s.toLowerCase())){ names.add(s); // e.g. 10.3.104.16 }
    }


}
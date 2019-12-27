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

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Arrays;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

@SuppressWarnings({"checkstyle:classdataabstractioncoupling", "checkstyle:classfanoutcomplexity"})
public class X509Util {

    private static final String ALGORITHM = "RSA";
    private static final String SIG_ALGORITHM = "SHA256withRSA";
    private static final int KEY_SIZE = 1024;
    private static final String CA_NAME = "CN=MyCA";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        final KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
        keyGen.initialize(KEY_SIZE);
        return keyGen.genKeyPair();
    }

    static class CABuilder {

        private final KeyPair keyPair;
        private final X509Certificate caCertificate;

        CABuilder() throws NoSuchAlgorithmException,
            CertificateException, CertIOException, OperatorCreationException {

            keyPair = generateKeyPair();
            caCertificate = build();
        }

        KeyPair getKeyPair() {
            return keyPair;
        }

        X509Certificate getCaCertificate() {
            return caCertificate;
        }

        private X509Certificate build() throws NoSuchAlgorithmException,
            CertIOException, OperatorCreationException, CertificateException {

            final X500Principal issuer = new X500Principal("CN=MyCA");
            final BigInteger sn = new BigInteger(64, new SecureRandom());
            final Date from = Date.valueOf(LocalDate.now());
            final Date to = Date.valueOf(LocalDate.now().plusYears(1));
            final X509v3CertificateBuilder v3CertGen =
                new JcaX509v3CertificateBuilder(issuer, sn, from, to, issuer, keyPair.getPublic());
            final JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
            v3CertGen.addExtension(Extension.authorityKeyIdentifier, false,
                extUtils.createAuthorityKeyIdentifier(keyPair.getPublic()));
            v3CertGen.addExtension(Extension.subjectKeyIdentifier, false,
                extUtils.createSubjectKeyIdentifier(keyPair.getPublic()));
            v3CertGen.addExtension(Extension.basicConstraints, true,
                new BasicConstraints(0));
            v3CertGen.addExtension(Extension.keyUsage, true,
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyCertSign | KeyUsage.cRLSign));
            final ContentSigner signer = new JcaContentSignerBuilder(SIG_ALGORITHM)
                .build(keyPair.getPrivate());
            return new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(v3CertGen.build(signer));
        }

    }

    static class CertBuilder {

        private final KeyPair keyPair;
        private LocalDate validFrom = LocalDate.now();
        private LocalDate validTo = LocalDate.now().plusYears(1);
        private PrivateKey caPrivateKey;
        private X509Certificate caCertificate;

        CertBuilder(final KeyPair keyPair) {
            this.keyPair = keyPair;
        }

        CertBuilder() throws NoSuchAlgorithmException {
            this(generateKeyPair());
        }

        CertBuilder validFrom(final LocalDate date) {
            this.validFrom = date;
            return this;
        }

        CertBuilder validTo(final LocalDate date) {
            this.validTo = date;
            return this;
        }

        CertBuilder caPrivateKey(final PrivateKey key) {
            this.caPrivateKey = key;
            return this;
        }

        CertBuilder caCertificate(final X509Certificate cert) {
            this.caCertificate = cert;
            return this;
        }

        X509Certificate build(final String commonName, final String... subjectAltName)
            throws IOException, OperatorCreationException, CertificateException,
            NoSuchAlgorithmException {

            final AlgorithmIdentifier sigAlgId =
                new DefaultSignatureAlgorithmIdentifierFinder().find(SIG_ALGORITHM);
            final AlgorithmIdentifier digAlgId =
                new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
            final AsymmetricKeyParameter privateKeyAsymKeyParam =
                PrivateKeyFactory.createKey(keyPair.getPrivate().getEncoded());
            final SubjectPublicKeyInfo subPubKeyInfo =
                SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
            final ContentSigner sigGen;

            final X500Name issuer = new X500Name(CA_NAME);
            final X500NameBuilder x500NameBuilder = new X500NameBuilder();
            if (commonName != null) {
                x500NameBuilder.addRDN(BCStyle.CN, commonName);
            }
            x500NameBuilder.addRDN(BCStyle.O, "snakeoil");
            final X500Name name = x500NameBuilder.build();

            final Date from = Date.valueOf(validFrom);
            final Date to = Date.valueOf(validTo);
            final BigInteger sn = new BigInteger(64, new SecureRandom());
            final X509v3CertificateBuilder v3CertGen =
                new X509v3CertificateBuilder(issuer, sn, from, to, name, subPubKeyInfo);

            if (caCertificate != null) {
                sigGen = new JcaContentSignerBuilder(SIG_ALGORITHM).build(caPrivateKey);

                final JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
                v3CertGen.addExtension(Extension.authorityKeyIdentifier, false,
                    extUtils.createAuthorityKeyIdentifier(caCertificate));
            } else {
                sigGen = new BcRSAContentSignerBuilder(sigAlgId, digAlgId)
                    .build(privateKeyAsymKeyParam);
            }

            if (subjectAltName != null) {
                final GeneralName[] generalNames = Arrays.stream(subjectAltName)
                    .map(s -> new GeneralName(GeneralName.dNSName, s))
                    .toArray(GeneralName[]::new);

                v3CertGen.addExtension(Extension.subjectAlternativeName, false,
                    new GeneralNames(generalNames).getEncoded());
            }

            final X509CertificateHolder certificateHolder = v3CertGen.build(sigGen);
            return new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(certificateHolder);
        }

    }

}

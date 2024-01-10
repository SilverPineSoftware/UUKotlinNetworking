package com.silverpine.uu.networking

import com.silverpine.uu.core.uuToBase64Bytes
import com.silverpine.uu.logging.UULog
import java.io.ByteArrayInputStream
import java.security.KeyFactory
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory

open class UUClientCertSocketFactory
{
    companion object
    {
        private const val BEGIN_CERT = "-----BEGIN CERTIFICATE-----"
        private const val END_CERT = "-----END CERTIFICATE-----"
        private const val BEGIN_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----"
        private const val END_PRIVATE_KEY = "-----END PRIVATE KEY-----"
    }

    // Expects plain text cert in PEM form
    //
    // -----BEGIN CERTIFICATE-----
    // base64 bytes
    // -----END CERTIFICATE-----
    // -----BEGIN PRIVATE KEY-----
    //  base 64 bytes
    // -----END PRIVATE KEY-----
    var plainTextPemCert: String = ""

    fun getSocketFactory(): SSLSocketFactory?
    {
        val context = SSLContext.getInstance("TLS")

        val certPart = plainTextPemCert.uuTextBetween(BEGIN_CERT, END_CERT) ?: return null
        val privateKeyPart = plainTextPemCert.uuTextBetween(BEGIN_PRIVATE_KEY, END_PRIVATE_KEY) ?: return null

        val certBytes = certPart.uuToBase64Bytes() ?: return null
        val privateKeyBytes = privateKeyPart.uuToBase64Bytes() ?: return null

        val x509Certificate = getX509Cert(certBytes) ?: return null
        val key = getRSAPrivateKey(privateKeyBytes) ?: return null

        val keystore = KeyStore.getInstance(KeyStore.getDefaultType())
        val localStorePassword = "password".toCharArray()
        val localStoreAlias = "alias"

        keystore.load(null)
        keystore.setCertificateEntry(localStoreAlias, x509Certificate)
        keystore.setKeyEntry(localStoreAlias, key, localStorePassword, arrayOf<Certificate>(x509Certificate))

        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(keystore, localStorePassword)

        context.init(kmf.keyManagers, null, null)

        return context.socketFactory
    }

    private fun String.uuTextBetween(startMarker: String, endMarker: String): String?
    {
        val afterFirstMarker = split(startMarker).getOrNull(1) ?: return null
        return afterFirstMarker.split(endMarker).getOrNull(0)
    }

    private fun getRSAPrivateKey(input: ByteArray?): RSAPrivateKey?
    {
        try
        {
            val bytes = input ?: return null
            val spec = PKCS8EncodedKeySpec(bytes)
            val factory = KeyFactory.getInstance("RSA")
            return factory.generatePrivate(spec) as? RSAPrivateKey
        }
        catch (ex: Exception)
        {
            UULog.d(javaClass, "getRSAPrivateKey", "", ex)
            return null
        }
    }

    private fun getX509Cert(input: ByteArray?): X509Certificate?
    {
        try
        {
            val bytes = input ?: return null

            val factory = CertificateFactory.getInstance("X.509")
            return factory.generateCertificate(ByteArrayInputStream(bytes)) as? X509Certificate
        }
        catch (ex: Exception)
        {
            UULog.d(javaClass, "getX509Cert", "", ex)
            return null
        }
    }
}
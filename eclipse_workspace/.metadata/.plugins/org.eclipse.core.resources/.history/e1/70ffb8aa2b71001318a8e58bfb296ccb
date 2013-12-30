/*
 * Created on Dec 7, 2004
 */
/*
 * Copyright (c) 2004-2006 Gregor Heinrich. All rights reserved. Redistribution and
 * use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met: 1. Redistributions of source
 * code must retain the above copyright notice, this list of conditions and the
 * following disclaimer. 2. Redistributions in binary form must reproduce the
 * above copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESSED OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.knowceans.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Singleton class to decrypt and encrypt Strings using the symmetric DES 
 * algorithm.
 *  
 * @author heinrich
 *
 */
public class Crypt {

	private static Crypt instance;
	private Cipher enc = null;
	private Cipher dec = null;

	/**
	 * @return
	 */
	private static Crypt getInstance() {
		if (instance == null) {
			instance = new Crypt();
		}
		return instance;
	}
    
    protected Crypt() {}

	/**
	 * @return
	 */
	private static void init(SecretKey key) {
		Crypt c = getInstance();
		try {
			c.enc = Cipher.getInstance("DES");
			c.enc.init(Cipher.ENCRYPT_MODE, getKey());
			c.dec = Cipher.getInstance("DES");
			c.dec.init(Cipher.DECRYPT_MODE, getKey());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String encrypt(String str) {
		Crypt c = getInstance();
		try {
			if (c.enc == null) {
				c.enc = Cipher.getInstance("DES");
				c.enc.init(Cipher.ENCRYPT_MODE, getKey());
			}

			// Encode the string into bytes using utf-8
			byte[] utf8 = str.getBytes("UTF8");

			// Encrypt
			byte[] enc = c.enc.doFinal(utf8);

			// Encode bytes to base64 to get a string
			return new sun.misc.BASE64Encoder().encode(enc);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String decrypt(String str) {
		Crypt c = getInstance();
		try {
			if (c.dec == null) {
				c.dec = Cipher.getInstance("DES");
				c.dec.init(Cipher.DECRYPT_MODE, getKey());
			}

			// Decode base64 to get bytes
			byte[] dec = new sun.misc.BASE64Decoder().decodeBuffer(str);

			// Decrypt
			byte[] utf8 = c.dec.doFinal(dec);

			// Decode using utf-8
			return new String(utf8, "UTF8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static SecretKey getKey() {
		// Generate a temporary key. In practice, you would save this key.
		// See also e464 Encrypting with DES Using a Pass Phrase.
		//SecretKey key = KeyGenerator.getInstance("DES").generateKey();

		//KeyPairGenerator kpg = KeyPairGenerator.getInstance( "RSA" );

		// constant key is enough to secure local database passwords etc.
		SecretKey key = new SecretKeySpec("3.141593".getBytes(), "DES");
		return key;

	}

	public static void main(String[] args) {

		try {
            System.out.println("DES password encrypter.");

			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			String str = "";
			while (str != null) {
				System.out.print("Plain password (.x to exit): ");
				str = in.readLine();

				if (str.trim().equals(".x"))
					break;

				String encrypted = Crypt.encrypt(str.trim());
				System.err.println("encrypted: " + encrypted);

				String decrypted = Crypt.decrypt(encrypted);
				System.out.println("decrypted: " + decrypted);
			}
            System.out.println("Password encrypter finished.");

		} catch (Exception e) {
            e.printStackTrace();
        }
	}
}

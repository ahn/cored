package org.vaadin.cored;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class GravatarUtil {

	public static String gravatarUrlFor(User user, int size) {
		String email = user.getEmail();
		String hash = email!=null ? md5Hex(email) : md5Hex(user.getName());
		if (hash!=null) {
			return "http://www.gravatar.com/avatar/"+hash+"?d=monsterid&s="+size;
		}
		return "http://www.gravatar.com/avatar/?d=mm&s="+size;
	}

	// http://fi.gravatar.com/site/implement/images/java/
	private static String hex(byte[] array) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < array.length; ++i) {
			sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(
					1, 3));
		}
		return sb.toString();
	}

	// http://fi.gravatar.com/site/implement/images/java/
	private static String md5Hex(String message) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			return hex(md.digest(message.getBytes("CP1252")));
		} catch (NoSuchAlgorithmException e) {
		} catch (UnsupportedEncodingException e) {
		}
		return null;
	}
}

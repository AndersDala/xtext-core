/*******************************************************************************
 * Copyright (c) 2015 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.util;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.jar.Manifest;

import org.junit.Ignore;
import org.junit.Test;

/**
 * @author dhuebner - Initial contribution and API
 */
public class XtextVersionTests {

	@Test
	public void testVersionKinds() {

		XtextVersion version = new XtextVersion("2.9.0-SNAPSHOT");
		//snapshot
		assertTrue(version.isSnapshot());
		assertFalse(version.isStable());

		// stable version pre-release
		version = new XtextVersion("2.9.0.beta1");
		assertFalse(version.isSnapshot());
		assertTrue(version.isStable());
		
		version = new XtextVersion("2.9.0.rc1");
		assertFalse(version.isSnapshot());
		assertTrue(version.isStable());
		
		version = new XtextVersion("2.9.0.maybeSomthingElse");
		assertFalse(version.isSnapshot());
		assertTrue(version.isStable());
		
		// releases
		version = new XtextVersion("2.9.0");
		assertFalse(version.isSnapshot());
		assertFalse(version.isStable());
		
		version = new XtextVersion("2.9.0.1");
		assertFalse(version.isSnapshot());
		assertFalse(version.isStable());
		
	}
	
	@Test @Ignore
	public void test_getCurrent () throws IOException {
		XtextVersion current = XtextVersion.getCurrent();
		try (FileInputStream fis = new FileInputStream("META-INF/MANIFEST.MF")) {
			assertNotNull(current);
			Manifest manifest = new Manifest(fis);
			String bundleVersion = manifest.getMainAttributes().getValue("Bundle-Version");
			assertNotNull(current);
			// in development Bundle-Version will have suffix .qualifier
			if (bundleVersion.endsWith(".qualifier")) {
				assertEquals(bundleVersion.replace(".qualifier", "-SNAPSHOT"), current.getVersion());
			}
		}
	}

}

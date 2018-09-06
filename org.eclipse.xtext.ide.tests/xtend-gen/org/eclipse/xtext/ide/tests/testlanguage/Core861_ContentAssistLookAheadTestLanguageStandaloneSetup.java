/**
 * Copyright (c) 2016, 2017 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.xtext.ide.tests.testlanguage;

import org.eclipse.xtext.ide.tests.testlanguage.Core861_ContentAssistLookAheadTestLanguageStandaloneSetupGenerated;

/**
 * Initialization support for running Xtext languages without Equinox extension registry.
 */
@SuppressWarnings("all")
public class Core861_ContentAssistLookAheadTestLanguageStandaloneSetup extends Core861_ContentAssistLookAheadTestLanguageStandaloneSetupGenerated {
  public static void doSetup() {
    new Core861_ContentAssistLookAheadTestLanguageStandaloneSetup().createInjectorAndDoEMFRegistration();
  }
}

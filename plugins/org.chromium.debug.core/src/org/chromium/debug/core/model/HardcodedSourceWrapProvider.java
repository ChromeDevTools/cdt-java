// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.core.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.osgi.util.NLS;
import org.json.simple.JSONObject;

/**
 * Provides a set of standard known source wrappers (for example for Node.JS).
 */
public class HardcodedSourceWrapProvider implements IPredefinedSourceWrapProvider {
  @Override
  public Collection<Entry> getWrappers() {
    return entries;
  }

  private final List<Entry> entries =
      Arrays.<Entry>asList(new NodeJsStandardEntry(), new NodeJsWithDefinedEntry());

  private static abstract class NodeJsBase extends Entry {
    protected static final String SUFFIX = "\n});"; //$NON-NLS-1$

    private final String prefix;

    NodeJsBase(String id, String name, String prefix) {
      super(id, new SourceWrapSupport.StringBasedWrapper(name, prefix, SUFFIX));
      this.prefix = prefix;
    }

    @Override
    public String getHumanDescription() {
      JSONObject object = new JSONObject();
      object.put("prefix", prefix); //$NON-NLS-1$
      object.put("suffix", SUFFIX); //$NON-NLS-1$

      return NLS.bind(Messages.HardcodedSourceWrapProvider_DESCRIPTION, getSpecialization(),
          object.toJSONString());
    }

    protected abstract String getSpecialization();
  }

  private static class NodeJsStandardEntry extends NodeJsBase {
    private static final String NAME = Messages.HardcodedSourceWrapProvider_STANDARD;
    // As defined at https://github.com/joyent/node.git
    //     commit fc025f878a0b7a5bbb5810005da3e09cb856b773 Jan 31, 2010 (version 0.1.29)
    private static final String PREFIX =
        "(function (exports, require, module, __filename, __dirname) { "; //$NON-NLS-1$

    NodeJsStandardEntry() {
      super(NodeJsStandardEntry.class.getName(), NAME, PREFIX);
    }

    @Override
    protected String getSpecialization() {
      return Messages.HardcodedSourceWrapProvider_STANDARD_2;
    }
  }

  private static class NodeJsWithDefinedEntry extends NodeJsBase {
    private static final String NAME = Messages.HardcodedSourceWrapProvider_WITH_DEFINED_2;
    // As defined at https://github.com/joyent/node.git between commits:
    //     703a1ffe52b66972f38db19fb68e0f70c3dd2631 Jul 29, 2011 (version 0.5.3)
    //     9967c369c9272335bb0343558673b689725c6d7c Jun 12, 2011 (version 0.5.0)
    private static final String PREFIX =
        "(function (exports, require, module, __filename, __dirname, define) { "; //$NON-NLS-1$

    NodeJsWithDefinedEntry() {
      super(NodeJsWithDefinedEntry.class.getName(), NAME, PREFIX);
    }

    @Override
    protected String getSpecialization() {
      return Messages.HardcodedSourceWrapProvider_WITH_DEFINED;
    }
  }
}

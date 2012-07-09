// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import java.util.List;

/**
 * Supports better matching of local source files with remote scripts by leveraging the fact
 * that the original source could be preprocessed by adding a prefix and suffix (usually to
 * put user code in some scope). This class tries to match script source with a list of
 * known wrappers.
 */
public class SourceWrapSupport {
  private final List<Wrapper> wrappers;

  public SourceWrapSupport(List<Wrapper> wrappers) {
    this.wrappers = wrappers;
  }

  /**
   * Describes a known type of script source wrapper. The wrapping is essentially adding
   * a prefix and suffix, but these parts can be flexible.
   * This object is responsible for recognizing a particular wrapping.
   */
  public interface Wrapper {
    /**
     * @return user-visible wrapper name
     */
    String getName();

    /**
     * Checks whether remoteContext text has this particular wrapping.
     * @return match object if input matches or null
     */
    Match match(String remoteContent);

    /**
     * Defines a particular wrapper match. It can be used to construct identical wrapping
     * for user-edited script source. The separate interface is needed because its instance
     * can hold parameters of this particular prefix/suffix, if they can match multiple strings
     * (for example RegExp-based).
     */
    interface Match {
      Wrapper getWrapper();

      /**
       * Wraps a user source identically to how the original script was wrapped.
       */
      String wrap(String localContent);

      /**
       * @return the prefix length (used for position recalculation in diff)
       */
      int getPrefixLength();

      /**
       * @return the suffix length (used for position recalculation in diff)
       */
      int getSuffixLength();
    }
  }

  /**
   * Matches the remote script source against all known wrappers.
   * @return match object of first matching wrapper or null if no wrapper match
   */
  public Wrapper.Match chooseWrapper(String remoteContent) {
    for (Wrapper nextWrapper : wrappers) {
      Wrapper.Match match = nextWrapper.match(remoteContent);
      if (match != null) {
        return match;
      }
    }
    return null;
  }

  // Simple prefix-suffix-based. More complex implementations are expected.
  public static class StringBasedWrapper implements Wrapper {
    private final String name;
    private final String prefix;
    private final String suffix;

    public StringBasedWrapper(String name, String prefix, String suffix) {
      this.name = name;
      this.prefix = prefix;
      this.suffix = suffix;
    }

    @Override public String getName() {
      return name;
    }

    @Override
    public Match match(String remoteContent) {
      if (remoteContent.length() < prefix.length() + suffix.length()) {
        return null;
      }
      if (!remoteContent.startsWith(prefix) || !remoteContent.endsWith(suffix)) {
        return null;
      }
      return singleMatch;
    }

    private final Match singleMatch = new Match() {
      @Override public Wrapper getWrapper() {
        return StringBasedWrapper.this;
      }
      @Override public String wrap(String localContent) {
        return prefix + localContent + suffix;
      }
      @Override public int getPrefixLength() {
        return prefix.length();
      }
      @Override public int getSuffixLength() {
        return suffix.length();
      }
    };
  }
}

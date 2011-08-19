// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip;

/**
 * Script address. It is an algebraic type -- either integer id or string url.
 */
public abstract class ScriptUrlOrId {

  public interface Visitor<RES> {
    RES forId(long sourceId);
    RES forUrl(String url);
  }

  public abstract <RES> RES accept(Visitor<RES> visitor);

  public static ScriptUrlOrId forUrl(final String url) {
    return new ScriptUrlOrId() {
      @Override public <RES> RES accept(Visitor<RES> visitor) {
        return visitor.forUrl(url);
      }
      @Override public String toString() {
        return "url=" + url;
      }
    };
  }

  public static ScriptUrlOrId forId(final long sourceId) {
    return new ScriptUrlOrId() {
      @Override public <RES> RES accept(Visitor<RES> visitor) {
        return visitor.forId(sourceId);
      }
      @Override public String toString() {
        return "id=" + sourceId;
      }
    };
  }
}

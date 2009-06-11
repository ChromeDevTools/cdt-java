// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.editors;

import java.util.regex.Pattern;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * A utility for handling Javascript-related data.
 */
public class JavascriptUtil {

  private static final String OPEN_BRACKET = "["; //$NON-NLS-1$
  private static final String CLOSE_BRACKET = "]"; //$NON-NLS-1$

  public static final String IDENTIFIER_START_CHARS_REGEX = "\\p{L}_$"; //$NON-NLS-1$

  public static final String INSPECTED_CHARS_REGEX =
      IDENTIFIER_START_CHARS_REGEX + "\\d"; //$NON-NLS-1$

  /** Contains chars acceptable as start of Javascript identifier to inspect. */
  public static final Pattern IDENTIFIER_START_PATTERN =
      Pattern.compile(OPEN_BRACKET + IDENTIFIER_START_CHARS_REGEX + CLOSE_BRACKET);

  /** Contains chars acceptable as part of expression to inspect. */
  public static final Pattern INSPECTED_PATTERN =
      Pattern.compile(OPEN_BRACKET + INSPECTED_CHARS_REGEX + CLOSE_BRACKET);

  public static boolean isJsIdentifierCharacter(char ch) {
    return INSPECTED_PATTERN.matcher(String.valueOf(ch)).find();
  }

  /**
   * Returns a Javascript identifier surrounding the "offset" character in the
   * given document.
   *
   * @param doc the document to extract an identifier from
   * @param offset of the pivot character (before, in, or after the identifier)
   * @return Javascript identifier, or null if none found
   */
  public static String extractSurroundingJsIdentifier(IDocument doc, int offset) {
    IRegion region = getSurroundingIdentifierRegion(doc, offset);
    try {
      return region == null
          ? null
          : doc.get(region.getOffset(), region.getLength());
    } catch (BadLocationException e) {
      ChromiumDebugPlugin.log(e);
      return null;
    }
  }

  /**
   * @param doc the document to extract an identifier region from
   * @param offset of the pivot character (before, in, or after the identifier)
   * @return an IRegion corresponding to the Javascript identifier overlapping
   *         offset, or null if none
   */
  public static IRegion getSurroundingIdentifierRegion(IDocument doc, int offset) {
    if (doc == null) {
      return null;
    }
    try {
      char ch = doc.getChar(offset);
      if (!isJsIdentifierCharacter(ch) && offset > 0) {
        --offset; // cursor is AFTER the identifier
      }
      int start = offset;
      int end = offset;
      int goodStart = offset;
      while (start >= 0) {
        ch = doc.getChar(start);
        if (!isJsIdentifierCharacter(ch)) {
          break;
        }
        goodStart = start;
        --start;
      }
      start = goodStart;

      int length = doc.getLength();
      while (end < length) {
        try {
          if (!isJsIdentifierCharacter(doc.getChar(end))) {
            break;
          }
          ++end;
        } catch (BadLocationException e) {
          ChromiumDebugPlugin.log(e);
        }
      }
      if (start >= end) {
        return null;
      } else {
        return new Region(start, end - start);
      }
    } catch (BadLocationException e) {
      ChromiumDebugPlugin.log(e);
      return null;
    }
  }

  private JavascriptUtil() {
    // not instantiable
  }

}

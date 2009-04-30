// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.editors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.tools.v8.model.mirror.ValueMirror.Type;
import org.chromium.debug.core.util.JsonUtil;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * A utility for handling Javascript-related data.
 */
public class JavascriptUtil {

  private static final String TRUNCATED_ARRAY_SUFFIX = "...]"; //$NON-NLS-1$

  private static final String UNKNOWN_VALUE = "<null>"; //$NON-NLS-1$

  private static final String CLOSE_BRACKET = "]"; //$NON-NLS-1$

  private static final String OPEN_BRACKET = "["; //$NON-NLS-1$

  public static final String IDENTIFIER_START_CHARS_REGEX = "\\p{L}_$"; //$NON-NLS-1$

  public static final String INSPECTED_CHARS_REGEX =
      IDENTIFIER_START_CHARS_REGEX + "\\d"; //$NON-NLS-1$

  /** Contains chars acceptable as start of Javascript identifier to inspect */
  public static final Pattern IDENTIFIER_START_PATTERN =
      Pattern.compile(OPEN_BRACKET + IDENTIFIER_START_CHARS_REGEX + CLOSE_BRACKET);

  /** Contains chars acceptable as part of expression to inspect */
  public static final Pattern INSPECTED_PATTERN =
      Pattern.compile(OPEN_BRACKET + INSPECTED_CHARS_REGEX + CLOSE_BRACKET);

  private static final int MAX_HOVER_TEXT_LENGTH = 64;

  private JavascriptUtil() {
    // not instantiable
  }

  public static boolean isJsIdentifierCharacter(char ch) {
    return INSPECTED_PATTERN.matcher(String.valueOf(ch)).find();
  }

  public static boolean isJsIdentifierStartCharacter(char ch) {
    return IDENTIFIER_START_PATTERN.matcher(String.valueOf(ch)).find();
  }

  /**
   * Returns a Javascript identifier surrounding the "offset" character in the
   * given document.
   *
   * @return Javascript identifier, or null if none found
   */
  public static String extractSurroundingJsIdentifier(
      IDocument doc, int offset) {
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
   * @return an IRegion corresponding to the Javascript identifier overlapping
   * offset, or null if none
   */
  public static IRegion getSurroundingIdentifierRegion(
      IDocument doc, int offset) {
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
        // Do not handle "3id" as an identifier, it should be "id" instead.
        if (isJsIdentifierStartCharacter(ch)) {
          goodStart = start;
        }
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

  /**
   * Converts an IValue to a human-readable representation (e.g. for a
   * mouseover tooltip).
   */
  public static String stringify(IValue value) {
    if (value == null) {
      return UNKNOWN_VALUE;
    }
    try {
      // TODO(apavlov): implement good stringification of other types
      String typeName = value.getReferenceTypeName();
      Type type = Type.fromJsonTypeAndClassName(typeName, null);
      switch (type) {
        case JS_ARRAY:
          StringBuilder sb = new StringBuilder(OPEN_BRACKET);
          IVariable[] variables = value.getVariables();
          List<IVariable> indices = new ArrayList<IVariable>(variables.length);
          for (IVariable var : variables) {
            try {
              String name = var.getName();
              if (name.startsWith(OPEN_BRACKET) &&
                  name.endsWith(CLOSE_BRACKET) &&
                  JsonUtil.isInteger(
                      name.substring(1, name.length() - 1))) {
                indices.add(var);
              }
            } catch (DebugException e) {
              // skip the variable
            }
          }
          Collections.sort(indices, new Comparator<IVariable>() {
            @Override
            public int compare(IVariable o1, IVariable o2) {
              String name1;
              String name2;
              try {
                name1 = o1.getName();
                name2 = o2.getName();
              } catch (DebugException e) {
                return 0;
              }
              // at this point names are guaranteed to be ("[" integer "]")
              return Double.compare(
                  Integer.valueOf(name1.substring(1, name1.length() - 1)),
                  Integer.valueOf(name2.substring(1, name2.length() - 1)));
            }
          });
          boolean isFirst = true;
          for (int i = 0, size = indices.size(); i < size; ++i) {
            IVariable var = indices.get(i);
            if (!isFirst) {
              sb.append(',');
            } else {
              isFirst = false;
            }
            sb.append(i).append('=').append(stringify(var.getValue()));
            if (sb.length() >= MAX_HOVER_TEXT_LENGTH) {
              return sb.substring(
                  0, MAX_HOVER_TEXT_LENGTH - TRUNCATED_ARRAY_SUFFIX.length()) +
                      TRUNCATED_ARRAY_SUFFIX;
            }
          }
          return sb.append(']').toString();
        default:
          return value.getValueString();
      }
    } catch (DebugException e) {
      ChromiumDebugPlugin.log(e);
      return null;
    }
  }
}

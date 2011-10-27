// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@96703

package org.chromium.sdk.internal.wip.protocol.input.page;

/**
 Cookie object
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface CookieValue {
  /**
   Cookie name.
   */
  String name();

  /**
   Cookie value.
   */
  String value();

  /**
   Cookie domain.
   */
  String domain();

  /**
   Cookie path.
   */
  String path();

  /**
   Cookie expires.
   */
  long expires();

  /**
   Cookie size.
   */
  long size();

  /**
   True if cookie is http-only.
   */
  boolean httpOnly();

  /**
   True if cookie is secure.
   */
  boolean secure();

  /**
   True in case of session cookie.
   */
  boolean session();

}

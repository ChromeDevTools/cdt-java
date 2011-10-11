// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@96703

package org.chromium.sdk.internal.wip.protocol.input.console;

/**
 Enables console domain, sends the messages collected so far to the client by means of the <code>messageAdded</code> notification.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface EnableData {
  /**
   Number of messages dropped due to message threashold overflow.
   */
  long expiredMessagesCount();

}

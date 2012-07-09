package org.chromium.debug.ui.actions.pinpoint;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
  private static final String BUNDLE_NAME = "org.chromium.debug.ui.actions.pinpoint.messages"; //$NON-NLS-1$
  public static String DialogImpl_ADD_WATCH;
  public static String DialogImpl_EXPRESSION_PROMPT;
  public static String DialogImpl_GLOBAL_MARK;
  public static String DialogImpl_PROPERTY_PREVIEW;
  public static String DialogImpl_WINDOW_SUBTITLE;
  public static String DialogImpl_WINDOW_TITLE;
  public static String LogicImpl_CONTEXT_DISMISSED;
  public static String LogicImpl_DOT_EXPECTED;
  public static String LogicImpl_ENTER_AFTER_DOT;
  public static String LogicImpl_ENTER_EXPRESSION;
  public static String LogicImpl_INVALID_COMPONENT_CHAR;
  public static String LogicImpl_INVALID_COMPONENT_START;
  public static String LogicImpl_NOT_FOR_PRIMITIVE;
  public static String LogicImpl_PROBLEM_ON_REMOTE;
  public static String LogicImpl_PROPERTY_FREE;
  public static String LogicImpl_PROPERTY_WILL_BE_OVERWRITTEN;
  public static String LogicImpl_RESULT_FAILURE_TITLE;
  public static String LogicImpl_VALUE_IS_NOT_AVAILABLE;
  public static String LogicImpl_WARNING_SEEMS_A_PROBLEM;
  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}

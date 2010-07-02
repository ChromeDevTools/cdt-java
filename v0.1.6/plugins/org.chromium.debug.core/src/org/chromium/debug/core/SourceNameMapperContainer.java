package org.chromium.debug.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.ISourceContainerTypeDelegate;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.osgi.util.NLS;

/**
 * A special type of container that translates names of the source and delegates lookup
 * to another source container.
 * This could be useful when JS resource name is like "http://localhost/scripts/util.js"; such
 * source name could be converted into "scripts/util.js".
 * Currently container supports only prefix-based translation: if source name starts with a prefix,
 * the prefix is truncated; otherwise the source name is discarded.
 */
public class SourceNameMapperContainer implements ISourceContainer {

  private static final String TYPE_ID =
      "org.chromium.debug.core.SourceNameMapperContainer.type"; //$NON-NLS-1$

  private final ISourceContainer targetContainer;
  private final String prefix;

  public SourceNameMapperContainer(String prefix, ISourceContainer targetContainer) {
    this.targetContainer = targetContainer;
    this.prefix = prefix;
  }

  public void dispose() {
  }

  public String getPrefix() {
    return prefix;
  }

  public ISourceContainer getTargetContainer() {
    return targetContainer;
  }

  public Object[] findSourceElements(String name) throws CoreException {
    if (!name.startsWith(prefix)) {
      return new Object[0];
    }
    String shortName = name.substring(prefix.length());
    return targetContainer.findSourceElements(shortName);
  }


  public String getName() {
    return NLS.bind(Messages.SourceNameMapperContainer_NAME, prefix);
  }


  public ISourceContainer[] getSourceContainers() {
    return new ISourceContainer[] { targetContainer };
  }


  public ISourceContainerType getType() {
    return DebugPlugin.getDefault().getLaunchManager().getSourceContainerType(TYPE_ID);
  }


  public void init(ISourceLookupDirector director) {
  }


  public boolean isComposite() {
    return true;
  }

  private String getMemento() throws CoreException {
    StringBuilder builder = new StringBuilder();
    MementoFormat.encodeComponent(prefix, builder);
    MementoFormat.encodeComponent(targetContainer.getType().getId(), builder);
    MementoFormat.encodeComponent(targetContainer.getType().getMemento(targetContainer), builder);
    return builder.toString();
  }


  public Object getAdapter(Class adapter) {
    return null;
  }

  /**
   * A type delegate that serializes/deserializes container instances into/from memento.
   */
  public static class TypeDelegate implements ISourceContainerTypeDelegate {
    public ISourceContainer createSourceContainer(String memento) throws CoreException {
      MementoFormat.Parser parser = new MementoFormat.Parser(memento);
      String prefix;
      String typeId;
      String subContainerMemento;
      try {
        prefix = parser.nextComponent();
        typeId = parser.nextComponent();
        subContainerMemento = parser.nextComponent();
      } catch (MementoFormat.ParserException e) {
        throw new CoreException(new Status(IStatus.ERROR,
            ChromiumDebugPlugin.PLUGIN_ID, "Failed to parse memento", e)); //$NON-NLS-1$
      }
      ISourceContainerType subContainerType =
          DebugPlugin.getDefault().getLaunchManager().getSourceContainerType(typeId);
      ISourceContainer subContainer = subContainerType.createSourceContainer(subContainerMemento);
      return new SourceNameMapperContainer(prefix, subContainer);
    }

    public String getMemento(ISourceContainer container) throws CoreException {
      SourceNameMapperContainer chromeContainer = (SourceNameMapperContainer) container;
      return chromeContainer.getMemento();
    }
  }

  /**
   * Handles memento string format. The format is just a sequence of strings that are preceded
   * with their lengths and decorated with parentheses to make it more human-readable.
   */
  private static class MementoFormat {

    static void encodeComponent(String component, StringBuilder output) {
      output.append(component.length());
      output.append('(').append(component).append(')');
    }

    /**
     * A simple parser that reads char sequence as a sequence of strings.
     */
    static class Parser {
      private final CharSequence charSequence;
      private int pos = 0;
      Parser(CharSequence charSequence) {
        this.charSequence = charSequence;
      }
      String nextComponent() throws ParserException {
        if (pos >= charSequence.length()) {
          throw new ParserException("Unexpected end of line"); //$NON-NLS-1$
        }
        char ch = charSequence.charAt(pos);
        pos++;
        int num = Character.digit(ch, 10);
        if (num == -1) {
          throw new ParserException("Digit expected"); //$NON-NLS-1$
        }
        int len = num;
        while (true) {
          if (pos >= charSequence.length()) {
            throw new ParserException("Unexpected end of line"); //$NON-NLS-1$
          }
          ch = charSequence.charAt(pos);
          if (!Character.isDigit(ch)) {
            break;
          }
          pos++;
          num = Character.digit(ch, 10);
          if (num == -1) {
            throw new ParserException("Digit expected"); //$NON-NLS-1$
          }
          len = len * 10 + num;
        }
        pos++;
        if (pos + len + 1 > charSequence.length()) {
          throw new ParserException("Unexpected end of line"); //$NON-NLS-1$
        }
        String result = charSequence.subSequence(pos, pos + len).toString();
        pos += len + 1;
        return result;
      }
    }
    private static class ParserException extends Exception {
      ParserException() {
      }
      ParserException(String message, Throwable cause) {
        super(message, cause);
      }
      ParserException(String message) {
        super(message);
      }
      ParserException(Throwable cause) {
        super(cause);
      }
    }
  }
}

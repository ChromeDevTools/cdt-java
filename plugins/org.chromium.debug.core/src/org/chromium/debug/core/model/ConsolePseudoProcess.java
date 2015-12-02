// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.core.model;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.debug.core.model.ITerminate;

/**
 * This process corresponds to a Debugger-Chrome connection and its main
 * purpose is to expose connection log (see process console in UI).
 */
public class ConsolePseudoProcess extends PlatformObject implements IProcess {

  private final ILaunch launch;
  private final Retransmitter outputMonitor;
  private final ITerminate connectionTerminate;
  private final String name;
  private Map<String, String> attributes = null;

  private final IStreamsProxy streamsProxy = new IStreamsProxy() {
    public IStreamMonitor getErrorStreamMonitor() {
      return NullStreamMonitor.INSTANCE;
    }
    public IStreamMonitor getOutputStreamMonitor() {
      return outputMonitor;
    }
    public void write(String input) {
      // ignore
    }
  };

  /**
   * Constructs a ConsolePseudoProcess, adding this process to the given launch.
   *
   * @param launch the parent launch of this process
   * @param name the label used for this process
   */
  public ConsolePseudoProcess(ILaunch launch, String name, Retransmitter retransmitter,
      ITerminate connectionTerminate) {
    this.launch = launch;
    this.name = name;
    this.outputMonitor = retransmitter;
    outputMonitor.consolePseudoProcess = this;
    this.connectionTerminate = connectionTerminate;

    this.launch.addProcess(this);
    fireCreationEvent();
  }

  /**
   * @return writer which directs its contents to process console
   */
  public Writer getOutputWriter() {
    return outputMonitor;
  }

  public String getLabel() {
      return name;
  }

  public ILaunch getLaunch() {
      return launch;
  }

  public boolean isTerminated() {
      return connectionTerminate.isTerminated();
  }

  public void terminate() throws DebugException {
    connectionTerminate.terminate();
  }

  public boolean canTerminate() {
    return connectionTerminate.canTerminate();
  }

  public IStreamsProxy getStreamsProxy() {
    return streamsProxy;
  }

  /*
   * We do not expect intensive usage of attributes for this class. In fact, other option was to
   * keep this method no-op.
   */
  public synchronized void setAttribute(String key, String value) {
    if (attributes == null) {
      attributes = new HashMap<String, String>(1);
    }
    String origVal = attributes.get(key);
    if (origVal != null && origVal.equals(value)) {
      return;
    }

    attributes.put(key, value);
    fireChangeEvent();
  }

  /*
   * We do not expect intensive usage of attributes for this class. In fact, other option was to
   * put a stub here.
   */
  public synchronized String getAttribute(String key) {
    if (attributes == null) {
      return null;
    }
    return attributes.get(key);
  }

  public int getExitValue() throws DebugException {
    if (isTerminated()) {
      return 0;
    }
    throw new DebugException(new Status(IStatus.ERROR, ChromiumDebugPlugin.PLUGIN_ID,
        "Process hasn't been terminated yet"));  //$NON-NLS-1$
  }

  private void fireCreationEvent() {
    fireEvent(new DebugEvent(this, DebugEvent.CREATE));
  }

  private void fireEvent(DebugEvent event) {
    DebugPlugin manager = DebugPlugin.getDefault();
    if (manager != null) {
      manager.fireDebugEventSet(new DebugEvent[] { event });
    }
  }

  private void fireTerminateEvent() {
    outputMonitor.flush();
    fireEvent(new DebugEvent(this, DebugEvent.TERMINATE));
  }

  private void fireChangeEvent() {
    fireEvent(new DebugEvent(this, DebugEvent.CHANGE));
  }

  @Override
  public Object getAdapter(Class adapter) {
    if (adapter.equals(IProcess.class)) {
      return this;
    }
    if (adapter.equals(ILaunch.class)) {
      return getLaunch();
    }
    if (adapter.equals(ILaunchConfiguration.class)) {
      return getLaunch().getLaunchConfiguration();
    }
    return super.getAdapter(adapter);
  }


  private static class NullStreamMonitor implements IStreamMonitor {
    public void addListener(IStreamListener listener) {
    }
    public String getContents() {
      return null;
    }
    public void removeListener(IStreamListener listener) {
    }
    static final NullStreamMonitor INSTANCE = new NullStreamMonitor();
  }

  /**
   * Responsible for getting text as {@link Writer} and retransmitting it
   * as {@link IStreamMonitor} to whoever is interested.
   * However in its initial state it only receives signal (the text) and saves it in a buffer.
   * For {@link Retransmitter} to start giving the signal away one should
   * call {@link #startFlushing} method.
   */
  public static class Retransmitter extends Writer implements IStreamMonitor {
    private StringWriter writer = new StringWriter();
    private boolean isFlushing = false;
    private final List<IStreamListener> listeners = new ArrayList<IStreamListener>(2);
    private volatile ConsolePseudoProcess consolePseudoProcess = null;

    public synchronized void addListener(IStreamListener listener) {
      listeners.add(listener);
    }

    public String getContents() {
      return null;
    }

    public synchronized void removeListener(IStreamListener listener) {
      listeners.remove(listener);
    }

    @Override
    public synchronized void flush() {
      if (!isFlushing) {
        return;
      }
      String text = writer.toString();
      int lastLinePos;
      final boolean flushOnlyFullLines = true;
      if (flushOnlyFullLines) {
        int pos = text.lastIndexOf('\n');
        if (pos == -1) {
          // No full line in the buffer.
          return;
        }
        lastLinePos = pos + 1;
      } else {
        lastLinePos = text.length();
      }
      String readyText = text.substring(0, lastLinePos);
      writer = new StringWriter();
      if (lastLinePos != text.length()) {
        String rest = text.substring(lastLinePos);
        writer.append(rest);
      }
      for (IStreamListener listener : listeners) {
        listener.streamAppended(readyText, this);
      }
    }

    @Override
    public synchronized void close() {
      // do nothing
    }

    @Override
    public synchronized void write(char[] cbuf, int off, int len) {
      writer.write(cbuf, off, len);
    }

    public synchronized void startFlushing() {
      isFlushing = true;
      flush();
    }

    public void processClosed() {
      ConsolePseudoProcess consolePseudoProcess0 = this.consolePseudoProcess;
      if (consolePseudoProcess0 != null) {
        consolePseudoProcess0.fireTerminateEvent();
      }
    }
  }
}

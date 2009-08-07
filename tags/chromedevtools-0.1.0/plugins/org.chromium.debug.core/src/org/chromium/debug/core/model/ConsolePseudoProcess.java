// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

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

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This process corresponds to a Debugger-Chrome connection and its main
 * purpose is to expose connection log (see process console in UI). 
 */
public class ConsolePseudoProcess extends PlatformObject implements IProcess {
  
  private final ILaunch launch;
  private final WritableStreamMonitor outputMonitor;
  private final String name;
  private volatile boolean terminated;
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
  public ConsolePseudoProcess(ILaunch launch, String name, WritableStreamMonitor outputMonitor) {
      this.launch = launch;
      this.name = name;
      this.terminated = false;
      this.outputMonitor = outputMonitor;

      this.launch.addProcess(this);
      fireCreationEvent();
  }

  /**
   * @return writer which directs its contents to process console 
   */
  public Writer getOutputWriter() {
    return outputMonitor;
  }

  public boolean canTerminate() {
      return false;
  }

  public String getLabel() {
      return name;
  }
  
  public ILaunch getLaunch() {
      return launch;
  }

  public boolean isTerminated() {
      return terminated;
  }

  public void terminate() {
    throw new UnsupportedOperationException();
  }

  /**
   * Owner should call this when debug session is terminated.
   */
  void terminated() {
    outputMonitor.flush();
    terminated = true;
    fireTerminateEvent();
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

  public static class WritableStreamMonitor extends Writer implements IStreamMonitor {
    private StringWriter writer = new StringWriter();
    private boolean isFlushing = false;
    private final List<IStreamListener> listeners = new ArrayList<IStreamListener>(2);

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
      writer = new StringWriter();
      for (IStreamListener listener : listeners) {
        listener.streamAppended(text, this);
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
  }
}

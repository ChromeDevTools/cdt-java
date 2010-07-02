// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.tests.system;

import org.chromium.sdk.DebugContext;
import org.chromium.sdk.DebugEventListener;
import org.chromium.sdk.Script;
import org.chromium.sdk.TabDebugEventListener;

/**
 * Listens to various asynchronous debug events and let main thread to approve them and
 * synchronize with them. Unless {@link #setDefaultReceiver} is called,
 * listener always blocks waiting for the main thread to synchronize by {@link #expectEvent}.
 */
class StateManager {
  private final StateManager.Monitor monitor = new Monitor();

  TabDebugEventListener getTabListener() {
    return tabDebugEventListener;
  }

  /**
   * Called from the main thread. Waits for a particular event to happen and returns its data.
   * The event mask is specified by receiver parameter. Receiver has several visit* methods
   * with the similar semantics: it returns null if event should be skipped, non-null if
   * event is accepted and the data should be passed to the main thread function or may throw
   * an exception if test assumptions are broken.
   */
  <RES> RES expectEvent(EventVisitor<RES> receiver) throws SmokeException {
    RES res;
    do {
      res = expectOneEvent(receiver);
    } while (res == null);
    return res;
  }
  private <RES> RES expectOneEvent(EventVisitor<RES> receiver) throws SmokeException {
    synchronized (monitor) {
      if (monitor.pendingException != null) {
        throw new SmokeException(monitor.pendingException);
      }
      monitor.defaultReceiver = null;
      if (monitor.pendingEvent == null) {
        try {
          monitor.wait(TIMEOUT_MS);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        if (monitor.pendingEvent == null) {
          throw new SmokeException("Timeout waiting for event with " + receiver);
        }
      }
      RES res = monitor.pendingEvent.accept(receiver);
      monitor.pendingEvent = null;
      monitor.notify();
      return res;
    }
  }

  /**
   * Called from the main thread. Allows non-blocking processing of events in listener.
   * The default receiver accepts all events but its return value is ignored unless it threw an
   * exception.
   */
  void setDefaultReceiver(EventVisitor<?> receiver) throws SmokeException {
    synchronized (monitor) {
      if (monitor.pendingException != null) {
        throw new SmokeException(monitor.pendingException);
      }
      monitor.defaultReceiver = receiver;

      if (receiver != null && monitor.pendingEvent != null) {
        monitor.pendingEvent.accept(receiver);
        monitor.pendingEvent = null;
        monitor.notify();
      }
    }
  }

  /**
   * Called from Dispatch thread by listener implementation.
   */
  private void processEvent(Event event) {
    synchronized (monitor) {
      if (monitor.defaultReceiver != null) {
        try {
          event.accept(monitor.defaultReceiver);
        } catch (SmokeException e) {
          monitor.pendingException  = e;
        }
        return;
      }
      monitor.pendingEvent = event;
      monitor.notify();
      try {
        monitor.wait(TIMEOUT_MS);
        if (monitor.pendingEvent != null) {
          throw new RuntimeException("Timeout waiting for event processing");
        }
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private final TabDebugEventListener tabDebugEventListener = new TabDebugEventListener() {
    public void closed() {
      processEvent(new Event() {
        public <RES> RES accept(EventVisitor<RES> visitor) throws SmokeException {
          return visitor.visitClosed();
        }
      });
    }
    public void navigated(final String newUrl) {
      processEvent(new Event() {
        public <RES> RES accept(EventVisitor<RES> visitor) throws SmokeException {
          return visitor.visitNavigated(newUrl);
        }
      });
    }
    public DebugEventListener getDebugEventListener() {
      return debugEventListener;
    }
  };
  private final DebugEventListener debugEventListener = new DebugEventListener() {
    public void disconnected() {
      processEvent(new Event() {
        public <RES> RES accept(EventVisitor<RES> visitor) throws SmokeException {
          return visitor.visitDisconnected();
        }
      });
    }

    public void resumed() {
      processEvent(new Event() {
        public <RES> RES accept(EventVisitor<RES> visitor) throws SmokeException {
          return visitor.visitResumed();
        }
      });
    }

    public void scriptLoaded(final Script newScript) {
      processEvent(new Event() {
        public <RES> RES accept(EventVisitor<RES> visitor) throws SmokeException {
          return visitor.visitScriptLoaded(newScript);
        }
      });
    }

    public void suspended(final DebugContext context) {
      processEvent(new Event() {
        public <RES> RES accept(EventVisitor<RES> visitor) throws SmokeException {
          return visitor.visitSuspended(context);
        }
      });
    }
  };

  /**
   * An internal implementation of event from {@link DebugEventListener}
   * and {@link TabDebugEventListener}.
   */
  private interface Event {
    <RES> RES accept(EventVisitor<RES> visitor) throws SmokeException;
  }

  private static class Monitor {
    EventVisitor<?> defaultReceiver = null;
    Event pendingEvent = null;
    SmokeException pendingException = null;
  }

  private static final int TIMEOUT_MS = 10000;
}
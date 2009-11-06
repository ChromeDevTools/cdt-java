// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Manager that switches on and off some resource for a shared multiuser access.
 * The nature of actual resource should be defined in subclass of
 * {@link SessionManager}. Time period when resource is on is called "session".
 * Switch on operation (aka session creation) must be an atomic operation.
 * Switch off (aka session closing) may be lengthy asynchronous operation.
 * <p>
 * If no user needs it, manager switches the resource off. On the first demand
 * resource gets switched on (and new session gets created). After the last user
 * has released the resource, the session finishes either instantly or
 * some time later. In the latter case resource becomes temporary unavailable.
 * The manager does not operate resource in any other sense than switching it
 * on and off.
 * <p>
 * Every user first acquires the resource by calling {@link #connect()} method.
 * It gets ticket which points to the corresponding session. Method
 * {@link Ticket#dismiss()} must be called when resource is no more needed.
 * @param <SESSION> user class that represents a session; must
 *                  extend {@link SessionBase}
 * @param <EX> exception that is allowed to be thrown when resource is being switched
 *             on and the new session is starting; {@link RuntimeException}
 *             is a good default parameter value
 */
public abstract class SessionManager<SESSION extends SessionManager.SessionBase<SESSION>,
    EX extends Exception> {

  // Holds current session; all access must be synchronized on "this".
  private SESSION currentSession = null;

  /**
   * Ticket to resource use. Every client gets its own copy. All tickets must
   * be dismissed in order for resource to be switched off.
   * @param <SESSION> is be the same type as of manager that issued this ticket
   */
  public interface Ticket<SESSION> {
    /**
     * Each valid ticket points to session of the resource. The actual type
     * {@code SESSION} is provided by user (as a type parameter of enclosing
     * SessionManager). The actual resource should be accessible from
     * {@code SESSION}.
     * @return non-null current session
     * @throws IllegalStateException if ticket is no longer valid
     */
    SESSION getSession();

    /**
     * Releases resource and makes ticket invalid. Switches the resource
     * off if it was a last ticket.
     * @throws IllegalStateException if ticket is no more valid
     */
    void dismiss();
  }

  /**
   * Registers user request for resource and switches the resource on if required.
   * @return new ticket which symbolize use of resource until
   *             {@link Ticket#dismiss()} is called
   * @throws EX if connect required creating a new session and if the new session creation
   *         has failed
   */
  public Ticket<SESSION> connect() throws EX {
    synchronized (this) {
      if (currentSession != null) {
        // this may reset currentSession
        currentSession.checkHealth();
      }
      if (currentSession == null) {
        currentSession = newSessionObject();
        if (currentSession.manager != this) {
          throw new IllegalArgumentException("Wrong manager was set in session");
        }
      }
      return currentSession.newTicket();
    }
  }

  /**
   * User-provided constructor of a new session. It should switch the resource on
   * whatever it actually means.
   * @return new instance of resource use session
   * @throws EX if switching resource on or creating a new session failed
   */
  protected abstract SESSION newSessionObject() throws EX;

  /**
   * Base class for user session. It should be subclassed and it is parameterized by
   * this subclass. Object construction should have semantics of switching resource
   * on. It gets constructed via user-defined {@link SessionManager#newSessionObject()}.
   * Subclass should honestly pass instance of {@link SessionManager} to the base
   * class. User also should implement {@link #lastTicketDismissed()} and helper
   * {@link #getThisAsSession()}.
   * @param <SESSION> the very user class which extends {@link SessionBase};
   *                  {@link #getThisAsSession()} should compile as "return this;"
   */
  public static abstract class SessionBase<SESSION extends SessionBase<SESSION>> {
    private final SessionManager<?, ?> manager;
    private boolean isConnectionStopped = false;
    private boolean isCancelled = false;

    SessionBase(SessionManager<SESSION, ?> manager) {
      this.manager = manager;
    }

    /**
     * Must be simply "return this;"
     */
    protected abstract SESSION getThisAsSession();

    /**
     * Session may check its health here. This check is made on
     * every new connection. If it appears that the session is no longer alive
     * the method should call {@link #interruptSession()}. However, this is a highly
     * unwanted scenario: session should interrupt itself synchronously, no
     * on-demand from this method.
     */
    protected abstract void checkHealth();

    /**
     * User-provided behavior when no more valid tickets left. Resource should
     * be switched off whatever it actually means and the session closed.
     * There are 3 options here:
     * <ol>
     * <li>Method is finished with {@link #closeSession()} call. Method
     * {@link SessionManager#connect()} does not interrupt its service and simply
     * creates new session the next call.
     * <li>Method is finished with {@link #stopNewConnections()} call. Connection
     * process is put on hold after this and {@link SessionManager#connect()} starts
     * to throw {@link IllegalStateException}. Later {@link #closeSession()} must
     * be called possibly asynchronously. After this the resource is available again
     * and a new session may be created.
     * <li>Do not call any of methods listed above. This probably works but is
     * not specified here.
     * </ol>
     */
    protected abstract void lastTicketDismissed();

    /**
     * See {@link #lastTicketDismissed()}. This method is supposed to be called
     * from there, but not necessarily.
     */
    protected void stopNewConnections() {
      synchronized (manager) {
        isConnectionStopped = true;
      }
    }

    /**
     * Stops all new connections and cancels all existing tickets. Don't forget
     * to call {@link #closeSession()} manually.
     * @return collection of exceptions we gathered from tickets
     */
    protected Collection<? extends RuntimeException> interruptSession() {
      synchronized (manager) {
        isConnectionStopped = true;
        isCancelled = true;
        // TODO(peter.rybin): notify listeners here in case they are interested
        tickets.clear();
      }

      return Collections.emptyList();
    }

    /**
     * See {@link #lastTicketDismissed()}. This method is supposed to be called
     * from there, but not necessarily.
     */
    protected void closeSession() {
      synchronized (manager) {
        isConnectionStopped = true;
        if (!tickets.isEmpty()) {
          throw new IllegalStateException("Some tickets are still valid");
        }
        if (manager.currentSession != this) {
          throw new IllegalStateException("Session is not active");
        }
        manager.currentSession = null;
      }
    }

    /**
     * Creates new ticket that is to be dismissed later.
     * Internal method. However user may use it or even make it public.
     */
    protected Ticket<SESSION> newTicket() {
      synchronized (manager) {
        if (isConnectionStopped) {
          throw new IllegalStateException("Connection has been stopped");
        }
        TicketImpl ticketImpl = new TicketImpl();
        tickets.add(ticketImpl);
        return ticketImpl;
      }
    }

    private final List<TicketImpl> tickets = new ArrayList<TicketImpl>();

    private class TicketImpl implements Ticket<SESSION> {
      private volatile boolean isDismissed = false;

      public void dismiss() {
        synchronized (manager) {
          if (!isCancelled) {
            boolean res = tickets.remove(this);
            if (!res) {
              throw new IllegalStateException("Ticket is already dismissed");
            }
            if (tickets.isEmpty()) {
              lastTicketDismissed();
            }
          }
          isDismissed = true;
        }
      }

      public SESSION getSession() {
        if (isDismissed) {
          throw new IllegalStateException("Ticket is dismissed");
        }
        return getThisAsSession();
      }
    }
  }

  /**
   * This method is completely unsynchronized. Is should be used for
   * single-threaded tests only.
   */
  public SESSION getCurrentSessionForTest() {
    return currentSession;
  }
}

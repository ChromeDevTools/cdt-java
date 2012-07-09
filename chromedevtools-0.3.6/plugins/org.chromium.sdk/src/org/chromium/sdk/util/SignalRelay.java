// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.util;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A node in a network that broadcasts some signal among all its peers one time. The signal may get
 * converted when goes from node to node. The first time a node receives a signal, it calls
 * a user callback. It ignores all further signals. A signal is accompanied by exception
 * called 'cause' that can be null.
 * <p>The class is thread-safe.
 * <p>This class is useful for a shutdown strategy, when several resources should be taken down
 * together, but there are no single authority to manage it. E.g. in a client-medium-server
 * threesome each part can initiate shutdown.
 * <p>Nodes of different signal system can be bound using {@link SignalConverter}.
 *
 * @param <SIGNAL> type of signal that this node works with
 */
public class SignalRelay<SIGNAL> {
  public static <T> SignalRelay<T> create(Callback<T> callback) {
    return new SignalRelay<T>(callback);
  }

  private static final Logger LOGGER = Logger.getLogger(SignalRelay.class.getName());

  private final List<PeerWrapper<SIGNAL, ?>> peers = new ArrayList<PeerWrapper<SIGNAL, ?>>(2);
  private final Callback<SIGNAL> callback;
  private boolean isSignalled = false;
  private SIGNAL savedSignal;

  public SignalRelay(Callback<SIGNAL> callback) {
    this.callback = callback;
  }

  public void sendSignal(SIGNAL signal, Exception cause) {
    sendSignalImpl(this, signal, cause);
  }

  public synchronized boolean isSignalled() {
    return isSignalled;
  }

  /**
   * @return a signal received or null if called before a signal came in.
   */
  public synchronized SIGNAL getReceivedSignal() {
    return savedSignal;
  }

  /**
   * Binds this node to a peer node. If the peer already received a signal, accepts it and
   * throws an exception.
   * @param <OPPOSITE> type of signal the peer node works with
   * @param toPeerConverter a signal converter that is used when sending signal or null
   * @param fromPeerConverter a signal converter that is used when receiving signal or null
   * @throws AlreadySignalledException when binding to node that already has a signal
   */
  public <OPPOSITE> void bind(SignalRelay<OPPOSITE> peer,
      SignalConverter<SIGNAL, OPPOSITE> toPeerConverter,
      SignalConverter<OPPOSITE, SIGNAL> fromPeerConverter) throws AlreadySignalledException {
    this.addPeer(peer, toPeerConverter);

    try {
      peer.addPeer(this, fromPeerConverter);
    } catch (AlreadySignalledException e) {
      PeerWrapper.send(this, peer.getReceivedSignal(), this, fromPeerConverter, e);
      throw e;
    }
  }

  /**
   * An interface to notify a user about a received signal.
   */
  public interface Callback<S> {
    /**
     * Called from the thread that initiated a broadcast (i.e. called
     * {@link #onSignal(Object, Exception)}).
     * @throws RuntimeException thrown exception gets caught and logged
     * @throws Error not caught
     */
    void onSignal(S signal, Exception cause) throws RuntimeException, Error;
  }

  /**
   * A converter that translates signals from one type to another so that unrelated resources
   * could co-work.
   */
  public interface SignalConverter<FROM, TO> {
    TO convert(FROM source);
  }

  public static class AlreadySignalledException extends Exception {
    AlreadySignalledException() {
      super();
    }

    AlreadySignalledException(String message, Throwable cause) {
      super(message, cause);
    }

    AlreadySignalledException(String message) {
      super(message);
    }

    AlreadySignalledException(Throwable cause) {
      super(cause);
    }
  }

  private void sendSignalImpl(SignalRelay<?> source, SIGNAL signal, Exception cause) {
    synchronized (this) {
      if (isSignalled) {
        return;
      }
      isSignalled = true;
      savedSignal = signal;
    }
    try {
      callback.onSignal(signal, cause);
    } catch (RuntimeException e) {
      LOGGER.log(Level.SEVERE, "Exception in relay callback", e);
    }
    for (PeerWrapper<SIGNAL, ?> peer : peers) {
      if (peer.getPeer() == source) {
        continue;
      }
      peer.send(this, signal, cause);
    }
  }

  private synchronized <OPPOSITE> void addPeer(SignalRelay<OPPOSITE> peer,
      SignalConverter<SIGNAL, OPPOSITE> converter) throws AlreadySignalledException {
    if (isSignalled) {
      throw new AlreadySignalledException();
    }
    peers.add(new PeerWrapper<SIGNAL, OPPOSITE>(peer, converter));
  }

  private static class PeerWrapper<S, P> {
    private final SignalRelay<P> peer;
    private final SignalConverter<S, P> converter;

    PeerWrapper(SignalRelay<P> peer, SignalConverter<S, P> converter) {
      this.peer = peer;
      this.converter = converter;
    }

    SignalRelay<?> getPeer() {
      return peer;
    }

    void send(SignalRelay<?> source, S signal, Exception cause) {
      send(source, signal, peer, converter, cause);
    }

    static <T, D> void send(SignalRelay<?> source, T signal, SignalRelay<D> destination,
        SignalConverter<T, D> converter, Exception cause) {
      D convertedSignal;
      if (converter == null) {
        convertedSignal = null;
      } else {
        convertedSignal = converter.convert(signal);
      }
      destination.sendSignalImpl(source, convertedSignal, cause);
    }
  }
}

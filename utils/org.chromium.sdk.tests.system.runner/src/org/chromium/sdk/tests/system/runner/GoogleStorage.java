// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.tests.system.runner;

import static org.chromium.sdk.tests.system.runner.DomUtils.getTextParameter;
import static org.chromium.sdk.tests.system.runner.DomUtils.iterableNodes;
import static org.chromium.sdk.tests.system.runner.DomUtils.visitNode;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.chromium.sdk.tests.system.runner.DomUtils.DefaultVisitor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Access to Google storage cloud service. The code is hacky and is based on plain and simple
 * reversed engineering of how directory index html page works.
 */
class GoogleStorage {
  interface Resource {
    String getShortName();
    String getFullName();

    interface Visitor<R> {
      R visitFile(File file);
      R visitDir(Dir dir);
    }

    <R> R accept(Visitor<R> visitor);
  }

  interface File extends Resource {
    String getUrl();
  }

  interface Dir extends Resource {
    List<Resource> getChildren();
  }

  private final String urlBase;
  private final DocumentBuilder documentBuilder;

  GoogleStorage(String urlBase) {
    this.urlBase = urlBase;

    try {
      documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
  }

  List<Resource> readNodes(String path) {
    try {
      return readNodesImpl(path);
    } catch (SAXException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private List<Resource> readNodesImpl(String path) throws SAXException, IOException {
    if (!path.endsWith("/")) {
      throw new IllegalArgumentException();
    }
    String urlString = urlBase + "/?delimiter=/&prefix=" + path;
    URL url = new URL(urlString);
    Document document = documentBuilder.parse(url.openStream());

    final List<Resource> result = new ArrayList<Resource>();

    DefaultVisitor<Void> nodeVisitor = new DefaultVisitor<Void>() {
      @Override public Void visitElement(Element element) {
        if (element.getTagName().equals("Contents")) {
          result.add(buildFile(element));
        } else if (element.getTagName().equals("CommonPrefixes")) {
          result.add(buildDir(element));
        }
        return null;
      }
      @Override protected Void visitDefault(Node node) {
        return null;
      }
    };
    for (Node childNode : iterableNodes(document.getDocumentElement().getChildNodes())) {
      visitNode(childNode, nodeVisitor);
    }
    return result;
  }

  private File buildFile(Element element) {
    final String key = getTextParameter(element, "Key");
    int separatorPos = key.lastIndexOf('/');
    final String shortName;
    if (separatorPos == -1) {
      shortName = key;
    } else {
      shortName = key.substring(separatorPos + 1);
    }
    return new File() {
      @Override
      public String getUrl() {
        return urlBase + '/' + key;
      }

      @Override public String getShortName() {
        return shortName;
      }
      @Override public String getFullName() {
        return key;
      }
      @Override public <R> R accept(Visitor<R> visitor) {
        return visitor.visitFile(this);
      }
    };
  }

  private Dir buildDir(Element element) {
    final String key = getTextParameter(element, "Key");
    final String fullName = key.substring(0, key.length() - 1);
    int separatorPos = fullName.lastIndexOf('/');
    final String shortName = fullName.substring(separatorPos + 1);
    return new Dir() {
      @Override
      public List<Resource> getChildren() {
        return readNodes(key);
      }

      @Override public String getShortName() {
        return shortName;
      }
      @Override public String getFullName() {
        return key;
      }
      @Override public <R> R accept(Visitor<R> visitor) {
        return visitor.visitDir(this);
      }
    };
  }
}

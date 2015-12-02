// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.tests.system.runner;

import java.util.AbstractList;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Entity;
import org.w3c.dom.EntityReference;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Notation;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;

/**
 * Utility class that adapts org.w3c.dom package to Java.
 */
public class DomUtils {

  static String getTextParameter(Element baseElement, final String name) {
    for (Node node : iterableNodes(baseElement.getChildNodes())) {
      String value = visitNode(node, new DefaultVisitor<String>() {
        @Override public String visitElement(Element element) {
          if (!element.getTagName().equals(name)) {
            return null;
          }
          return readTextParameter(element);
        }
        @Override protected String visitDefault(Node node1) {
          throw new RuntimeException();
        }
      });
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  static String readTextParameter(Element element) {
    NodeList list = element.getChildNodes();
    if (list.getLength() != 1) {
      throw new RuntimeException();
    }
    return visitNode(list.item(0), GET_TEXT_CONTENT_VISITOR);
  }

  private static final DefaultVisitor<String> GET_TEXT_CONTENT_VISITOR =
        new DefaultVisitor<String>() {
        @Override public String visitText(Text node) {
          return node.getTextContent();
        }
        @Override protected String visitDefault(Node node) {
          throw new RuntimeException();
        }
      };

  interface NodeVisitor<R> {
    R visitElement(Element node);
    R visitAttr(Attr node);
    R visitText(Text node);
    R visitCDATASection(CDATASection node);
    R visitEntityReference(EntityReference node);
    R visitEntity(Entity node);
    R visitProcessingInstruction(ProcessingInstruction node);
    R visitComment(Comment node);
    R visitDocument(Document node);
    R visitDocumentType(DocumentType node);
    R visitDocumentFragment(DocumentFragment node);
    R visitNotation(Notation node);
  }

  static abstract class DefaultVisitor<R> implements NodeVisitor<R> {
    @Override public R visitElement(Element node) {
      return visitDefault(node);
    }
    @Override public R visitAttr(Attr node) {
      return visitDefault(node);
    }
    @Override public R visitText(Text node) {
      return visitDefault(node);
    }
    @Override public R visitCDATASection(CDATASection node) {
      return visitDefault(node);
    }
    @Override public R visitEntityReference(EntityReference node) {
      return visitDefault(node);
    }
    @Override public R visitEntity(Entity node) {
      return visitDefault(node);
    }
    @Override public R visitProcessingInstruction(ProcessingInstruction node) {
      return visitDefault(node);
    }
    @Override public R visitComment(Comment node) {
      return visitDefault(node);
    }
    @Override public R visitDocument(Document node) {
      return visitDefault(node);
    }
    @Override public R visitDocumentType(DocumentType node) {
      return visitDefault(node);
    }
    @Override public R visitDocumentFragment(DocumentFragment node) {
      return visitDefault(node);
    }
    @Override public R visitNotation(Notation node) {
      return visitDefault(node);
    }
    protected abstract R visitDefault(Node node);
  }

  static <R> R visitNode(Node node, NodeVisitor<R> visitor) {
    switch (node.getNodeType()) {
      case Node.ELEMENT_NODE: return visitor.visitElement((Element) node);
      case Node.ATTRIBUTE_NODE: return visitor.visitAttr((Attr) node);
      case Node.TEXT_NODE: return visitor.visitText((Text) node);
      case Node.CDATA_SECTION_NODE: return visitor.visitCDATASection((CDATASection) node);
      case Node.ENTITY_REFERENCE_NODE: return visitor.visitEntityReference((EntityReference) node);
      case Node.ENTITY_NODE: return visitor.visitEntity((Entity) node);
      case Node.PROCESSING_INSTRUCTION_NODE:
          return visitor.visitProcessingInstruction((ProcessingInstruction) node);
      case Node.COMMENT_NODE: return visitor.visitComment((Comment) node);
      case Node.DOCUMENT_NODE: return visitor.visitDocument((Document) node);
      case Node.DOCUMENT_TYPE_NODE: return visitor.visitDocumentType((DocumentType) node);
      case Node.DOCUMENT_FRAGMENT_NODE:
          return visitor.visitDocumentFragment((DocumentFragment) node);
      case Node.NOTATION_NODE: return visitor.visitNotation((Notation) node);
      default: throw new RuntimeException();
    }
  }

  static Iterable<Node> iterableNodes(final NodeList nodeList) {
    return new AbstractList<Node>() {
      @Override public Node get(int index) {
        return nodeList.item(index);
      }
      @Override public int size() {
        return nodeList.getLength();
      }
    };
  }
}

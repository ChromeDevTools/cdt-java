package org.chromium.sdk.internal.protocol.data;

import java.util.List;

import org.chromium.sdk.internal.protocolparser.JsonOptionalField;
import org.chromium.sdk.internal.protocolparser.JsonType;

@JsonType
public interface LiveEditResult {
  OldTreeNode change_tree();
  TextualDiff textual_diff();

  @JsonOptionalField
  String created_script_name();

  @JsonOptionalField
  boolean stack_modified();

  boolean updated();

  @JsonType
  interface TextualDiff {
    List<Long> chunks();
  }

  @JsonType
  interface OldTreeNode {
    String name();
    String status();
    Positions positions();
    List<OldTreeNode> children();

    @JsonOptionalField
    Positions new_positions();

    @JsonOptionalField
    List<NewTreeNode> new_children();

    @JsonOptionalField
    String status_explanation();
  }

  @JsonType
  interface NewTreeNode {
    String name();
    Positions positions();
    List<NewTreeNode> children();
  }

  @JsonType
  interface Positions {
    long start_position();
    long end_position();
  }
}

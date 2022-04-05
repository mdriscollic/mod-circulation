package org.folio.circulation.domain;

import java.util.Collection;
import java.util.List;

import lombok.NonNull;
import lombok.Value;

@Value
public class ItemDescription {
  public static ItemDescription unknown() {
    return new ItemDescription(null, null, null, null, List.of());
  }

  String volume;
  String chronology;
  String numberOfPieces;
  String descriptionOfPieces;
  @NonNull Collection<String> yearCaption;
}

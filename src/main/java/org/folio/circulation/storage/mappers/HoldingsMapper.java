package org.folio.circulation.storage.mappers;

import static org.folio.circulation.domain.representations.HoldingsProperties.COPY_NUMBER_ID;
import static org.folio.circulation.domain.representations.ItemProperties.PERMANENT_LOCATION_ID;
import static org.folio.circulation.support.json.JsonPropertyFetcher.getProperty;

import org.folio.circulation.domain.Holdings;

import io.vertx.core.json.JsonObject;

public class HoldingsMapper {
  public Holdings toDomain(JsonObject holdingsRepresentation) {
    return new Holdings(getProperty(holdingsRepresentation, "instanceId"),
      getProperty(holdingsRepresentation, COPY_NUMBER_ID),
      getProperty(holdingsRepresentation, PERMANENT_LOCATION_ID));
  }
}

package api.support.builders;

import static org.folio.circulation.support.json.JsonPropertyWriter.write;
import static org.joda.time.DateTime.now;
import static org.joda.time.DateTimeZone.UTC;

import java.util.UUID;

import org.joda.time.DateTime;

import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.With;

@With
@AllArgsConstructor
class RequestByInstanceIdRequestBuilder implements Builder {
  private final DateTime requestDate;
  private final UUID requesterId;
  private final UUID instanceId;
  private final DateTime requestExpirationDate;
  private final UUID pickupServicePointId;
  private final String patronComments;

  public RequestByInstanceIdRequestBuilder() {
    this(now(UTC), null, null, now(UTC).plusWeeks(1), null, null);
  }

  @Override
  public JsonObject create() {
    JsonObject requestBody = new JsonObject();

    write(requestBody, "instanceId", instanceId);
    write(requestBody, "requestDate", requestDate);
    write(requestBody, "requesterId", requesterId);
    write(requestBody, "pickupServicePointId", pickupServicePointId);
    write(requestBody, "fulfilmentPreference", "Hold Shelf");
    write(requestBody, "requestExpirationDate", requestExpirationDate);
    write(requestBody, "patronComments", patronComments);

    return requestBody;
  }
}

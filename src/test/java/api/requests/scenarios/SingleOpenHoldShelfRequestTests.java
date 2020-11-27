package api.requests.scenarios;

import static api.support.builders.ItemBuilder.AVAILABLE;
import static api.support.builders.ItemBuilder.AWAITING_PICKUP;
import static api.support.builders.ItemBuilder.CHECKED_OUT;
import static api.support.builders.RequestBuilder.CLOSED_FILLED;
import static api.support.builders.RequestBuilder.OPEN_AWAITING_PICKUP;
import static api.support.fakes.PublishedEvents.byLogEventType;
import static api.support.http.ResourceClient.forRequestsStorage;
import static api.support.matchers.ItemStatusCodeMatcher.hasItemStatus;
import static api.support.matchers.ResponseStatusCodeMatcher.hasStatus;
import static api.support.matchers.ValidationErrorMatchers.hasErrorWith;
import static api.support.matchers.ValidationErrorMatchers.hasMessage;
import static api.support.matchers.ValidationErrorMatchers.hasParameter;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.waitAtMost;
import static org.folio.HttpStatus.HTTP_OK;
import static org.folio.circulation.support.json.JsonObjectArrayPropertyFetcher.mapToList;
import static org.folio.circulation.support.json.JsonPropertyFetcher.getProperty;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.UUID;

import org.folio.circulation.support.http.client.Response;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import api.support.APITests;
import api.support.builders.CheckInByBarcodeRequestBuilder;
import api.support.builders.RequestBuilder;
import api.support.fakes.FakePubSub;
import api.support.http.IndividualResource;
import api.support.http.ResourceClient;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.val;

public class SingleOpenHoldShelfRequestTests extends APITests {
  @Test
  public void statusChangesToAwaitingPickupWhenItemCheckedIn() {
    IndividualResource smallAngryPlanet = itemsFixture.basedUponSmallAngryPlanet();
    val james = usersFixture.james();
    val jessica = usersFixture.jessica();

    checkOutFixture.checkOutByBarcode(smallAngryPlanet, james);

    IndividualResource requestByJessica = requestsFixture.placeHoldShelfRequest(
      smallAngryPlanet, jessica, new DateTime(2017, 7, 22, 10, 22, 54, DateTimeZone.UTC));

    checkInFixture.checkInByBarcode(smallAngryPlanet);

    Response request = requestsClient.getById(requestByJessica.getId());

    assertThat(request, hasStatus(HTTP_OK));

    assertThat(request.getJson().getString("status"), is(OPEN_AWAITING_PICKUP));
    assertThat(request.getJson().getInteger("position"), is(1));

    smallAngryPlanet = itemsClient.get(smallAngryPlanet);

    assertThat(smallAngryPlanet, hasItemStatus(AWAITING_PICKUP));
  }

  @Test
  public void statusChangesToFulfilledWhenItemCheckedOutToRequester() {
    IndividualResource smallAngryPlanet = itemsFixture.basedUponSmallAngryPlanet();
    val james = usersFixture.james();
    val jessica = usersFixture.jessica();

    checkOutFixture.checkOutByBarcode(smallAngryPlanet, james);

    IndividualResource requestByJessica = requestsFixture.placeHoldShelfRequest(
      smallAngryPlanet, jessica, new DateTime(2017, 7, 22, 10, 22, 54, DateTimeZone.UTC));

    checkInFixture.checkInByBarcode(smallAngryPlanet);

    FakePubSub.clearPublishedEvents();

    final var checkOutResource = checkOutFixture.checkOutByBarcode(smallAngryPlanet, jessica);

    final var loan = checkOutResource.getJson();

    Response request = requestsClient.getById(requestByJessica.getId());

    assertThat(request, hasStatus(HTTP_OK));

    assertThat(request.getJson().getString("status"), is(CLOSED_FILLED));

    smallAngryPlanet = itemsClient.get(smallAngryPlanet);

    assertThat(smallAngryPlanet, hasItemStatus(CHECKED_OUT));

    final var checkOutLogEvent = waitAtMost(1, SECONDS)
      .until(this::getCheckOutLogEvent, is(notNullValue()));

    assertThat(checkOutLogEvent.loanId, is(getProperty(loan, "id")));
    assertThat(checkOutLogEvent.changedRequests, hasSize(1));

    final var onlyChangedRequest = checkOutLogEvent.changedRequests.get(0);

    assertThat(onlyChangedRequest.id, is(requestByJessica.getId().toString()));
    assertThat(onlyChangedRequest.requestType, is("Hold"));
    assertThat(onlyChangedRequest.oldRequestStatus, is("Open - Awaiting pickup"));
    assertThat(onlyChangedRequest.newRequestStatus, is("Closed - Filled"));
  }

  @Test
  public void itemCannotBeCheckedOutToOtherPatronWhenRequestIsAwaitingPickup() {
    IndividualResource smallAngryPlanet = itemsFixture.basedUponSmallAngryPlanet();
    val james = usersFixture.james();
    val jessica = usersFixture.jessica();
    val rebecca = usersFixture.rebecca();

    checkOutFixture.checkOutByBarcode(smallAngryPlanet, james);

    IndividualResource requestByJessica = requestsFixture.placeHoldShelfRequest(
      smallAngryPlanet, jessica, new DateTime(2017, 7, 22, 10, 22, 54, DateTimeZone.UTC));

    checkInFixture.checkInByBarcode(smallAngryPlanet);

    Response response = checkOutFixture.attemptCheckOutByBarcode(smallAngryPlanet, rebecca);

    assertThat(response.getJson(), hasErrorWith(allOf(
      hasMessage("The Long Way to a Small, Angry Planet (Barcode: 036000291452) " +
        "cannot be checked out to user Stuart, Rebecca " +
        "because it has been requested by another patron"),
      hasParameter("userBarcode", rebecca.getBarcode()))));

    Response request = requestsClient.getById(requestByJessica.getId());

    assertThat(request, hasStatus(HTTP_OK));

    assertThat(request.getJson().getString("status"), is(OPEN_AWAITING_PICKUP));

    smallAngryPlanet = itemsClient.get(smallAngryPlanet);

    assertThat(smallAngryPlanet, hasItemStatus(AWAITING_PICKUP));
  }

  @Test
  public void checkingInLoanThatFulfilsRequestShouldMakeItemAvailable() {
    IndividualResource smallAngryPlanet = itemsFixture.basedUponSmallAngryPlanet();
    val james = usersFixture.james();
    val jessica = usersFixture.jessica();

    checkOutFixture.checkOutByBarcode(smallAngryPlanet, james);

    requestsFixture.placeHoldShelfRequest(smallAngryPlanet, jessica,
      new DateTime(2017, 7, 22, 10, 22, 54, DateTimeZone.UTC));

    checkInFixture.checkInByBarcode(smallAngryPlanet);

    checkOutFixture.checkOutByBarcode(smallAngryPlanet, jessica);

    checkInFixture.checkInByBarcode(smallAngryPlanet);

    smallAngryPlanet = itemsClient.get(smallAngryPlanet);

    assertThat(smallAngryPlanet, hasItemStatus(AVAILABLE));
  }

  @Test
  public void closedRequestShouldNotAffectFurtherLoans() {
    IndividualResource smallAngryPlanet = itemsFixture.basedUponSmallAngryPlanet();
    val james = usersFixture.james();
    val jessica = usersFixture.jessica();
    val steve = usersFixture.steve();

    checkOutFixture.checkOutByBarcode(smallAngryPlanet, james);

    IndividualResource requestByJessica = requestsFixture.placeHoldShelfRequest(
      smallAngryPlanet, jessica, new DateTime(2017, 7, 22, 10, 22, 54, DateTimeZone.UTC));

    checkInFixture.checkInByBarcode(smallAngryPlanet);

    checkOutFixture.checkOutByBarcode(smallAngryPlanet, jessica);

    Response request = requestsClient.getById(requestByJessica.getId());

    assertThat(request, hasStatus(HTTP_OK));

    assertThat(request.getJson().getString("status"), is(CLOSED_FILLED));

    checkInFixture.checkInByBarcode(smallAngryPlanet);

    checkOutFixture.checkOutByBarcode(smallAngryPlanet, steve);

    smallAngryPlanet = itemsClient.get(smallAngryPlanet);

    assertThat(smallAngryPlanet, hasItemStatus(CHECKED_OUT));
  }

  @Test
  public void itemCannotBeCheckedInWhenRequestIsMissingPickupServicePoint() {
    IndividualResource smallAngryPlanet = itemsFixture.basedUponSmallAngryPlanet();
    val james = usersFixture.james();
    val jessica = usersFixture.jessica();
    val checkInServicePoint = servicePointsFixture.cd1();

    checkOutFixture.checkOutByBarcode(smallAngryPlanet, james);

    final IndividualResource holdRequest = requestsFixture.place(new RequestBuilder()
      .hold()
      .forItem(smallAngryPlanet)
      .by(jessica)
      .withRequestDate(new DateTime(2017, 7, 22, 10, 22, 54, DateTimeZone.UTC))
      .withPickupServicePoint(servicePointsFixture.cd1()));

    removeServicePoint(holdRequest.getId());

    Response response = checkInFixture.attemptCheckInByBarcode(new CheckInByBarcodeRequestBuilder()
        .forItem(smallAngryPlanet)
        .at(checkInServicePoint.getId()));

    assertThat(response.getJson(), hasErrorWith(hasMessage(
      "Failed to check in item due to the highest priority request missing a pickup service point")));
  }

  private void removeServicePoint(UUID requestId) {
    final ResourceClient requestsStorage = forRequestsStorage();

    final Response fetchedRequest = requestsStorage.getById(requestId);

    final JsonObject holdRequestWithoutPickupServicePoint = fetchedRequest.getJson();

    holdRequestWithoutPickupServicePoint.remove("pickupServicePointId");

    requestsStorage.replace(requestId, holdRequestWithoutPickupServicePoint);
  }

  private CheckOutLogEvent getCheckOutLogEvent() {
    final var publishedEvent = FakePubSub.getPublishedEvents().findFirst(byLogEventType("CHECK_OUT_EVENT"));
    final var logEventPayload = new JsonObject(getProperty(publishedEvent, "eventPayload"));

    return CheckOutLogEvent.builder()
      .loanId(getProperty(logEventPayload, "loanId"))
      .changedRequests(getChangedRequests(logEventPayload))
      .build();
  }

  private List<CheckOutLogEventChangedRequest> getChangedRequests(JsonObject logEventPayload) {
    return mapToList(logEventPayload, "requests",
      request -> CheckOutLogEventChangedRequest.builder()
        .id(getProperty(request, "id"))
        .requestType(getProperty(request, "requestType"))
        .oldRequestStatus(getProperty(request, "oldRequestStatus"))
        .newRequestStatus(getProperty(request, "newRequestStatus"))
        .build());
  }

  @AllArgsConstructor
  @Builder
  static class CheckOutLogEvent {
    private final String loanId;
    private final List<CheckOutLogEventChangedRequest> changedRequests;
  }

  @AllArgsConstructor
  @Builder
  static class CheckOutLogEventChangedRequest {
    private final String id;
    private final String requestType;
    private final String oldRequestStatus;
    private final String newRequestStatus;
  }
}

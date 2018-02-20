package org.folio.circulation.api.requests;

import org.folio.circulation.api.support.APITests;
import org.folio.circulation.api.support.builders.RequestBuilder;
import org.folio.circulation.api.support.builders.UserBuilder;
import org.folio.circulation.support.http.client.Response;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.folio.circulation.api.support.fixtures.LoansFixture.renewLoan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;

public class RequestsAPILoanRenewalTests extends APITests {

  @Test
  public void RenewalWithOutstandingHoldRequestDoesNotChangeItemStatus()
    throws InterruptedException,
    ExecutionException,
    TimeoutException,
    MalformedURLException,
    UnsupportedEncodingException {

    UUID id = UUID.randomUUID();

    UUID itemId = itemsFixture.basedUponSmallAngryPlanet().getId();

    UUID loanId = loansFixture.checkOutItem(itemId).getId();

    requestsClient.create(new RequestBuilder()
      .hold()
      .withId(id)
      .withItemId(itemId)
      .withRequesterId(usersClient.create(new UserBuilder()).getId()));

    renewLoan(loanId, loansClient);

    Response changedItem = itemsClient.getById(itemId);

    assertThat(changedItem.getJson().getJsonObject("status").getString("name"),
      is("Checked out - Held"));
  }

  @Test
  public void RenewalWithOutstandingRecallRequestDoesNotChangeItemStatus()
    throws InterruptedException,
    ExecutionException,
    TimeoutException,
    MalformedURLException,
    UnsupportedEncodingException {

    UUID id = UUID.randomUUID();

    UUID itemId = itemsFixture.basedUponSmallAngryPlanet().getId();

    UUID loanId = loansFixture.checkOutItem(itemId).getId();

    requestsClient.create(new RequestBuilder()
      .recall()
      .withId(id)
      .withItemId(itemId)
      .withRequesterId(usersClient.create(new UserBuilder()).getId()));

    renewLoan(loanId, loansClient);

    Response changedItem = itemsClient.getById(itemId);

    assertThat(changedItem.getJson().getJsonObject("status").getString("name"),
      is("Checked out - Recalled"));
  }

  @Test
  public void RenewalWithOutstandingPageRequestDoesNotChangeItemStatus()
    throws InterruptedException,
    ExecutionException,
    TimeoutException,
    MalformedURLException,
    UnsupportedEncodingException {

    UUID id = UUID.randomUUID();

    UUID itemId = itemsFixture.basedUponSmallAngryPlanet().getId();

    UUID loanId = loansFixture.checkOutItem(itemId).getId();

    requestsClient.create(new RequestBuilder()
      .page()
      .withId(id)
      .withItemId(itemId)
      .withRequesterId(usersClient.create(new UserBuilder()).getId()));

    renewLoan(loanId, loansClient);

    Response changedItem = itemsClient.getById(itemId);

    assertThat(changedItem.getJson().getJsonObject("status").getString("name"),
      is("Checked out"));
  }
}

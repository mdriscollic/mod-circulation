package org.folio.circulation.domain;

import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.folio.circulation.support.*;

import java.util.concurrent.CompletableFuture;

import static org.folio.circulation.domain.RequestStatus.OPEN_AWAITING_PICKUP;
import static org.folio.circulation.domain.representations.LoanProperties.ITEM_ID;
import static org.folio.circulation.support.HttpResult.failure;
import static org.folio.circulation.support.HttpResult.success;

public class LoanValidation {
  private static final String ITEM_BARCODE_PROPERTY_NAME = "itemBarcode";
  public static final String USER_BARCODE_PROPERTY_NAME = "userBarcode";
  private static final String PROXY_USER_BARCODE_PROPERTY_NAME = "proxyUserBarcode";

  private LoanValidation() { }

  public static HttpResult<LoanAndRelatedRecords> refuseWhenItemDoesNotExist(
    HttpResult<LoanAndRelatedRecords> result) {

    return result.next(loan -> {
      if(loan.getLoan().getItem().isNotFound()) {
        final String itemId = loan.getLoan().getItemId();

        return failure(ValidationErrorFailure.failure(
          "Item does not exist", ITEM_ID, itemId));
      }
      else {
        return result;
      }
    });
  }

  public static HttpResult<LoanAndRelatedRecords> refuseWhenItemBarcodeDoesNotExist(
    HttpResult<LoanAndRelatedRecords> result, String barcode) {

    return result.next(loanAndRelatedRecords -> {
      if(loanAndRelatedRecords.getLoan().getItem().isNotFound()) {
        return failure(ValidationErrorFailure.failure(
          String.format("No item with barcode %s exists", barcode),
          ITEM_BARCODE_PROPERTY_NAME, barcode));
      }
      else {
        return result;
      }
    });
  }

  public static void defaultStatusAndAction(JsonObject loan) {
    if(!loan.containsKey("status")) {
      loan.put("status", new JsonObject().put("name", "Open"));

      if(!loan.containsKey("action")) {
        loan.put("action", "checkedout");
      }
    }
  }


  public static HttpResult<LoanAndRelatedRecords> refuseWhenRequestingUserIsInactive(
    HttpResult<LoanAndRelatedRecords> loanAndRelatedRecords, String barcode) {

    return loanAndRelatedRecords.next(loan -> {
      try {
        final User requestingUser = loan.getLoan().getUser();

        if (requestingUser.canDetermineStatus()) {
          return failure(ValidationErrorFailure.failure(
            "Cannot determine if user is active or not",
            USER_BARCODE_PROPERTY_NAME, barcode));
        }
        if (requestingUser.isInactive()) {
          return failure(ValidationErrorFailure.failure(
            "Cannot check out to inactive user",
            USER_BARCODE_PROPERTY_NAME, barcode));
        } else {
          return success(loan);
        }
      } catch (Exception e) {
        return failure(new ServerErrorFailure(e));
      }
    });
  }

  public static HttpResult<LoanAndRelatedRecords> refuseWhenProxyingUserIsInactive(
    HttpResult<LoanAndRelatedRecords> loanAndRelatedRecords, String barcode) {

    return loanAndRelatedRecords.next(loan -> {
      final User proxyingUser = loan.getProxyingUser();

      if(proxyingUser == null) {
        return loanAndRelatedRecords;
      }
      else if (proxyingUser.canDetermineStatus()) {
        return failure(ValidationErrorFailure.failure(
          "Cannot determine if proxying user is active or not",
          PROXY_USER_BARCODE_PROPERTY_NAME, barcode));
      }
      else if(proxyingUser.isInactive()) {
        return failure(ValidationErrorFailure.failure(
          "Cannot check out via inactive proxying user",
          PROXY_USER_BARCODE_PROPERTY_NAME, barcode));
      }
      else {
        return loanAndRelatedRecords;
      }
    });
  }

  public static CompletableFuture<HttpResult<LoanAndRelatedRecords>> refuseWhenHasOpenLoan(
    LoanAndRelatedRecords loanAndRelatedRecords,
    LoanRepository loanRepository,
    String barcode) {

    final String itemId = loanAndRelatedRecords.getLoan().getItemId();

    return loanRepository.hasOpenLoan(itemId)
      .thenApply(r -> r.next(openLoan -> {
        if(openLoan) {
          return failure(ValidationErrorFailure.failure(
            "Cannot check out item that already has an open loan",
            ITEM_BARCODE_PROPERTY_NAME, barcode));
        }
        else {
          return success(loanAndRelatedRecords);
        }
      }));
  }

  public static HttpResult<LoanAndRelatedRecords> refuseWhenItemIsAlreadyCheckedOut(
    HttpResult<LoanAndRelatedRecords> loanAndRelatedRecords) {

    return loanAndRelatedRecords.next(loan -> {
      final Item records = loan.getLoan().getItem();

      if(records.isCheckedOut()) {
        return failure(ValidationErrorFailure.failure(
          "Item is already checked out", "itemId", records.getItemId()));
      }
      else {
        return loanAndRelatedRecords;
      }
    });
  }

  public static HttpResult<LoanAndRelatedRecords> refuseWhenItemIsAlreadyCheckedOut(
    HttpResult<LoanAndRelatedRecords> loanAndRelatedRecords, String barcode) {

    //TODO: Extract duplication with above
    return loanAndRelatedRecords.next(loan -> {
      if(loan.getLoan().getItem().isCheckedOut()) {
        return failure(ValidationErrorFailure.failure(
          "Item is already checked out", ITEM_BARCODE_PROPERTY_NAME, barcode));
      }
      else {
        return loanAndRelatedRecords;
      }
    });
  }
}

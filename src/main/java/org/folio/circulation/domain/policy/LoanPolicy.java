package org.folio.circulation.domain.policy;

import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.folio.circulation.domain.Loan;
import org.folio.circulation.support.HttpResult;
import org.folio.circulation.support.ServerErrorFailure;
import org.folio.circulation.support.ValidationErrorFailure;
import org.folio.circulation.support.http.server.ValidationError;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import static org.folio.circulation.support.HttpResult.failure;
import static org.folio.circulation.support.JsonPropertyFetcher.*;

public class LoanPolicy {
  private final JsonObject representation;
  private final FixedDueDateSchedules fixedDueDateSchedules;
  private final FixedDueDateSchedules alternateRenewalFixedDueDateSchedules;

  private LoanPolicy(JsonObject representation) {
    this(representation,
      new NoFixedDueDateSchedules(),
      new NoFixedDueDateSchedules());
  }

  LoanPolicy(
    JsonObject representation,
    FixedDueDateSchedules fixedDueDateSchedules,
    FixedDueDateSchedules alternateRenewalFixedDueDateSchedules) {

    this.representation = representation;
    this.fixedDueDateSchedules = fixedDueDateSchedules;
    this.alternateRenewalFixedDueDateSchedules = alternateRenewalFixedDueDateSchedules;
  }

  static LoanPolicy from(JsonObject representation) {
    return new LoanPolicy(representation);
  }

  //TODO: make this have similar signature to renew
  public HttpResult<DateTime> calculateInitialDueDate(Loan loan) {
    return determineStrategy(false, null).calculateDueDate(loan);
  }

  public HttpResult<Loan> renew(Loan loan, DateTime systemDate) {
    //TODO: Create HttpResult wrapper that traps exceptions
    try {
      final HttpResult<DateTime> proposedDueDateResult =
        determineStrategy(true, systemDate)
          .calculateDueDate(loan);

      List<ValidationError> errors = new ArrayList<>();

      //TODO: Need a more elegent way of combining validation errors
      if(proposedDueDateResult.failed()) {
        if (proposedDueDateResult.cause() instanceof ValidationErrorFailure) {
          ValidationErrorFailure failureCause =
            (ValidationErrorFailure) proposedDueDateResult.cause();

          errors.addAll(failureCause.getErrors());
        }
      }
      else {
        errorWhenEarlierOrSameDueDate(loan, proposedDueDateResult.value(), errors);
      }

      errorWhenReachedRenewalLimit(loan, errors);

      if(errors.isEmpty()) {
        return proposedDueDateResult.map(dueDate -> loan.renew(dueDate, getId()));
      }
      else {
        return HttpResult.failure(new ValidationErrorFailure(errors));
      }
    }
    catch(Exception e) {
      return failure(new ServerErrorFailure(e));
    }
  }

  private void errorWhenReachedRenewalLimit(Loan loan, List<ValidationError> errors) {
    if(!unlimitedRenewals() && reachedNumberOfRenewalsLimit(loan)) {
      errors.add(new ValidationError(
        "loan has reached it's maximum number of renewals",
        "loanPolicyId", getId()));
    }
  }

  private void errorWhenEarlierOrSameDueDate(
    Loan loan,
    DateTime proposedDueDate,
    List<ValidationError> errors) {

    final ValidationError dueDateError = new ValidationError(
      "renewal at this time would not change the due date",
      "loanPolicyId", getId());

    if(proposedDueDate.isEqual(loan.getDueDate())) {
      errors.add(dueDateError);
    }
    else if(proposedDueDate.isBefore(loan.getDueDate())) {
      errors.add(dueDateError);
    }
  }

  private boolean reachedNumberOfRenewalsLimit(Loan loan) {
    return loan.getRenewalCount() >= getRenewalLimit();
  }

  private boolean unlimitedRenewals() {
    return getBooleanProperty(getRenewalsPolicy(), "unlimited");
  }

  private Integer getRenewalLimit() {
    return getIntegerProperty(getRenewalsPolicy(), "numberAllowed", null);
  }

  private DueDateStrategy determineStrategy(boolean isRenewal, DateTime systemDate) {
    final JsonObject loansPolicy = getLoansPolicy();
    final JsonObject renewalsPolicy = getRenewalsPolicy();

    //TODO: Temporary until have better logic for missing loans policy
    if(loansPolicy == null) {
      return new UnknownDueDateStrategy(getId(), getName(), "", isRenewal);
    }

    if(isRolling(loansPolicy)) {
      if(isRenewal) {
        return new RollingRenewalDueDateStrategy(getId(), getName(),
          systemDate, getRenewFrom(), getRenewalPeriod(loansPolicy, renewalsPolicy),
          getRenewalDueDateLimitSchedules());
      }
      else {
        return new RollingCheckOutDueDateStrategy(getId(), getName(),
          getPeriod(loansPolicy), fixedDueDateSchedules);
      }
    }
    else if(isFixed(loansPolicy)) {
      if(isRenewal) {
        return new FixedScheduleRenewalDueDateStrategy(getId(), getName(),
          getRenewalFixedDueDateSchedules(), systemDate);
      }
      else {
        return new FixedScheduleCheckOutDueDateStrategy(getId(), getName(),
          fixedDueDateSchedules);
      }
    }
    else {
      return new UnknownDueDateStrategy(getId(), getName(),
        getProfileId(loansPolicy), isRenewal);
    }
  }

  private JsonObject getLoansPolicy() {
    return representation.getJsonObject("loansPolicy");
  }

  private JsonObject getRenewalsPolicy() {
    return representation.getJsonObject("renewalsPolicy");
  }

  private FixedDueDateSchedules getRenewalDueDateLimitSchedules() {
    return useDifferentPeriod()
      ? alternateRenewalFixedDueDateSchedules
      : fixedDueDateSchedules;
  }

  private Period getRenewalPeriod(
    JsonObject loansPolicy,
    JsonObject renewalsPolicy) {

    return useDifferentPeriod()
      ? getPeriod(renewalsPolicy)
      : getPeriod(loansPolicy);
  }

  private Period getPeriod(JsonObject policy) {
    String interval = getNestedStringProperty(policy, "period", "intervalId");
    Integer duration = getNestedIntegerProperty(policy, "period", "duration");
    return Period.from(duration, interval);
  }

  private Boolean useDifferentPeriod() {
    return getBooleanProperty(getRenewalsPolicy(), "differentPeriod");
  }

  private String getRenewFrom() {
    return getNestedStringProperty(representation, "renewalsPolicy", "renewFromId");
  }

  private FixedDueDateSchedules getRenewalFixedDueDateSchedules() {
    return useDifferentPeriod()
      ? alternateRenewalFixedDueDateSchedules
      : fixedDueDateSchedules;
  }

  private String getProfileId(JsonObject loansPolicy) {
    return loansPolicy.getString("profileId");
  }

  private String getName() {
    return representation.getString("name");
  }

  private boolean isFixed(JsonObject loansPolicy) {
    return isProfile(loansPolicy, "Fixed");
  }

  private boolean isRolling(JsonObject loansPolicy) {
    return isProfile(loansPolicy, "Rolling");
  }

  private boolean isProfile(JsonObject loansPolicy, String profileId) {
    return StringUtils.equalsIgnoreCase(getProfileId(loansPolicy), profileId);
  }

  LoanPolicy withDueDateSchedules(FixedDueDateSchedules loanSchedules) {
    return new LoanPolicy(representation, loanSchedules,
      alternateRenewalFixedDueDateSchedules);
  }

  //TODO: potentially remove this, when builder can create class or JSON representation
  LoanPolicy withDueDateSchedules(JsonObject fixedDueDateSchedules) {
    return withDueDateSchedules(FixedDueDateSchedules.from(fixedDueDateSchedules));
  }

  LoanPolicy withAlternateRenewalSchedules(FixedDueDateSchedules renewalSchedules) {
    return new LoanPolicy(representation, fixedDueDateSchedules, renewalSchedules);
  }

  //TODO: potentially remove this, when builder can create class or JSON representation
  LoanPolicy withAlternateRenewalSchedules(JsonObject renewalSchedules) {
    return withAlternateRenewalSchedules(FixedDueDateSchedules.from(renewalSchedules));
  }

  public String getId() {
    return representation.getString("id");
  }

  String getLoansFixedDueDateScheduleId() {
    return getNestedStringProperty(representation, "loansPolicy",
      "fixedDueDateScheduleId");
  }

  String getAlternateRenewalsFixedDueDateScheduleId() {
    return getNestedStringProperty(representation, "renewalsPolicy",
      "alternateFixedDueDateScheduleId");
  }
}

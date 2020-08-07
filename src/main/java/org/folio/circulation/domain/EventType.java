
package org.folio.circulation.domain;

public enum EventType {
  ITEM_CHECKED_OUT,
  ITEM_CHECKED_IN,
  ITEM_DECLARED_LOST,
  ITEM_CLAIMED_RETURNED,
  LOAN_DUE_DATE_CHANGED
}

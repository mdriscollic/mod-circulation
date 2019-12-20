package api.support.http;

import static api.support.APITestContext.getOkapiHeadersFromContext;
import static api.support.http.CqlQuery.noQuery;
import static api.support.http.Limit.limit;
import static api.support.http.Offset.noOffset;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static org.folio.circulation.support.JsonArrayHelper.mapToList;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.UUID;

import org.folio.circulation.support.http.client.IndividualResource;
import org.folio.circulation.support.http.client.OkapiHttpClient;
import org.folio.circulation.support.http.client.Response;

import api.support.RestAssuredClient;
import api.support.builders.Builder;
import io.vertx.core.json.JsonObject;

public class ResourceClient {
  private final OkapiHttpClient client;
  private final RestAssuredClient restAssuredClient;
  private final UrlMaker urlMaker;
  private final String resourceName;
  private final String collectionArrayPropertyName;

  public static ResourceClient forItems(OkapiHttpClient client) {
    return new ResourceClient(client, InterfaceUrls::itemsStorageUrl,
      "items");
  }

  public static ResourceClient forHoldings(OkapiHttpClient client) {
    return new ResourceClient(client, InterfaceUrls::holdingsStorageUrl,
      "holdingsRecords");
  }

  public static ResourceClient forInstances(OkapiHttpClient client) {
    return new ResourceClient(client, InterfaceUrls::instancesStorageUrl,
      "instances");
  }

  public static ResourceClient forRequests(OkapiHttpClient client) {
    return new ResourceClient(client, InterfaceUrls::requestsUrl,
      "requests");
  }

  public static ResourceClient forRequestReport(OkapiHttpClient client) {
    return new ResourceClient(client, InterfaceUrls::requestReportUrl,
      "requestReport");
  }

  public static ResourceClient forItemsInTransitReport(OkapiHttpClient client) {
    return new ResourceClient(client,
      subPath -> InterfaceUrls.itemsInTransitReportUrl(), "items");
  }

  public static ResourceClient forPickSlips(OkapiHttpClient client) {
    return new ResourceClient(client,
        InterfaceUrls::pickSlipsUrl, "pickSlips");
  }

  public static ResourceClient forLoans(OkapiHttpClient client) {
    return new ResourceClient(client, InterfaceUrls::loansUrl,
      "loans");
  }

  public static ResourceClient forAccounts(OkapiHttpClient client) {
    return new ResourceClient(client, InterfaceUrls::accountsUrl,
      "accounts");
  }

  public static ResourceClient forFeeFineActions(OkapiHttpClient client) {
    return new ResourceClient(client, InterfaceUrls::feeFineActionsUrl, "feefineactions");
  }

  public static ResourceClient forLoanPolicies(OkapiHttpClient client) {
    return new ResourceClient(client, InterfaceUrls::loanPoliciesStorageUrl,
      "loan policies", "loanPolicies");
  }

  public static ResourceClient forRequestPolicies(OkapiHttpClient client) {
    return new ResourceClient(client, InterfaceUrls::requestPoliciesStorageUrl,
      "request policies", "requestPolicies");
  }

  public static ResourceClient forNoticePolicies(OkapiHttpClient client) {
    return new ResourceClient(client, InterfaceUrls::noticePoliciesStorageUrl,
      "notice policies", "noticePolicies");
  }

  public static ResourceClient forOverdueFinePolicies(OkapiHttpClient client) {
    return new ResourceClient(client, InterfaceUrls::overdueFinesPoliciesStorageUrl,
      "overdue fines policies", "overdueFinePolicies");
  }

  public static ResourceClient forLostItemFeePolicies(OkapiHttpClient client) {
    return new ResourceClient(client, InterfaceUrls::lostItemFeesPoliciesStorageUrl,
      "lost item fee policies", "lostItemFeePolicies");
  }

  public static ResourceClient forFixedDueDateSchedules(OkapiHttpClient client) {
    return new ResourceClient(client, InterfaceUrls::fixedDueDateSchedulesStorageUrl,
      "fixed due date schedules", "fixedDueDateSchedules");
  }

  public static ResourceClient forCirculationRules(OkapiHttpClient client) {
    return new ResourceClient(client, InterfaceUrls::circulationRulesStorageUrl,
      "circulation rules", "circulationRules");
  }

  public static ResourceClient forUsers(OkapiHttpClient client) {
    return new ResourceClient(client, InterfaceUrls::usersUrl,
      "users");
  }

  public static ResourceClient forCalendar(OkapiHttpClient client) {
    return new ResourceClient(client, InterfaceUrls::calendarUrl,
      "calendar", "calendars");
  }

  public static ResourceClient forProxyRelationships(OkapiHttpClient client) {
    return new ResourceClient(client, InterfaceUrls::proxyRelationshipsUrl,
      "proxiesFor");
  }

  public static ResourceClient forPatronGroups(OkapiHttpClient client) {
    return new ResourceClient(client, InterfaceUrls::patronGroupsStorageUrl,
      "patron groups", "usergroups");
  }

  public static ResourceClient forAddressTypes(OkapiHttpClient client) {
    return new ResourceClient(client, InterfaceUrls::addressTypesUrl,
      "address types", "addressTypes");
  }

  public static ResourceClient forLoansStorage(OkapiHttpClient client) {
    return new ResourceClient(client, InterfaceUrls::loansStorageUrl,
      "storage loans", "loans");
  }

  public static ResourceClient forRequestsStorage(OkapiHttpClient client) {
    return new ResourceClient(client, InterfaceUrls::requestStorageUrl,
      "storage requests", "requests");
  }

  public static ResourceClient forMaterialTypes(OkapiHttpClient client) {
    return new ResourceClient(client, InterfaceUrls::materialTypesStorageUrl,
      "material types", "mtypes");
  }

  public static ResourceClient forUserManualBlocks(OkapiHttpClient client) {
    return new ResourceClient(client, subPath ->
      InterfaceUrls.userManualBlocksStorageUrl(), "manualblocks", "manualblocks");
  }

  public static ResourceClient forLoanTypes(OkapiHttpClient client) {
    return new ResourceClient(client, InterfaceUrls::loanTypesStorageUrl,
      "loan types", "loantypes");
  }

  public static ResourceClient forInstanceTypes(OkapiHttpClient client) {
    return new ResourceClient(client, InterfaceUrls::instanceTypesStorageUrl,
      "instance types", "instanceTypes");
  }

  public static ResourceClient forContributorNameTypes(OkapiHttpClient client) {
    return new ResourceClient(client, InterfaceUrls::contributorNameTypesStorageUrl,
      "contributor name types", "contributorNameTypes");
  }

  public static ResourceClient forInstitutions(OkapiHttpClient client) {
    return new ResourceClient(client, InterfaceUrls::institutionsStorageUrl,
      "institutions", "locinsts");
  }

  public static ResourceClient forCampuses(OkapiHttpClient client) {
    return new ResourceClient(client, InterfaceUrls::campusesStorageUrl,
      "campuses", "loccamps");
  }

  public static ResourceClient forLibraries(OkapiHttpClient client) {
    return new ResourceClient(client, InterfaceUrls::librariesStorageUrl,
      "libraries", "loclibs");
  }

  public static ResourceClient forLocations(OkapiHttpClient client) {
    return new ResourceClient(client, InterfaceUrls::locationsStorageUrl,
      "locations");
  }

  public static ResourceClient forCancellationReasons(OkapiHttpClient client) {
    return new ResourceClient(client, InterfaceUrls::cancellationReasonsStorageUrl,
      "cancellationReasons");
  }

  public static ResourceClient forServicePoints(OkapiHttpClient client) {
    return new ResourceClient(client, InterfaceUrls::servicePointsStorageUrl,
      "service points", "servicepoints");
  }

  public static ResourceClient forPatronNotices(OkapiHttpClient client) {
    return new ResourceClient(client, InterfaceUrls::patronNoticesUrl,
      "patron notice", "patronnotices");
  }

  public static ResourceClient forScheduledNotices(OkapiHttpClient client) {
    return new ResourceClient(client, InterfaceUrls::scheduledNoticesUrl,
      "scheduled notice", "scheduledNotices");
  }

  public static ResourceClient forPatronSessionRecords(OkapiHttpClient client) {
    return new ResourceClient(client, InterfaceUrls::patronActionSessionsUrl,
      "patron session records", "patronActionSessions");
  }

  public static ResourceClient forExpiredSessions(OkapiHttpClient client) {
    return new ResourceClient(client, InterfaceUrls::patronExpiredSessionsUrl,
      "expired session records");
  }

  public static ResourceClient forConfiguration(OkapiHttpClient client) {
    return new ResourceClient(client, InterfaceUrls::configurationUrl,
      "configuration entries", "configs");
  }

  public static ResourceClient forRenewByBarcode(OkapiHttpClient client) {
    return new ResourceClient(client, subPath -> InterfaceUrls.renewByBarcodeUrl(),
      "renew by barcode");
  }

  public static ResourceClient forRenewById(OkapiHttpClient client) {
    return new ResourceClient(client, subPath -> InterfaceUrls.renewByIdUrl(),
      "renew by id");
  }

  public static ResourceClient forOverrideRenewalByBarcode(OkapiHttpClient client) {
    return new ResourceClient(
      client, subPath -> InterfaceUrls.overrideRenewalByBarcodeUrl(),
      "override renewal by barcode"
    );
  }

  private ResourceClient(OkapiHttpClient client, UrlMaker urlMaker,
      String resourceName, String collectionArrayPropertyName) {

    this.client = client;
    this.urlMaker = urlMaker;
    this.resourceName = resourceName;
    this.collectionArrayPropertyName = collectionArrayPropertyName;
    restAssuredClient = new RestAssuredClient(getOkapiHeadersFromContext());
  }

  private ResourceClient(OkapiHttpClient client, UrlMaker urlMaker,
    String resourceName) {

    this(client, urlMaker, resourceName, resourceName);
  }

  public Response attemptCreate(Builder builder) throws MalformedURLException {
    return attemptCreate(builder.create());
  }

  public Response attemptCreate(JsonObject representation)
      throws MalformedURLException {

    return restAssuredClient.post(representation, rootUrl(),
        "attempt-create-record");
  }

  public IndividualResource create(Builder builder) throws MalformedURLException {
    return create(builder.create());
  }

  public IndividualResource create(JsonObject representation)
      throws MalformedURLException {

    return  new IndividualResource(restAssuredClient.post(representation,
      rootUrl(), 201, "create-record"));
  }

  public Response attemptCreateAtSpecificLocation(Builder builder)
      throws MalformedURLException {

    final JsonObject representation = builder.create();
    final URL location = recordUrl(representation.getString("id"));

    return restAssuredClient.put(representation, location,
      "attempt-create-record-at-specific-location");
  }

  public IndividualResource createAtSpecificLocation(Builder builder)
      throws MalformedURLException {

    final JsonObject representation = builder.create();
    final URL location = recordUrl(representation.getString("id"));

    restAssuredClient.put(representation, location, HTTP_NO_CONTENT,
      "create-record-at-specific-location");

    return get(location);
  }

  public Response attemptReplace(UUID id, Builder builder)
      throws MalformedURLException {

    return attemptReplace(id, builder.create());
  }

  public Response attemptReplace(UUID id, JsonObject representation)
      throws MalformedURLException {

    return restAssuredClient.put(representation, recordUrl(id),
      "attempt-replace-record");
  }

  public void replace(UUID id, Builder builder) throws MalformedURLException {
    replace(id, builder.create());
  }

  public void replace(UUID id, JsonObject representation)
      throws MalformedURLException {

    restAssuredClient.put(representation, recordUrl(id), HTTP_NO_CONTENT,
      "create-record-at-specific-location");
  }

  public Response getById(UUID id) throws MalformedURLException {
    return restAssuredClient.get(recordUrl(id), "get-record");
  }

  public IndividualResource get(IndividualResource record)
      throws MalformedURLException {

    return get(record.getId());
  }

  public IndividualResource get(UUID id) throws MalformedURLException {
    return get(recordUrl(id));
  }

  public IndividualResource get(URL url) {
    return new IndividualResource(restAssuredClient.get(url, 200, "get-record"));
  }

  public Response attemptGet(IndividualResource resource)
      throws MalformedURLException {

    return getById(resource.getId());
  }

  public void delete(UUID id) throws MalformedURLException {
    restAssuredClient.delete(recordUrl(id), HTTP_NO_CONTENT, "delete-record");
  }

  public void delete(IndividualResource resource) throws MalformedURLException {
    delete(resource.getId());
  }

  public void deleteAll() throws MalformedURLException {
    restAssuredClient.delete(rootUrl(), HTTP_NO_CONTENT, "delete-all-records");
  }

  public void deleteAllIndividually() throws MalformedURLException {
    for (JsonObject record : getAll()) {
      delete(UUID.fromString(record.getString("id")));
    }
  }

  //TODO: Replace return valu[e with MultipleJsonRecords
  public List<JsonObject> getAll() throws MalformedURLException {
    final URL location = rootUrl();

    return mapToList(restAssuredClient
      .get(location, noQuery(), limit(1000), noOffset(), 200, "get-all")
      .getJson(), collectionArrayPropertyName);
  }

  private URL recordUrl(Object id) throws MalformedURLException {
    return urlMaker.combine(String.format("/%s", id));
  }

  private URL rootUrl() throws MalformedURLException {
    return urlMaker.combine("");
  }

  @FunctionalInterface
  public interface UrlMaker {
    URL combine(String subPath) throws MalformedURLException;
  }
}

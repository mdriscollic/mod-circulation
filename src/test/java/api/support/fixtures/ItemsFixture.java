package api.support.fixtures;

import static api.APITestSuite.booksInstanceTypeId;
import static api.APITestSuite.thirdFloorLocationId;
import static java.util.function.Function.identity;

import java.net.MalformedURLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import org.folio.circulation.support.http.client.IndividualResource;
import org.folio.circulation.support.http.client.OkapiHttpClient;

import api.support.builders.HoldingBuilder;
import api.support.builders.InstanceBuilder;
import api.support.builders.ItemBuilder;
import api.support.http.ResourceClient;

public class ItemsFixture {

  private final ResourceClient itemsClient;
  private final ResourceClient holdingsClient;
  private final ResourceClient instancesClient;

  public ItemsFixture(OkapiHttpClient client) {
    itemsClient = ResourceClient.forItems(client);
    holdingsClient = ResourceClient.forHoldings(client);
    instancesClient = ResourceClient.forInstances(client);
  }

  public IndividualResource basedUponDunkirk()
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    return create(
      InstanceExamples.basedUponDunkirk(),
      thirdFloorHoldings(),
      ItemExamples.basedUponDunkirk());
  }

  public IndividualResource basedUponSmallAngryPlanet()
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    return basedUponSmallAngryPlanet(identity());
  }

  public IndividualResource basedUponSmallAngryPlanet(
    Function<ItemBuilder, ItemBuilder> additionalItemProperties)
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    return basedUponSmallAngryPlanet(
      identity(),
      additionalItemProperties);
  }

  public IndividualResource basedUponSmallAngryPlanet(
    Function<HoldingBuilder, HoldingBuilder> additionalHoldingsRecordProperties,
    Function<ItemBuilder, ItemBuilder> additionalItemProperties)
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    return applyAdditionalProperties(
      additionalHoldingsRecordProperties,
      additionalItemProperties,
      InstanceExamples.basedUponSmallAngryPlanet(),
      thirdFloorHoldings(),
      ItemExamples.basedUponSmallAngryPlanet());
  }

  public IndividualResource basedUponNod()
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    return basedUponNod(identity());
  }

  public IndividualResource basedUponNod(
    Function<ItemBuilder, ItemBuilder> additionalItemProperties)
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    return applyAdditionalProperties(
      identity(),
      additionalItemProperties,
      InstanceExamples.basedUponNod(),
      thirdFloorHoldings(),
      ItemExamples.basedUponNod());
  }

  public IndividualResource basedUponTemeraire()
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    return basedUponTemeraire(identity());
  }

  public IndividualResource basedUponTemeraire(
    Function<HoldingBuilder, HoldingBuilder> additionalHoldingsRecordProperties,
    Function<ItemBuilder, ItemBuilder> additionalItemProperties)
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    return applyAdditionalProperties(
      additionalHoldingsRecordProperties,
      additionalItemProperties,
      InstanceExamples.basedUponTemeraire(),
      thirdFloorHoldings(),
      ItemExamples.basedUponTemeraire());
  }

  public IndividualResource basedUponTemeraire(
    Function<ItemBuilder, ItemBuilder> additionalItemProperties)
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    return basedUponTemeraire(identity(), additionalItemProperties);
  }

  public IndividualResource basedUponUprooted()
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    return basedUponUprooted(identity());
  }

  public IndividualResource basedUponUprooted(
    Function<ItemBuilder, ItemBuilder> additionalItemProperties)
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    return applyAdditionalProperties(
      identity(),
      additionalItemProperties,
      InstanceExamples.basedUponUprooted(),
      thirdFloorHoldings(),
      ItemExamples.basedUponUprooted());
  }

  public IndividualResource basedUponInterestingTimes()
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    return basedUponInterestingTimes(identity());
  }

  public IndividualResource basedUponInterestingTimes(
    Function<ItemBuilder, ItemBuilder> additionalItemProperties)
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    return applyAdditionalProperties(
      identity(),
      additionalItemProperties,
      InstanceExamples.basedUponInterestingTimes(),
      thirdFloorHoldings(),
      ItemExamples.basedUponInterestingTimes());
  }

  private IndividualResource applyAdditionalProperties(
    Function<HoldingBuilder, HoldingBuilder> additionalHoldingsRecordProperties,
    Function<ItemBuilder, ItemBuilder> additionalItemProperties,
    InstanceBuilder instanceBuilder,
    HoldingBuilder holdingsRecordBuilder,
    ItemBuilder itemBuilder)
    throws InterruptedException,
    MalformedURLException,
    TimeoutException,
    ExecutionException {

    return create(
      instanceBuilder,
      additionalHoldingsRecordProperties.apply(holdingsRecordBuilder),
      additionalItemProperties.apply(itemBuilder));
  }

  private IndividualResource create(
    InstanceBuilder instanceBuilder,
    HoldingBuilder holdingsRecordBuilder,
    ItemBuilder itemBuilder)
    throws MalformedURLException,
    InterruptedException,
    ExecutionException,
    TimeoutException {

    IndividualResource instance = instancesClient.create(
      instanceBuilder.withInstanceTypeId(booksInstanceTypeId()));

    IndividualResource holding = holdingsClient.create(
      holdingsRecordBuilder.forInstance(instance.getId()));

    return itemsClient.create(
      itemBuilder.forHolding(holding.getId())
      .create());
  }

  private HoldingBuilder thirdFloorHoldings() {
    return new HoldingBuilder()
      .withPermanentLocation(thirdFloorLocationId())
      .withNoTemporaryLocation()
      .withCallNumber("123456");
  }
}

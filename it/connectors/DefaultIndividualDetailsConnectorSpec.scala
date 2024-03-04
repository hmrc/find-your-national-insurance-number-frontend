

package connectors

import connectors._
import models._
import models.individualdetails._
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.{DefaultAwaitTimeout, Injecting}
import uk.gov.hmrc.http.client.HttpClientV2
import base.WireMockHelper
import config.{DesApiServiceConfig, FrontendAppConfig}
import models.IndividualDetailsResponseEnvelope.IndividualDetailsResponseEnvelope
import org.mockito.MockitoSugar.{mock, when}
import org.mockito.MockitoSugar.when
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import java.util.UUID
import scala.util.Random




class DefaultIndividualDetailsConnectorSpec
  extends ConnectorSpec
    with WireMockHelper
    with MockitoSugar
    with DefaultAwaitTimeout
    with Injecting {


  val mockHttpClient: HttpClient = mock[HttpClient]
  val config: FrontendAppConfig = mock[FrontendAppConfig]
  val metrics: Metrics = mock[Metrics]



  override implicit lazy val app: play.api.Application = new GuiceApplicationBuilder()
    .overrides(
      bind[HttpClient].toInstance(mockHttpClient),
      bind[FrontendAppConfig].toInstance(config),
      bind[Metrics].toInstance(metrics)
    )
    .configure("external-url.individual-details-service.port" -> server.port())
    .build()


  val identifier:IndividualDetailsIdentifier = IndividualDetailsNino(new Generator(new Random()).nextNino.nino)
  val resolveMerge = ResolveMerge('Y')

  trait SpecSetup {
    def url(identifier: IndividualDetailsIdentifier, resolveMerge: ResolveMerge): String

    lazy val connector = {
      val httpClient = app.injector.instanceOf[HttpClient]
      val config = app.injector.instanceOf[FrontendAppConfig]
      val metrics = app.injector.instanceOf[Metrics]
      new DefaultIndividualDetailsConnector(httpClient, config, metrics)
    }
  }

  "DefaultIndividualDetailsConnector" must {

    trait LocalSetup extends SpecSetup {
      def url(identifier: IndividualDetailsIdentifier, resolveMerge: ResolveMerge) = s"/individuals/details/NINO/${identifier.value}/${resolveMerge.value}"
    }

    "return IndividualDetails when called with a valid identifier and resolveMerge" in new LocalSetup {


      when(config.individualDetailsServiceUrl)
        .thenReturn(s"http://localhost:${server.port()}")



      implicit val correlationId = CorrelationId(UUID.randomUUID())
      stubGet(url(identifier, resolveMerge), OK, Some(""))
      val result = connector.getIndividualDetails(identifier, resolveMerge)
      result mustBe a[IndividualDetailsResponseEnvelope[IndividualDetails]]
    }

  }
}
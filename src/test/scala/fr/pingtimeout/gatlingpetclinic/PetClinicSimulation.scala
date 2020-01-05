package fr.pingtimeout.gatlingpetclinic

import com.typesafe.scalalogging.StrictLogging
import io.gatling.core.Predef._
import io.gatling.commons.validation._
import io.gatling.core.session.Expression
import io.gatling.http.Predef._

import scala.util.Random

class PetClinicSimulation extends Simulation {
  private val randomSource = RandomSource()
  private val randomParameters = RandomParameters(randomSource)

  private val httpProtocol = http
    .baseUrl("http://localhost:8080/")
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .userAgentHeader("User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:71.0) Gecko/20100101 Firefox/71.0")

  val scn = scenario("Spring Petclinic")

    .exec(http("Go to landing page")
      .get("/"))

    .exec(http("Go to find owners page")
      .get("/owners/find"))

    .exec(http("Go to new owner page")
      .get("/owners/new"))

    .exec(http("Create an owner")
      .post("/owners/new")
      .formParam("firstName", randomParameters.fromScalaRandom(r => r.nextString(10) + " " + r.nextString(10)))
      .formParam("lastName", randomParameters.fromScalaRandom(_.nextString(15)))
      .formParam("address", randomParameters.fromScalaRandom(_.nextString(50)))
      .formParam("city", randomParameters.fromScalaRandom(_.nextString(20)))
      .formParam("telephone", randomParameters.nextNumericString(10)))

    .exec(http("Go to find owners page")
      .get("/owners/find"))

    .exec(http("List all owners")
      .get("/owners?lastName=")
      .check(css("table#owners a", "href")
        .findAll
        .transform(owners => randomSource.nextElement(owners).split('/')(2))
        .saveAs("ownerId")))

    .exec(http("Go to owner page")
      .get("/owners/${ownerId}"))

    .exec(http("Go to the new pet page")
      .get("/owners/${ownerId}/pets/new"))

    .exec(http("Create a new pet")
      .post("/owners/${ownerId}/pets/new")
      .formParam("name", randomParameters.fromScalaRandom(_.nextString(5)))
      .formParam("birthDate", randomParameters.nextDate())
      .formParam("type", randomParameters.nextElement(Seq("bird", "cat", "dog", "hamster", "lizard", "snake"))))

    .exec(http("Go to owner page")
      .get("/owners/${ownerId}")
      .check(css("a[href$='/visits/new']", "href")
        .findAll
        .transform(visits => randomSource.nextElement(visits).split('/')(2))
        .saveAs("petId")))

    .exec(http("Go to the new visit page for ${ownerId}/${petId}")
      .get("/owners/${ownerId}/pets/${petId}/visits/new"))

    .exec(http("Create a new visit for pet ${petId}")
      .post("/owners/${ownerId}/pets/${petId}/visits/new")
      .formParam("date", randomParameters.nextDate())
      .formParam("description", randomParameters.fromScalaRandom(_.nextString(50)))
      .formParam("petId", "${petId}"))

  setUp(scn.inject(atOnceUsers(2)).protocols(httpProtocol))
}

case class RandomSource(seed: Long = System.currentTimeMillis()) {
  val random: Random = new Random(seed)

  def nextDate(): String = {
    val year = 2010 + random.nextInt(10) // between 2010 and 2020
    val month = random.nextInt(12)
    val day = random.nextInt(12)
    f"$year%04d-$month%02d-$day%02d"
  }

  def nextNumericString(length: Int): String = {
    val bytes = new Array[Byte](length)
    random.nextBytes(bytes)
    bytes.map(b => Math.abs(b % 10)).mkString("")
  }

  def nextElement[E](list: Seq[E]): E =
    list(random.nextInt(list.size))
}

case class RandomParameters(randomSource: RandomSource) extends StrictLogging {
  def nextDate(): Expression[String] = _ => randomSource.nextDate().success

  def nextNumericString(length: Int): Expression[String] = _ => randomSource.nextNumericString(length).success

  def nextElement[E](list: Seq[E]): Expression[E] = _ => randomSource.nextElement(list).success

  def fromScalaRandom(generator: Random => Any): Expression[Any] = _ => generator.apply(randomSource.random).success
}
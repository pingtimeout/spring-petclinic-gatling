package fr.pingtimeout.gatlingpetclinic

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.util.Random

class PetClinicSimulation extends Simulation {
  private val randomSource = RandomSource()
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
      .formParam("firstName", randomSource.random.nextString(10) + " " + randomSource.random.nextString(10))
      .formParam("lastName", randomSource.random.nextString(15))
      .formParam("address", randomSource.random.nextString(50))
      .formParam("city", randomSource.random.nextString(20))
      .formParam("telephone", randomSource.nextNumericString(10)))

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
      .formParam("name", randomSource.random.nextString(5))
      .formParam("birthDate", randomSource.nextDate())
      .formParam("type", randomSource.nextElement(Seq("bird", "cat", "dog", "hamster", "lizard", "snake"))))

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
      .formParam("date", randomSource.nextDate())
      .formParam("description", randomSource.random.nextString(50))
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
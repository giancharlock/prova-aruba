package com.experis.receiver.load

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import scala.util.Random

class ReceiverLoadTest extends Simulation {

  // 1. Configurazione del protocollo HTTP
  val httpProtocol = http
    .baseUrl("http://localhost:8080") // URL base del servizio receiver
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  // 2. Generatore di dati dinamici (Feeder)
  val invoiceFeeder = Iterator.continually {
    val invoiceNumber = Random.nextInt(Integer.MAX_VALUE)
    val customerId = 2 + Random.nextInt(3) // Genera un ID cliente casuale tra 2, 3, e 4, quelli inseriti nel db
    val test_user = "test_user_" + customerId-1;
    val callbackUrl = s"http://mock-callback-server.com/notify/$customerId/$invoiceNumber"
    val fatturaRandom = test_user + " " + invoiceNumber + Random.nextInt(9999999999);

    // Creiamo il payload JSON direttamente qui
    val jsonPayload = s"""
      {
        "invoiceNumber": $invoiceNumber,
        "invoice": "<xml>Test di carico...</xml>",
        "customer": {
          "customerId": $customerId,
          "username": "$test_user"
        },
        "callback": "$callbackUrl"
      }
      """
    Map("jsonPayload" -> jsonPayload)
  }

  // 3. Definizione degli scenari

  val salvaFatturaInterna = scenario("Salva Fattura Interna")
    .feed(invoiceFeeder)
    .exec(
      http("request_interna")
        .post("/api/salvaFatturaInterna")
        .body(StringBody(session => session("jsonPayload").as[String])).asJson
        .check(status.is(200)) // Ci aspettiamo che la richiesta venga accettata
    )

  val salvaFatturaEsterna = scenario("Salva Fattura Esterna")
    .feed(invoiceFeeder)
    .exec(
      http("request_esterna")
        .post("/api/salvaFatturaEsterna")
        .body(StringBody(session => session("jsonPayload").as[String])).asJson
        .check(status.is(200))
    )

  // 4. Definizione del profilo di carico
  setUp(
    // Eseguiamo i due scenari in parallelo
    salvaFatturaInterna.inject(rampUsers(50).during(10.seconds)),
    salvaFatturaEsterna.inject(rampUsers(50).during(10.seconds))
  ).protocols(httpProtocol)
   .maxDuration(1.minute) // Durata massima del test

}
package com.experis.receiver.load

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import scala.util.Random

class ReceiverLoadInternalInvoiceTest extends Simulation {

  // Configurazione del protocollo HTTP
  val httpProtocol = http
    .baseUrl("http://localhost:8080") // URL base del servizio receiver
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  // Generatore di dati dinamici (Feeder)
  val invoiceFeeder = Iterator.continually {
    val invoiceNumber = Random.nextInt(Integer.MAX_VALUE)
    val customerId = 2 + Random.nextInt(3) // Genera un ID cliente casuale tra 2, 3, e 4, quelli inseriti nel db
    val test_user = "test_user_" + (customerId-1);
    val callbackUrl = s"http://mock-callback-server.com/notify/$customerId/$invoiceNumber"
    val fatturaRandom = test_user + " " + invoiceNumber + Random.nextInt(99999999);

    // Creiamo il payload JSON direttamente qui
    val jsonPayload = s"""
      {
        "invoice": "<xml>Test di carico $fatturaRandom </xml>",
        "customer": {
          "customerId": $customerId,
          "username": "$test_user"
        },
        "callback": "$callbackUrl"
      }
      """
    Map("jsonPayload" -> jsonPayload)
  }

  // Definizione degli scenari

  val salvaFatturaInterna = scenario("Salva Fattura Interna")
    .feed(invoiceFeeder)
    .exec(
      http("request_interna")
        .post("/api/salvaFatturaInterna")
        .body(StringBody(session => session("jsonPayload").as[String])).asJson
        .check(status.is(202)) // Ci aspettiamo che la richiesta venga accettata
    )

  // Definizione del profilo di carico
  setUp(
    // Eseguiamo i due scenari in parallelo
    salvaFatturaInterna.inject(rampUsers(20000).during(10.seconds)),
  ).protocols(httpProtocol)
   .maxDuration(2.minute) // Durata massima del test

}
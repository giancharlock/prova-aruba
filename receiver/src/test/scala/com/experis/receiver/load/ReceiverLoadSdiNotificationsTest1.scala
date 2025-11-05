package com.experis.receiver.load

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._
import scala.util.Random

class ReceiverLoadSdiNotificationsTest1 extends Simulation {

  // Configurazione del protocollo HTTP
  val httpProtocol = http
    .baseUrl("http://localhost:8080") // URL base del servizio receiver
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  // Generatore di dati dinamici (Feeder)
  val invoiceFeeder = Iterator.continually {
    val invoiceNumber = Random.nextInt(Integer.MAX_VALUE)
    val customerId = 1
    val statusVect = List("INTERNAL_INVOICE_DELIVERED", "INTERNAL_INVOICE_DISCARDED", "INTERNAL_INVOICE_NOT_DELIVERED")
    val statusVectIndex = Random.nextInt(2);

    // Creiamo il payload JSON direttamente qui
    val jsonPayload = s"""
      {
        "invoiceNumber": $invoiceNumber,
        "customerId": $customerId,
        "status": $statusVect[$statusVectIndex];
      }
      """
    Map("jsonPayload" -> jsonPayload)
  }

  // Definizione degli scenari

  val sdiNotification = scenario("Salva Fattura Interna")
    .feed(invoiceFeeder)
    .exec(
      http("sdi_notification")
        .post("/api/sdiNotification")
        .body(StringBody(session => session("jsonPayload").as[String])).asJson
        .check(status.is(202)) // Ci aspettiamo che la richiesta venga accettata
    )

  // Definizione del profilo di carico
  setUp(
    // Eseguiamo i due scenari in parallelo
    sdiNotification.inject(rampUsers(2).during(2.seconds)),
  ).protocols(httpProtocol)
   .maxDuration(10.seconds)

}
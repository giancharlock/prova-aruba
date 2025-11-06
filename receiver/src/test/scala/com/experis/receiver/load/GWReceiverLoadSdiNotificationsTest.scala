package com.experis.receiver.load

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._
import scala.util.Random

class GWReceiverLoadSdiNotificationsTest extends Simulation {

  // Configurazione del protocollo HTTP
  val httpProtocol = http
    .baseUrl("http://localhost:8072") // URL base del servizio receiver
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  val sdiToken = "sdi-secret-token"

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
        .post("/receiver/api/sdiNotification")
        .header("X-API-KEY",sdiToken)
        .body(StringBody(session => session("jsonPayload").as[String])).asJson
        .check(status.is(202)) // Ci aspettiamo che la richiesta venga accettata
    )

  // Definizione del profilo di carico
  setUp(
    // Eseguiamo i due scenari in parallelo
    sdiNotification.inject(rampUsers(100).during(10.seconds)),
  ).protocols(httpProtocol)
   .maxDuration(2.minute) // Durata massima del test

}
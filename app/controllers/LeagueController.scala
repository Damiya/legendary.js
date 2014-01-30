/*
 * Copyright 2014 Kate von Roeder (katevonroder at gmail dot com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.duration._
import actions.{AuthenticatedRequest, SecuredAction}
import akka.actor._
import actors.{ConnectionStatus, LeagueClientImpl, LeagueClient}
import models.UserPass
import play.api.libs.json._
import play.api.libs.concurrent.Akka
import scala.concurrent.{Await, Future}
import play.api.Play.current
import akka.pattern.AskableActorSelection
import akka.util.Timeout
import scala.Some
import akka.actor.Identify
import play.Logger

object LeagueController extends Controller {
  private def findOrCreateLeagueClient(username:String): LeagueClient = {
    implicit val timeout = Timeout(5.seconds)
    val potentialActor = Akka.system.actorSelection(s"/user/$username")
    val identifyFuture = new AskableActorSelection(potentialActor).ask(Identify(1))
    val usernameActorRef = Await.result(identifyFuture, timeout.duration).asInstanceOf[ActorIdentity].getRef
    if (usernameActorRef == null) {
      TypedActor(Akka.system).typedActorOf(TypedProps[LeagueClientImpl]().withTimeout(60.seconds), name = username)
    } else {
      TypedActor(Akka.system).typedActorOf(TypedProps[LeagueClientImpl]().withTimeout(60.seconds), usernameActorRef)
    }
  }

  def login() = SecuredAction.async(parse.json) { authenticatedRequest =>
    val loginActor = findOrCreateLeagueClient(authenticatedRequest.user.username)

    authenticatedRequest.request.body.validate[UserPass].asOpt match {
      case Some(user) =>
        loginActor.login(user).map { result =>
          val resultObj = Json.obj(
            "result" -> result
          )
          Ok(resultObj)
        }
      case None =>
        Future.successful(Ok("Ok"))
    }
  }

  def logout() = SecuredAction { authenticatedRequest =>
    val loginActor = findOrCreateLeagueClient(authenticatedRequest.user.username)
    if (loginActor.isConnected) {
      loginActor.logout()
      Ok(Json.obj(
        "result" -> ConnectionStatus.LOGGED_OUT
      ))
    } else {
      Logger.error("Attempted to log out despite not being logged in on the client.")
      BadRequest("Not current logged in.")
    }
  }
}
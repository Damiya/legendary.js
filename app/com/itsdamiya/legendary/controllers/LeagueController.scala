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

package com.itsdamiya.legendary.controllers

import com.itsdamiya.legendary.actors.ConnectionStatus
import play.api.mvc._
import scala.concurrent.Future
import com.itsdamiya.legendary.utils.{MagicStrings, DefaultWebServices}
import com.itsdamiya.legendary.actions.SecuredAction
import play.api.libs.ws.WS
import scala.concurrent.duration._
import play.api.libs.json.{JsValue, Json}
import play.Logger
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import com.itsdamiya.legendary.models.UserPass
import com.itsdamiya.legendary.cache.Cache

object LeagueController extends Controller with DefaultWebServices {
  def login() = SecuredAction.async(parse.json) { authenticatedRequest =>
    val loginActor = authenticatedRequest.userSession.getLeagueConnection

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

  def featuredGames() = SecuredAction.async { authenticatedRequest =>
    Cache.getAs[JsValue]("featuredGames") match {
      case Some(featuredGames) =>
        Logger.debug("Cache hit for featuredGames")
        Future.successful(Ok(featuredGames))
      case None =>
        Logger.debug("Cache miss for featuredGames")
        WS.url(MagicStrings.featuredGamesUrl)
          .withDefaultHeaders().get().map { response =>
          val json = response.json
          val refreshInterval = (json \ "clientRefreshInterval").as[Int]
          Cache.set("featuredGames", json, 0, refreshInterval)
          Ok(json)
        }
    }

  }

  def landingPage() = SecuredAction.async { authenticatedRequest =>
    Cache.getAs[JsValue]("landingPage") match {
      case Some(landingPageInfo) =>
        Logger.debug("Cache hit for landingPage")
        Future.successful(Ok(landingPageInfo))
      case None =>
        Logger.debug("Cache miss for landingPage")
        WS.url(MagicStrings.landingPageUrl)
          .withDefaultHeaders().get().map { response =>
          Cache.set("landingPage", response.json, 2.hours)
          Ok(response.json)
        }
    }
  }

  def logout() = SecuredAction { authenticatedRequest =>
    val loginActor = authenticatedRequest.userSession.getLeagueConnection

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
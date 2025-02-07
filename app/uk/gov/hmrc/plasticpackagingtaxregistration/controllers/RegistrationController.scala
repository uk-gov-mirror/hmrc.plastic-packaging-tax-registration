/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.plasticpackagingtaxregistration.controllers

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.{Json, Writes}
import play.api.mvc._
import uk.gov.hmrc.plasticpackagingtaxregistration.controllers.actions.Authenticator
import uk.gov.hmrc.plasticpackagingtaxregistration.controllers.response.JSONResponses
import uk.gov.hmrc.plasticpackagingtaxregistration.models.RegistrationRequest
import uk.gov.hmrc.plasticpackagingtaxregistration.repositories.RegistrationRepository
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegistrationController @Inject() (
  registrationRepository: RegistrationRepository,
  authenticator: Authenticator,
  override val controllerComponents: ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends BackendController(controllerComponents) with JSONResponses {
  private val logger = Logger(this.getClass)

  def get(id: String): Action[AnyContent] =
    authenticator.authorisedAction(parse.default) { _ =>
      registrationRepository.findByRegistrationId(id).map {
        case Some(registration) => Ok(registration)
        case None               => NotFound
      }
    }

  def create(): Action[RegistrationRequest] =
    authenticator.authorisedAction(authenticator.parsingJson[RegistrationRequest]) { implicit request =>
      logPayload("Create Registration Request Received", request.body)
      registrationRepository
        .create(request.body.toRegistration(request.pptId))
        .map(logPayload("Create Registration Response", _))
        .map(registration => Created(registration))
    }

  def update(id: String): Action[RegistrationRequest] =
    authenticator.authorisedAction(authenticator.parsingJson[RegistrationRequest]) { implicit request =>
      logPayload("Update Registration Request Received", request.body)
      registrationRepository.findByRegistrationId(id).flatMap {
        case Some(_) =>
          registrationRepository
            .update(request.body.toRegistration(request.pptId))
            .map(logPayload("Update Registration Response", _))
            .map {
              case Some(registration) => Ok(registration)
              case None               => NotFound
            }
        case None =>
          logPayload("Update Registration Response", "Not Found")
          Future.successful(NotFound)
      }
    }

  private def logPayload[T](prefix: String, payload: T)(implicit wts: Writes[T]): T = {
    logger.debug(s"Payload: ${Json.toJson(payload)}")
    payload
  }

}

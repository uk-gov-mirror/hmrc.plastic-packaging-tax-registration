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

package uk.gov.hmrc.plasticpackagingtaxregistration.repositories

import com.codahale.metrics.Timer
import com.kenshoo.play.metrics.Metrics
import javax.inject.Inject
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.collection.JSONCollection
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats.objectIdFormats
import uk.gov.hmrc.plasticpackagingtaxregistration.models.Registration

import scala.concurrent.{ExecutionContext, Future}

class RegistrationRepository @Inject() (mc: ReactiveMongoComponent, metrics: Metrics)(implicit ec: ExecutionContext)
    extends ReactiveRepository[Registration, BSONObjectID](collectionName = "registrations",
                                                           mongo = mc.mongoConnector.db,
                                                           domainFormat = Registration.format,
                                                           idFormat = objectIdFormats
    ) {

  override lazy val collection: JSONCollection =
    mongo().collection[JSONCollection](collectionName, failoverStrategy = RepositorySettings.failoverStrategy)

  override def indexes: Seq[Index] = Seq(Index(Seq("id" -> IndexType.Ascending), Some("idIdx"), unique = true))

  def findByRegistrationId(id: String): Future[Option[Registration]] = {
    val findStopwatch = newMongoDBTimer("mongo.registration.find").time()
    super.find("id" -> id).map(_.headOption).andThen {
      case _ => findStopwatch.stop()
    }
  }

  def create(registration: Registration): Future[Registration] =
    super.insert(registration).map(_ => registration)

  def update(registration: Registration): Future[Option[Registration]] = {
    val updateStopwatch = newMongoDBTimer("mongo.registration.update").time()
    super
      .findAndUpdate(Json.obj("id" -> registration.id),
                     Json.toJson(registration).as[JsObject],
                     fetchNewObject = true,
                     upsert = false
      )
      .map(_.value.map(_.as[Registration]))
      .andThen {
        case _ => updateStopwatch.stop()
      }
  }

  def delete(registration: Registration): Future[Unit] =
    super
      .remove("id" -> registration.id)
      .map(_ => Unit)

  private def newMongoDBTimer(name: String): Timer = metrics.defaultRegistry.timer(name)
}

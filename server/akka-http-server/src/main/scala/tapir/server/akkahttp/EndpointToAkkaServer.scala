package tapir.server.akkahttp

import akka.http.scaladsl.model.{HttpResponse, StatusCode => AkkaStatusCode, MediaType => _}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.RouteDirectives
import akka.http.scaladsl.server.util.{Tuple => AkkaTuple}
import tapir._
import tapir.model.StatusCodes
import tapir.server.ServerEndpoint
import tapir.typelevel.ParamsToTuple

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

class EndpointToAkkaServer(serverOptions: AkkaHttpServerOptions) {
  def toDirective[I, E, O, T](e: Endpoint[I, E, O, AkkaStream])(implicit paramsToTuple: ParamsToTuple.Aux[I, T]): Directive[T] = {
    implicit val tIsAkkaTuple: AkkaTuple[T] = AkkaTuple.yes
    toDirective1(e).flatMap { values =>
      tprovide(paramsToTuple.toTuple(values))
    }
  }

  def toRouteRecoverErrors[I, E, O](
      e: Endpoint[I, E, O, AkkaStream]
  )(logic: I => Future[O])(implicit eIsThrowable: E <:< Throwable, eClassTag: ClassTag[E]): Route = {
    def reifyFailedFuture(f: Future[O]): Future[Either[E, O]] = {
      import ExecutionContext.Implicits.global
      f.map(Right(_): Either[E, O]).recover {
        case e: Throwable if implicitly[ClassTag[E]].runtimeClass.isInstance(e) => Left(e.asInstanceOf[E]): Either[E, O]
      }
    }

    toRoute(e.serverLogic(logic.andThen(reifyFailedFuture)))
  }

  def toRoute[I, E, O](se: ServerEndpoint[I, E, O, AkkaStream, Future]): Route = {
    toDirective1(se.endpoint) { values =>
      onSuccess(se.logic(values)) {
        case Left(v)  => outputToRoute(StatusCodes.BadRequest, se.endpoint.errorOutput, v)
        case Right(v) => outputToRoute(StatusCodes.Ok, se.endpoint.output, v)
      }
    }
  }

  def toRoute(serverEndpoints: List[ServerEndpoint[_, _, _, AkkaStream, Future]]): Route = {
    serverEndpoints.map(se => toRoute(se)).foldLeft(RouteDirectives.reject: Route)(_ ~ _)
  }

  private def toDirective1[I, E, O](e: Endpoint[I, E, O, AkkaStream]): Directive1[I] = new EndpointToAkkaDirective(serverOptions)(e)

  private def outputToRoute[O](defaultStatusCode: AkkaStatusCode, output: EndpointOutput[O], v: O): Route = {
    val responseValues = OutputToAkkaResponse(output, v)

    val statusCode = responseValues.statusCode.map(c => c: AkkaStatusCode).getOrElse(defaultStatusCode)

    val completeRoute = responseValues.body match {
      case Some(entity) => complete(HttpResponse(entity = entity, status = statusCode))
      case None         => complete(HttpResponse(statusCode))
    }

    if (responseValues.headers.nonEmpty) {
      respondWithHeaders(responseValues.headers: _*)(completeRoute)
    } else {
      completeRoute
    }
  }
}

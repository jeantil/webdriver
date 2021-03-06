package com.typesafe.webdriver

import org.specs2.mutable.Specification
import akka.testkit._
import akka.actor.ActorRef
import java.io.File
import scala.concurrent.{Promise, Future}
import spray.json.{JsObject, JsNull, JsValue, JsArray}
import com.typesafe.webdriver.WebDriverCommands.{WebDriverSession, WebDriverError}

// Note that this test will only run on Unix style environments where the "rm" command is available.
class LocalBrowserSpec extends Specification {

  object TestWebDriverCommands extends WebDriverCommands {
    override def createSession(desiredCapabilities:JsObject=JsObject(), requiredCapabilities:JsObject=JsObject()): Future[Either[WebDriverError, WebDriverSession]] =
      Promise.successful(Right(WebDriverSession("123",JsObject()))).future

    override def destroySession(sessionId: String) {}

    override def executeJs(sessionId: String, script: String, args: JsArray): Future[Either[WebDriverError, JsValue]] =
      Future.successful(Right(JsNull))

    override def executeNativeJs(sessionId: String, script: String, args: JsArray): Future[Either[WebDriverError, JsValue]] = {
      throw new UnsupportedOperationException
    }

    override def screenshot(sessionId: String): Future[Either[WebDriverError, JsValue]] = {
      throw new UnsupportedOperationException
    }

    override def navigateTo(sessionId: String, url: String): Future[Either[WebDriverError, Unit]] =
      throw new UnsupportedOperationException
  }

  "The local browser" should {
    "be started when requested, remove a file and then shutdown" in new TestActorSystem {
      val f = File.createTempFile("LocalBrowserSpec", "")
      f.deleteOnExit()

      val localBrowser = TestFSMRef(new LocalBrowser(Session.props(TestWebDriverCommands), Some(Seq("rm", f.getCanonicalPath))))

      val probe = TestProbe()
      probe watch localBrowser

      localBrowser.stateName must_== LocalBrowser.Uninitialized

      localBrowser ! LocalBrowser.Startup

      localBrowser.stateName must_== LocalBrowser.Started

      localBrowser.stop()

      probe.expectTerminated(localBrowser)

      f.exists() must beFalse
    }

    "permit a session to be created" in new TestActorSystem {
      val localBrowser = TestFSMRef(new LocalBrowser(Session.props(TestWebDriverCommands), None))

      localBrowser ! LocalBrowser.Startup

      localBrowser ! LocalBrowser.CreateSession()
      expectMsgClass(classOf[ActorRef])
    }
  }

}

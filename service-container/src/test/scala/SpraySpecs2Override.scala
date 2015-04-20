/**
 * This code is here only because spray.io does not support specs2 version 3.x
 * so we need to override some logic in order for testing to compile and operate
 * correctly
 *
 * See the following link for more information:
 * https://groups.google.com/forum/#!topic/spray-user/2T6SBp4OJeI
 */
package spray.testkit

import org.specs2.execute.{ Failure, FailureException }
import org.specs2.specification.core.{Fragments, SpecificationStructure}
import org.specs2.specification.dsl.ActionDsl

trait Specs2Interface extends TestFrameworkInterface with SpecificationStructure with ActionDsl {

  def failTest(msg: String) = {
    val trace = new Exception().getStackTrace.toList
    val fixedTrace = trace.drop(trace.indexWhere(_.getClassName.startsWith("org.specs2")) - 1)
    throw new FailureException(Failure(msg, stackTrace = fixedTrace))
  }

  override def map(fs: ⇒ Fragments) = super.map(fs).append(step(cleanUp()))
}

trait Specs2RouteTest extends RouteTest with Specs2Interface


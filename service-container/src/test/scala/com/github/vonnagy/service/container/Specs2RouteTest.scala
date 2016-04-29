package com.github.vonnagy.service.container

import akka.http.scaladsl.testkit.{RouteTest, TestFrameworkInterface}
import org.specs2.execute.{Failure, FailureException}
import org.specs2.specification.AfterAll
import org.specs2.specification.core.{Fragments, SpecificationStructure}
import org.specs2.specification.dsl.ActionDsl

trait Specs2Interface extends TestFrameworkInterface with SpecificationStructure with ActionDsl with AfterAll {

  def afterAll(): Unit = {
    cleanUp()
  }

  def failTest(msg: String) = {
    val trace = new Exception().getStackTrace.toList
    val fixedTrace = trace.drop(trace.indexWhere(_.getClassName.startsWith("org.specs2")) - 1)
    throw new FailureException(Failure(msg, stackTrace = fixedTrace))
  }

  override def map(fs: ⇒ Fragments) = super.map(fs).append(step(cleanUp()))
}

trait Specs2RouteTest extends RouteTest with Specs2Interface


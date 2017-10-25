package com.nexthink.utils.parsing.combinator.completion
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods.{compact, render}
import org.junit.Assert._
import org.junit.Test

import scala.util.parsing.input.OffsetPosition

class CompletionTypesTest extends CompletionTypes {
  override type Elem = Char

  val setA = CompletionSet(
    CompletionTag("A", 10, "description").updateMeta("type" -> "a-type"),
    Set(Completion("a", 2, Some("meta1")), Completion("b", 1).updateMeta(("objects" -> Seq("devices")) ~ ("themes" -> Seq("some"))))
  )

  val setB = CompletionSet(CompletionTag("B", 5), Set(Completion("c", 4), Completion("d", 3)))
  val setC = CompletionSet("C", Completion("e", 10))
  val setAPrime = CompletionSet(
    CompletionTag("A", 10).updateMeta("style" -> "highlight"),
    Set(
      Completion("a", 4, Some("meta2")),
      Completion("b", 1).updateMeta(("objects" -> Seq("users", "packages")) ~ ("themes" -> Seq("other"))),
      Completion("aa")
    )
  )

  @Test
  def completionsTakeTopWorks(): Unit = {
    // Arrange
    val compl = Completions(Seq(setA, setB, setC))

    // Act
    val lettersInOrder = Seq("a", "b", "c", "d", "e")
    val letterSets     = for (i <- 1 until lettersInOrder.length) yield lettersInOrder.take(i)
    letterSets.foreach(set => assertEquals(set, compl.takeTop(set.length).completionStrings))
  }

  @Test
  def completionsSetsScoredWithMaxCompletionWorks(): Unit = {
    // Arrange
    val compl = Completions(Seq(setA, setB, setC))

    // Act
    assertEquals(Seq("e", "c", "d", "a", "b"), compl.setsScoredWithMaxCompletion().completionStrings)
  }

  @Test
  def completionsAtSamePositionAreMerged(): Unit = {
    // Act
    val merged = Completions(Seq(setA, setB)).updateMeta("context" -> Seq("contextA")) | Completions(Seq(setAPrime, setC))
      .updateMeta("context" -> Seq("contextB"))

    // Assert
    assertArrayEquals(
      merged.allSets.toArray[AnyRef],
      Seq(
        CompletionSet(
          CompletionTag("A", 10, "description").updateMeta(("type" -> "a-type") ~ ("style" -> "highlight")),
          Set(
            Completion("a", 4, Some("meta1, meta2")),
            Completion("b", 1).updateMeta(("objects" -> Seq("devices", "users", "packages")) ~ ("themes" -> Seq("some", "other"))),
            Completion("aa")
          )
        ),
        setB,
        setC
      ).toArray[AnyRef]
    )
    assertEquals(Some(compact(render("context" -> Seq("contextA", "contextB")))), merged.meta)
  }

  @Test
  def completionsAtMostAdvancedPositionArePicked(): Unit = {
    // Arrange
    val foobar           = "foobar"
    val initialPosition  = OffsetPosition(foobar, 0)
    val advancedPosition = OffsetPosition(foobar, 1)

    // Act
    assertEquals((Completions(initialPosition, setA) | Completions(advancedPosition, setB)).allSets.head, setB)
    assertEquals((Completions(advancedPosition, setA) | Completions(initialPosition, setB)).allSets.head, setA)
  }
}

/*
 * Copyright (C) 2023 The Dagger Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dagger.functional.kotlinsrc.variance

import com.google.common.truth.Truth.assertThat
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.inject.Inject
import javax.inject.Singleton
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

// This class tests some usages of the declaration-site variance defined in the declaration of the
// List class, i.e. List<out T>. In these tests, I'm only using @JvmWildcard and
// @JvmSuppressWildcards when it's required for correctness, which depends on:
//
//   1) Where the type is used
//   2) If the type argument is an open or final class
//
// This isn't an exhaustive list, but covers some of the common cases and we can add more as we find
// them.
@RunWith(JUnit4::class)
class DeclarationVarianceTest {

  @Singleton
  @Component(modules = [BarModule::class])
  interface BarComponent {
    fun listBar(): List<Bar>
    fun listOutBar(): List<@JvmWildcard Bar>
    fun barUsage(): BarUsage
  }

  @Module
  object BarModule {
    @Singleton
    @Provides
    fun provideListBar(): List<Bar> = listOf(Bar("provideListBar"))

    @Singleton
    @Provides
    fun provideListOutBar(): List<@JvmWildcard Bar> = listOf(Bar("provideListOutBar"))
  }

  @Suppress("BadInject") // We're using constructor and members injection on purpose for this test.
  class BarUsage @Inject constructor(
    val listBar1: List<Bar>,
    val listOutBar1: List<@JvmWildcard Bar>,
  ) {
    @Inject lateinit var listBar2: List<Bar>
    @Inject lateinit var listOutBar2: List<@JvmWildcard Bar>
    lateinit var listBar3: List<Bar>
    lateinit var listOutBar3: List<Bar>

    @Inject
    fun injectMethod(
      listBar3: List<Bar>,
      listOutBar3: List<@JvmWildcard Bar>,
    ) {
      this.listBar3 = listBar3
      this.listOutBar3 = listOutBar3
    }
  }

  class Bar(val providesMethod: String) {
    override fun toString(): String = providesMethod
  }

  @Test
  fun testFinalClassBarUsedAsTypeArgument() {
    val component = DaggerDeclarationVarianceTest_BarComponent.create()
    val listBar = component.listBar()
    val listOutBar = component.listOutBar()
    val barUsage = component.barUsage()

    assertThat(listBar).isNotNull()
    assertThat(listOutBar).isNotNull()
    assertThat(listBar).isNotEqualTo(listOutBar)

    assertThat(barUsage.listBar1).isEqualTo(listBar)
    assertThat(barUsage.listBar2).isEqualTo(listBar)
    assertThat(barUsage.listBar3).isEqualTo(listBar)

    assertThat(barUsage.listOutBar1).isEqualTo(listOutBar)
    assertThat(barUsage.listOutBar2).isEqualTo(listOutBar)
    assertThat(barUsage.listOutBar3).isEqualTo(listOutBar)
  }

  @Singleton
  @Component(modules = [FooModule::class])
  interface FooComponent {
    fun listFoo(): List<Foo>
    fun listOutFoo(): List<@JvmWildcard Foo>
    fun fooUsage(): FooUsage
  }

  @Module
  object FooModule {
    @Singleton
    @Provides
    fun provideListFoo(): List<Foo> = listOf(Foo("provideListFoo"))

    @Singleton
    @Provides
    fun provideListOutFoo(): List<@JvmWildcard Foo> = listOf(Foo("provideListOutFoo"))
  }

  @Suppress("BadInject") // We're using constructor and members injection on purpose for this test.
  class FooUsage @Inject constructor(
    val listFoo1: List<@JvmSuppressWildcards Foo>,
    val listOutFoo1: List<Foo>,
  ) {
    @Inject lateinit var listFoo2: List<@JvmSuppressWildcards Foo>
    @Inject lateinit var listOutFoo2: List<@JvmWildcard Foo>
    lateinit var listFoo3: List<Foo>
    lateinit var listOutFoo3: List<Foo>

    @Inject
    fun injectMethod(
      listFoo3: List<@JvmSuppressWildcards Foo>,
      listOutFoo3: List<Foo>,
    ) {
      this.listFoo3 = listFoo3
      this.listOutFoo3 = listOutFoo3
    }
  }

  open class Foo(val providesMethod: String) {
    override fun toString(): String = providesMethod
  }

  @Test
  fun testOpenClassFooUsedAsTypeArgument() {
    val component = DaggerDeclarationVarianceTest_FooComponent.create()
    val listFoo = component.listFoo()
    val listOutFoo = component.listOutFoo()
    val fooUsage = component.fooUsage()

    assertThat(listFoo).isNotNull()
    assertThat(listOutFoo).isNotNull()
    assertThat(listFoo).isNotEqualTo(listOutFoo)

    assertThat(fooUsage.listFoo1).isEqualTo(listFoo)
    assertThat(fooUsage.listFoo2).isEqualTo(listFoo)
    assertThat(fooUsage.listFoo3).isEqualTo(listFoo)

    assertThat(fooUsage.listOutFoo1).isEqualTo(listOutFoo)
    assertThat(fooUsage.listOutFoo2).isEqualTo(listOutFoo)
    assertThat(fooUsage.listOutFoo3).isEqualTo(listOutFoo)
  }
}

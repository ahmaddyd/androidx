/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.room.compiler.processing

import com.squareup.kotlinpoet.OriginatingElementsHolder
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.javapoet.KClassName

internal val KOTLIN_NONE_TYPE_NAME: KClassName =
    KClassName("androidx.room.compiler.processing.error", "NotAType")

/**
 * Adds the given element as an originating element for compilation.
 * see [OriginatingElementsHolder.Builder.addOriginatingElement].
 */
fun <T : OriginatingElementsHolder.Builder<T>> T.addOriginatingElement(
    element: XElement
): T {
    element.originatingElementForPoet()?.let(this::addOriginatingElement)
    return this
}

internal fun TypeName.rawTypeName(): TypeName {
    return if (this is ParameterizedTypeName) {
        this.rawType
    } else {
        this
    }
}
/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.room.compiler.codegen.kotlin

import androidx.room.compiler.codegen.KTypeSpecBuilder
import androidx.room.compiler.codegen.VisibilityModifier
import androidx.room.compiler.codegen.XAnnotationSpec
import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.XTypeSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.javapoet.KTypeSpec

internal class KotlinTypeSpec(
    override val className: XClassName,
    internal val actual: KTypeSpec
) : KotlinLang(), XTypeSpec {

    internal class Builder(
        private val className: XClassName,
        internal val actual: KTypeSpecBuilder
    ) : KotlinLang(), XTypeSpec.Builder {
        override fun superclass(typeName: XTypeName) = apply {
            actual.superclass(typeName.kotlin)
        }

        override fun addAnnotation(annotation: XAnnotationSpec) {
            check(annotation is KotlinAnnotationSpec)
            actual.addAnnotation(annotation.actual)
        }

        override fun addProperty(
            typeName: XTypeName,
            name: String,
            visibility: VisibilityModifier,
            isMutable: Boolean,
            initExpr: XCodeBlock?,
            annotations: List<XAnnotationSpec>
        ) = apply {
            actual.addProperty(
                PropertySpec.builder(name, typeName.kotlin).apply {
                    mutable(isMutable)
                    addModifiers(visibility.toKotlinVisibilityModifier())
                    initExpr?.let {
                        require(it is KotlinCodeBlock)
                        initializer(it.actual)
                    }
                    // TODO(b/247247439): Add other annotations
                }.build()
            )
        }

        override fun addFunction(functionSpec: XFunSpec) = apply {
            require(functionSpec is KotlinFunSpec)
            actual.addFunction(functionSpec.actual)
        }

        override fun build(): XTypeSpec {
            return KotlinTypeSpec(className, actual.build())
        }
    }
}
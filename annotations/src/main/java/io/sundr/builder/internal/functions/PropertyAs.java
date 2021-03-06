/*
 * Copyright 2015 The original authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.sundr.builder.internal.functions;

import io.sundr.Function;
import io.sundr.builder.internal.BuilderContext;
import io.sundr.builder.internal.BuilderContextManager;
import io.sundr.codegen.model.JavaClazz;
import io.sundr.codegen.model.JavaClazzBuilder;
import io.sundr.codegen.model.JavaMethod;
import io.sundr.codegen.model.JavaMethodBuilder;
import io.sundr.codegen.model.JavaProperty;
import io.sundr.codegen.model.JavaPropertyBuilder;
import io.sundr.codegen.model.JavaType;
import io.sundr.codegen.model.JavaTypeBuilder;

import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static io.sundr.builder.Constants.BODY;
import static io.sundr.builder.Constants.MEMBER_OF;
import static io.sundr.builder.Constants.N;
import static io.sundr.codegen.utils.StringUtils.captializeFirst;

public final class PropertyAs {

    public static final Function<JavaProperty, JavaClazz> CLASS = new Function<JavaProperty, JavaClazz>() {
        @Override
        public JavaClazz apply(JavaProperty item) {
            BuilderContext context = BuilderContextManager.getContext();
            Elements elements = context.getElements();
            JavaType type = TypeAs.UNWRAP_COLLECTION_OF.apply(item.getType());
            TypeElement typeElement = elements.getTypeElement(type.getFullyQualifiedName());
            return BuilderContextManager.getContext().getToClazz().apply(typeElement);
        }
    };

    public static final Function<JavaProperty, JavaClazz> NESTED_CLASS = new Function<JavaProperty, JavaClazz>() {
        @Override
        public JavaClazz apply(JavaProperty item) {
            JavaType baseType = TypeAs.UNWRAP_COLLECTION_OF.apply(item.getType());
            JavaType builderType = TypeAs.SHALLOW_BUILDER.apply(baseType);

            JavaType nestedType = NESTED_TYPE.apply(item);
            JavaType nestedUnwrapped = new JavaTypeBuilder(nestedType).withGenericTypes(new JavaType[0]).build();

            Set<JavaMethod> nestedMethods = new HashSet<JavaMethod>();
            nestedMethods.add(ToMethod.AND.apply(item));
            nestedMethods.add(ToMethod.END.apply(item));

            Set<JavaProperty> properties = new HashSet<JavaProperty>();
            Set<JavaMethod> constructors = new HashSet<JavaMethod>();
            
            JavaType memberOf = (JavaType) item.getAttributes().get(MEMBER_OF);
            properties.add(new JavaPropertyBuilder()
                    .withName("builder")
                    .withType(builderType).build());

            constructors.add(new JavaMethodBuilder()
                    .withName("")
                    .withReturnType(nestedUnwrapped)
                    .addNewArgument()
                        .withName("item")
                        .withType(baseType)
                    .endArgument()
                    .addToAttributes(BODY, "this.builder = new " + builderType.getSimpleName() + "(this, item);")
                    .build());

            constructors.add(new JavaMethodBuilder()
                    .withName("")
                    .withReturnType(nestedUnwrapped)
                    .addToAttributes(BODY, "this.builder = new " + builderType.getSimpleName() + "(this);")
                    .build());

            return new JavaClazzBuilder()
                    .withType(nestedType)
                    .withFields(properties)
                    .withMethods(nestedMethods)
                    .withConstructors(constructors)
                    .addToAttributes(MEMBER_OF, memberOf)
                    .build();
        }
    };


    public static final Function<JavaProperty, JavaType> NESTED_TYPE = new Function<JavaProperty, JavaType>() {
        @Override
        public JavaType apply(JavaProperty item) {
            JavaType nested = SHALLOW_NESTED_TYPE.apply(item);
            //Not a typical fluent
            JavaType fluent = TypeAs.UNWRAP_COLLECTION_OF.apply(item.getType());
            JavaType superClassFluent = new JavaTypeBuilder(fluent)
                    .withClassName(TypeAs.UNWRAP_COLLECTION_OF.apply(item.getType()) + "Fluent")
                    .withGenericTypes(new JavaType[]{nested})
                    .build();

            return new JavaTypeBuilder(nested)
                    .withGenericTypes(new JavaType[]{N})
                    .withSuperClass(superClassFluent)
                    .withInterfaces(new HashSet(Arrays.asList(BuilderContextManager.getContext().getNestedInterface().getType())))
                    .build();
        }

    };

    public static final Function<JavaProperty, JavaType> SHALLOW_NESTED_TYPE = new Function<JavaProperty, JavaType>() {
        public JavaType apply(JavaProperty property) {
            return new JavaTypeBuilder()
                    .withClassName(captializeFirst(property.getName() + "Nested"))
                    .withGenericTypes(new JavaType[]{N})
                    .build();
        }
    };

}

package com.luufery.bytebuddy.api.plugin.point;


import com.luufery.bytebuddy.api.advice.InterceptorPoint;
import com.luufery.bytebuddy.api.advice.RaspAdvice;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class PluginInterceptorPoint implements InterceptorPoint {


    public String getTargetClass() {
        //TODO 这里还需要这个接口吗
        return targetClass.toString();
    }

    private final ElementMatcher.Junction<TypeDescription> targetClass;

    private final List<RaspTransformationPoint<? extends RaspAdvice>> transformationPoint;

    public static PluginInterceptorPoint createDefault() {
        return new PluginInterceptorPoint(ElementMatchers.is(Object.class), Collections.emptyList());
    }


    public static Builder intercept(final ElementMatcher.Junction<TypeDescription> classNameOfTarget) {
        return new Builder(classNameOfTarget);
    }

    public static final class Builder {

        private final List<RaspTransformationPoint<? extends RaspAdvice>> transformationPoints = new ArrayList<>();

        private final ElementMatcher.Junction<TypeDescription> targetClass;

        private Builder(final ElementMatcher.Junction<TypeDescription> classNameOfTarget) {
            this.targetClass = classNameOfTarget;
        }


        public TransformationPointBuilder on(final ElementMatcher<? super MethodDescription> matcher) {
            return new TransformationPointBuilder(this, matcher);
        }

        public PluginInterceptorPoint install() {
            return new PluginInterceptorPoint(targetClass, transformationPoints);
        }

        public static final class TransformationPointBuilder {

            private final Builder builder;

            private final ElementMatcher<? super MethodDescription> matcher;

            private Class<? extends RaspAdvice> classOfAdvice;

            private TransformationPointBuilder(final Builder builder, final ElementMatcher<? super MethodDescription> matcher) {
                this.builder = builder;
                this.matcher = matcher;
            }

            public TransformationPointBuilder implement(final Class<? extends RaspAdvice> classOfAdvice) {
                this.classOfAdvice = classOfAdvice;
                return this;
            }

            public Builder build() {
                builder.transformationPoints.add(new RaspTransformationPoint<>(matcher, classOfAdvice));
                return builder;
            }
        }
    }
}


package com.luufery.bytebuddy.api.plugin.point;

import com.luufery.bytebuddy.api.advice.RaspAdvice;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;

@Getter
@RequiredArgsConstructor
public class RaspTransformationPoint<T> {

    private final ElementMatcher<? super MethodDescription> matcher;

    private final Class<? extends RaspAdvice> classOfAdvice;
}

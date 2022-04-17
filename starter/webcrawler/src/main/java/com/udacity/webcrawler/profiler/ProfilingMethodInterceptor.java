package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Objects;

class ProfilingMethodInterceptor implements InvocationHandler {
    private Clock clock;
    private ProfilingState profilingState;
    private ZonedDateTime startTime;
    private Object delegate;
    Object targetObject;

    public ProfilingMethodInterceptor(Object delegate, Clock clock, ProfilingState profilingState) {
        this.delegate = Objects.requireNonNull(delegate);
        this.clock = Objects.requireNonNull(clock);
        this.profilingState = Objects.requireNonNull(profilingState);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {


            if (method.isAnnotationPresent(Profiled.class)) {
                Instant theStartTime = clock.instant();
                try
                {
                    method.invoke(delegate.getClass(), args);
                }catch (Throwable exception){throw exception.getCause();}
                finally {
                    if(method.isAnnotationPresent(Profiled.class))
                    {
                        Duration duration = Duration.between(theStartTime, clock.instant());
                        profilingState.record(ProfilingMethodInterceptor.class, method, duration);
                    }
                }
            }
            return method.invoke(delegate.getClass(),args);

        }

}

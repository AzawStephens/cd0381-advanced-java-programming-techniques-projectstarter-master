package com.udacity.webcrawler.profiler;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.*;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Objects;

import static java.lang.reflect.Proxy.newProxyInstance;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Concrete implementation of the {@link Profiler}.
 */
final class ProfilerImpl implements Profiler {

  private final Clock clock;
  private final ProfilingState state = new ProfilingState();
  private final ZonedDateTime startTime;

  @Inject
  ProfilerImpl(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
    this.startTime = ZonedDateTime.now(clock);
  }

  @Override
  public <T> T wrap(Class<T> klass, T delegate) {
    Objects.requireNonNull(klass);

    // TODO: Use a dynamic proxy (java.lang.reflect.Proxy) to "wrap" the delegate in a
    //       ProfilingMethodInterceptor and return a dynamic proxy from this method.
    //       See https://docs.oracle.com/javase/10/docs/api/java/lang/reflect/Proxy.html.

    Method[] theMethods = klass.getMethods();
    for (Method method: theMethods)
    {
      if(method.isAnnotationPresent(Profiled.class))
      {

        Object proxy = newProxyInstance(
                klass.getClassLoader(),
                klass.getInterfaces(),
                new ProfilingMethodInterceptor(delegate,clock, state, startTime));

            return (T)proxy;
      }
    }


    return delegate;
  }

  @Override
  public void writeData(Path path) {
    // TODO: Write the ProfilingState data to the given file path. If a file already exists at that
    //       path, the new data should be appended to the existing file.
  }

  @Override
  public void writeData(Writer writer) throws IOException {
    writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
    writer.write(System.lineSeparator());
    state.write(writer);
    writer.write(System.lineSeparator());
  }

}
 class ProfilingMethodInterceptor implements InvocationHandler
{
  private Clock clock;
  private ProfilingState profilingState;
  private ZonedDateTime startTime;
  private Object delegate;
  Object targetObject;
  public ProfilingMethodInterceptor(Object delegate, Clock clock, ProfilingState profilingState, ZonedDateTime startTime )
  {
    this.delegate = delegate;
    this.clock = clock;
    this.profilingState = profilingState;
    this.startTime = startTime;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

    try
    {

        Duration duration = Duration.between(startTime,clock.instant());
        profilingState.record(ProfilingMethodInterceptor.class,method,duration);
        return method.invoke(method.getClass(),args);
    }catch(InvocationTargetException e)
    {
      Duration duration = Duration.between(startTime,clock.instant());
      profilingState.record(ProfilingMethodInterceptor.class,method,duration);
      throw e.getTargetException();}
    catch(UndeclaredThrowableException e){return e.getUndeclaredThrowable();}
      //this maybe wrong
  }
}


package com.butent.bee.server.rest;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("rest")
public class RestApplication extends Application {

  @Override
  public Set<Class<?>> getClasses() {
    Set<Class<?>> classes = new HashSet<>();

    classes.add(AuthenticationFilter.class);
    classes.add(Worker.class);
    classes.add(CompaniesWorker.class);
    classes.add(CompanyPersonsWorker.class);
    classes.add(TasksWorker.class);
    classes.add(PayrollWorker.class);

    return classes;
  }
}
